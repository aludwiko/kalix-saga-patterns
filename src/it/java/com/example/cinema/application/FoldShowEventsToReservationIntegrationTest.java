package com.example.cinema.application;

import com.example.Main;
import com.example.cinema.domain.Reservation;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.cinema.application.TestUtils.randomId;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
class FoldShowEventsToReservationIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;
  @Autowired
  private ShowCalls showCalls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldUpdateShowByReservationEntry() {
    //given
    var showId = randomId();
    var reservationId1 = randomId();
    var reservationId2 = randomId();
    var walletId = randomId();
    showCalls.createShow(showId, "title");

    //when
    showCalls.reserveSeat(showId, walletId, reservationId1, 3);
    showCalls.reserveSeat(showId, walletId, reservationId2, 4);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        Reservation result = getReservation(reservationId1).getBody();
        assertThat(result).isEqualTo(new Reservation(reservationId1, showId));

        Reservation result2 = getReservation(reservationId2).getBody();
        assertThat(result2).isEqualTo(new Reservation(reservationId2, showId));
      });

    //when
    showCalls.cancelSeatReservation(showId, reservationId2);

    //then
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        Reservation result = getReservation(reservationId1).getBody();
        assertThat(result).isEqualTo(new Reservation(reservationId1, showId));

        HttpStatusCode statusCode = showCalls.getShowByReservation(reservationId2).getStatusCode();
        assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND);
      });
  }

  private ResponseEntity<Reservation> getReservation(String reservationId) {
    return webClient.get().uri("/reservation/" + reservationId)
      .retrieve()
      .toEntity(Reservation.class)
      .onErrorResume(WebClientResponseException.class, error -> {
        if (error.getStatusCode().is4xxClientError()) {
          return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else {
          return Mono.error(error);
        }
      })
      .block();
  }
}