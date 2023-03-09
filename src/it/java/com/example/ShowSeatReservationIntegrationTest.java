package com.example;

import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.ShowCommand;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class ShowSeatReservationIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  private final Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldCompleteSeatReservation() {
    //given
    var walletId = randomId();
    var showId = "show1";
    var reservationId = randomId();
    var seatNumber = 10;

    createWallet(walletId, 200);
    createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          ResponseEntity<SeatStatus> seatStatus = getSeatStatus(showId, seatNumber);
          assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.PAID);
        });
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() {
    //given
    var walletId = randomId();
    var showId = "show1";
    var reservationId = randomId();
    var seatNumber = 11;

    createWallet(walletId, 1);
    createShow(showId, "pulp fiction");

    //when
    ResponseEntity<Void> reservationResponse = reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.getStatusCode()).isEqualTo(OK);

    //then
    await()
        .atMost(10, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          ResponseEntity<SeatStatus> seatStatus = getSeatStatus(showId, seatNumber);
          assertThat(seatStatus.getBody()).isEqualTo(SeatStatus.AVAILABLE);
        });
  }

  private String randomId() {
    return UUID.randomUUID().toString().substring(0, 7);
  }

  private ResponseEntity<Void> reserveSeat(String showId, String walletId, String reservationId, int seatNumber) {
    return webClient.patch().uri("/cinema-show/" + showId + "/reserve")
        .bodyValue(new ShowCommand.ReserveSeat(walletId, reservationId, seatNumber))
        .retrieve()
        .toBodilessEntity()
        .block(timeout);
  }

  private ResponseEntity<SeatStatus> getSeatStatus(String showId, int seatNumber) {
    return webClient.get().uri("/cinema-show/" + showId + "/seat-status/" + seatNumber)
        .retrieve()
        .toEntity(SeatStatus.class)
        .block(timeout);
  }

  private void createWallet(String walletId, int amount) {
    ResponseEntity<Void> response = webClient.post().uri("/wallet/" + walletId + "/create/" + amount)
        .retrieve()
        .toBodilessEntity()
        .block(timeout);

    assertThat(response.getStatusCode()).isEqualTo(OK);
  }

  private void createShow(String showId, String title) {
    int maxSeats = 100;

    ResponseEntity<Void> getOrCreate = webClient.get().uri("/cinema-show/" + showId)
        .retrieve()
        .toBodilessEntity()
        .onErrorResume(WebClientResponseException.class, error -> {
          if (error.getStatusCode().is4xxClientError()) {
            return webClient.post().uri("/cinema-show/" + showId)
                .bodyValue(new ShowCommand.CreateShow(title, maxSeats))
                .retrieve()
                .toBodilessEntity();
          } else {
            return Mono.error(error);
          }
        })
        .block(timeout);

    assertThat(getOrCreate.getStatusCode()).isEqualTo(OK);
  }
}