package com.example.cinema.application;

import com.example.Main;
import com.example.cinema.application.SeatReservationWorkflow.ReserveSeat;
import com.example.cinema.domain.SeatReservationStatus;
import com.example.cinema.domain.SeatStatus;
import com.example.wallet.application.WalletResponse;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.cinema.application.TestUtils.randomId;
import static com.example.cinema.domain.SeatReservationStatus.SEAT_RESERVATION_REFUNDED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
@ActiveProfiles("orchestration")
class SeatReservationWorkflowTest {

  @Autowired
  private WebClient webClient;
  @Autowired
  private Calls calls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldCompleteSeatReservation() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 10;

    calls.createWallet(walletId, 200);
    calls.createShow(showId, "pulp fiction");

    ReserveSeat reserveSeat = new ReserveSeat(showId, seatNumber, new BigDecimal(100), walletId);

    //when
    ResponseEntity<Void> reservationResponse = reserveSeat(reservationId, reserveSeat);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        SeatReservationStatus status = getReservationStatus(reservationId);
        assertThat(status).isEqualTo(SeatReservationStatus.COMPLETED);

        WalletResponse walletResponse = calls.getWallet(walletId);
        assertThat(walletResponse.balance()).isEqualTo(new BigDecimal(200 - 100));

        SeatStatus seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(SeatStatus.PAID);
      });
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 10;

    calls.createWallet(walletId, 50);
    calls.createShow(showId, "pulp fiction");

    ReserveSeat reserveSeat = new ReserveSeat(showId, seatNumber, new BigDecimal(100), walletId);

    //when
    ResponseEntity<Void> reservationResponse = reserveSeat(reservationId, reserveSeat);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        SeatReservationStatus status = getReservationStatus(reservationId);
        assertThat(status).isEqualTo(SeatReservationStatus.SEAT_RESERVATION_FAILED);

        WalletResponse walletResponse = calls.getWallet(walletId);
        assertThat(walletResponse.balance()).isEqualTo(new BigDecimal(50));

        SeatStatus seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  @Test
  public void shouldCancelReservationInCaseOfWalletTimeoutAndRefundMoney() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = "42";
    var seatNumber = 10;

    calls.createWallet(walletId, 200);
    calls.createShow(showId, "pulp fiction");

    ReserveSeat reserveSeat = new ReserveSeat(showId, seatNumber, new BigDecimal(100), walletId);

    //when
    ResponseEntity<Void> reservationResponse = reserveSeat(reservationId, reserveSeat);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //simulating charging after timeout
    calls.chargeWallet(walletId, new ChargeWallet(new BigDecimal(100), reservationId, randomId()));

    //then
    await()
      .atMost(30, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        SeatReservationStatus status = getReservationStatus(reservationId);
        assertThat(status).isEqualTo(SEAT_RESERVATION_REFUNDED);

        WalletResponse walletResponse = calls.getWallet(walletId);
        assertThat(walletResponse.balance()).isEqualTo(new BigDecimal(200));

        SeatStatus seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  private ResponseEntity<Void> reserveSeat(String reservationId, ReserveSeat reserveSeat) {
    return webClient.post().uri("/seat-reservation/" + reservationId)
      .bodyValue(reserveSeat)
      .retrieve()
      .toBodilessEntity()
      .block(timeout);
  }

  private SeatReservationStatus getReservationStatus(String reservationId) {
    return webClient.get().uri("/seat-reservation/" + reservationId)
      .retrieve()
      .bodyToMono(SeatReservationStatus.class)
      .block(timeout);
  }

}