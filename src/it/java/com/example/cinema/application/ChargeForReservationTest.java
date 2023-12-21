package com.example.cinema.application;

import com.example.Main;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.wallet.application.WalletResponse;
import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.EventingTestKit.IncomingMessages;
import kalix.javasdk.testkit.EventingTestKit.Message;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DirtiesContext
@SpringBootTest(classes = Main.class)
@ActiveProfiles("choreography")
@Import(TestKitConfig.class)
public class ChargeForReservationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private KalixTestKit kalixTestKit;
  @Autowired
  private Calls calls;

  @Test
  public void shouldChargeWalletWithDeduplication() {
    //given
    IncomingMessages events = kalixTestKit.getEventSourcedEntityIncomingMessages("cinema-show");
    String walletId = "w1";
    SeatReserved seatReserved1 = new SeatReserved("s1", walletId, "r1", 1, new BigDecimal(100));
    SeatReserved seatReserved2 = new SeatReserved("s1", walletId, "r2", 1, new BigDecimal(100));
    String subject = "s1";

    calls.createWallet(walletId, 500);

    Metadata defaultMetadata = kalixTestKit.getMessageBuilder().of(seatReserved1, subject).getMetadata();
    Message<SeatReserved> seatReservedMessage1 = kalixTestKit.getMessageBuilder().of(seatReserved1, defaultMetadata.add("ce-sequence", "1"));
    Message<SeatReserved> seatReservedMessage2 = kalixTestKit.getMessageBuilder().of(seatReserved2, defaultMetadata.add("ce-sequence", "2"));

    //when
    events.publish(seatReservedMessage1);
    events.publish(seatReservedMessage1);
    events.publish(seatReservedMessage2);

    //then
    await()
        .atMost(ofSeconds(10))
        .ignoreExceptions()
        .untilAsserted(() -> {
          WalletResponse wallet = calls.getWallet(walletId);
          assertThat(wallet.balance()).isEqualTo(new BigDecimal(300));
        });
  }
}

@Configuration
class TestKitConfig {

  @Bean
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT.withAclEnabled()
        .withEventSourcedEntityIncomingMessages("cinema-show");
  }
}