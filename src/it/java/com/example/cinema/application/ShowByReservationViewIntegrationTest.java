package com.example.cinema.application;

import com.example.Main;
import com.example.cinema.domain.ShowByReservation;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.cinema.application.TestUtils.randomId;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
class ShowByReservationViewIntegrationTest extends KalixIntegrationTestKitSupport {

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
    ShowByReservation expected = new ShowByReservation(showId, Set.of(reservationId1, reservationId2));
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        ShowByReservation result = showCalls.getShowByReservation(reservationId1).getBody();
        assertThat(result).isEqualTo(expected);

        ShowByReservation result2 = showCalls.getShowByReservation(reservationId2).getBody();
        assertThat(result2).isEqualTo(expected);
      });

    //when
    showCalls.cancelSeatReservation(showId, reservationId2);

    //then
    ShowByReservation expectedAfterCancel = new ShowByReservation(showId, Set.of(reservationId1));
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        ShowByReservation result = showCalls.getShowByReservation(reservationId1).getBody();
        assertThat(result).isEqualTo(expectedAfterCancel);

        HttpStatusCode statusCode = showCalls.getShowByReservation(reservationId2).getStatusCode();
        assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND);
      });
  }

}