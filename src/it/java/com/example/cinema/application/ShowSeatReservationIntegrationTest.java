package com.example.cinema.application;

import com.example.Main;
import com.example.cinema.domain.SeatStatus;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

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
  private ShowCalls showCalls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldCompleteSeatReservation() throws InterruptedException {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 10;

    createWallet(walletId, 200);
    createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = showCalls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        ResponseEntity<SeatStatus> seatStatus = showCalls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.PAID);
      });
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() throws InterruptedException {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 11;

    createWallet(walletId, 1);
    createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = showCalls.reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        ResponseEntity<SeatStatus> seatStatus = showCalls.getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  private void createWallet(String walletId, int amount) {
    ResponseEntity<Void> response = webClient.post().uri("/wallet/" + walletId + "/create/" + amount)
      .retrieve()
      .toBodilessEntity()
      .block(timeout);

    assertThat(response.getStatusCode()).isEqualTo(OK);
  }

  private void createShow(String showId, String title) {
    assertThat(showCalls.createShow(showId, title).getStatusCode()).isEqualTo(OK);
  }
}