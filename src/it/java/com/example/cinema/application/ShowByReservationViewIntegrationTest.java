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
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.cinema.application.TestUtils.randomId;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
@ActiveProfiles("choreography")
class ShowByReservationViewIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private Calls calls;

  private Duration timeout = Duration.ofSeconds(10);

  @Test
  public void shouldUpdateShowByReservationEntry() {
    //given
    var showId = randomId();
    var reservationId1 = randomId();
    var reservationId2 = randomId();
    var walletId = randomId();
    calls.createShow(showId, "title");
    calls.createWallet(walletId, 500);

    //when
    calls.reserveSeat(showId, walletId, reservationId1, 3);
    calls.reserveSeat(showId, walletId, reservationId2, 4);

    //then
    ShowByReservation expected = new ShowByReservation(showId, Set.of(reservationId1, reservationId2));
    await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        ShowByReservation result = calls.getShowByReservation(reservationId1).getBody();
        assertThat(result).isEqualTo(expected);

        ShowByReservation result2 = calls.getShowByReservation(reservationId2).getBody();
        assertThat(result2).isEqualTo(expected);
      });
  }

}