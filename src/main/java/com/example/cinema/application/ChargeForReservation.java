package com.example.cinema.application;

import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ChargeForReservation extends Action {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;
  
  public ChargeForReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<String> charge(SeatReserved seatReserved) {
    logger.info("charging for reservation, triggered by " + seatReserved);
    String expenseId = seatReserved.reservationId();

    String sequenceNum = contextForComponents().metadata().get("ce-sequence").orElseThrow();
    String walletId = seatReserved.walletId();
    String commandId = UUID.nameUUIDFromBytes(sequenceNum.getBytes(UTF_8)).toString();
    var chargeWallet = new ChargeWallet(seatReserved.price(), expenseId, commandId);

    var attempts = 3;
    var retryDelay = Duration.ofSeconds(1);
    ActorSystem actorSystem = actionContext().materializer().system();

    return effects().asyncReply(
      Patterns.retry(() -> chargeWallet(walletId, chargeWallet),
          attempts,
          retryDelay,
          actorSystem)
        .exceptionallyComposeAsync(throwable ->
          registerFailure(throwable, walletId, chargeWallet)
        )
    );
  }

  private CompletionStage<String> chargeWallet(String walletId, ChargeWallet chargeWallet) {
    return componentClient.forEventSourcedEntity(walletId)
      .call(WalletEntity::charge)
      .params(chargeWallet)
      .execute()
      .thenApply(response -> "done");
  }


  private CompletionStage<String> registerFailure(Throwable throwable, String walletId, ChargeWallet chargeWallet) {
    var msg = getMessage(throwable);

    return componentClient.forEventSourcedEntity(walletId)
      .call(WalletFailureEntity::registerChargeError)
      .params(chargeWallet, msg)
      .execute();
  }

  private String getMessage(Throwable throwable) {
    if (throwable.getCause() != null &&
      throwable.getCause() instanceof WebClientResponseException.BadRequest badRequest) {
      return badRequest.getStatusCode() + "-" + badRequest.getResponseBodyAsString();
    } else {
      return throwable.getMessage();
    }
  }
}
