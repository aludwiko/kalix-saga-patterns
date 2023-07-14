package com.example.cinema.application;

import com.example.Main;
import com.example.cinema.domain.SeatStatus;
import com.example.wallet.application.WalletResponse;
import com.example.wallet.domain.WalletCommand;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.cinema.application.TestUtils.randomId;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


@DirtiesContext
@SpringBootTest(classes = Main.class)
public class ShowSeatReservationIntegrationTest extends KalixIntegrationTestKitSupport {

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

    //when
    ResponseEntity<Void> reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        ResponseEntity<SeatStatus> seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.PAID);

        WalletResponse wallet = calls.getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(100));
      });
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 11;

    calls.createWallet(walletId, 1);
    calls.createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        ResponseEntity<SeatStatus> seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  @Test
  public void shouldConfirmCancelledReservationAndRefund() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = "42";
    var seatNumber = 11;

    calls.createWallet(walletId, 300);
    calls.createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        ResponseEntity<SeatStatus> seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.AVAILABLE);
      });

    //simulating that the wallet was actually charged
    calls.chargeWallet(walletId, new WalletCommand.ChargeWallet(new BigDecimal(100), reservationId, randomId()));

    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        WalletResponse wallet = calls.getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(300));
      });
  }

  @Test
  public void shouldAllowToCancelAlreadyCancelledReservation() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = "42";
    var seatNumber = 11;

    calls.createWallet(walletId, 300);
    calls.createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = calls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        ResponseEntity<SeatStatus> seatStatus = calls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.AVAILABLE);
      });

    //simulating that the wallet charging was rejected for this reservation
    calls.chargeWallet(walletId, new WalletCommand.ChargeWallet(new BigDecimal(400), reservationId, randomId()));

    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        WalletResponse wallet = calls.getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(300));
      });
  }
}