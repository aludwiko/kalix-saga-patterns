package com.example.cinema.application;

import com.example.cinema.domain.Reservation;
import com.example.cinema.domain.ShowEvent.CancelledReservationConfirmed;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletCommand;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class RefundForReservation extends Action {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public RefundForReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Response> refund(CancelledReservationConfirmed cancelledReservationConfirmed) {
    logger.info("refunding for reservation, triggered by " + cancelledReservationConfirmed);

    String sequenceNum = contextForComponents().metadata().get("ce-sequence").orElseThrow();
    String commandId = UUID.nameUUIDFromBytes(sequenceNum.getBytes(UTF_8)).toString();

    return effects().asyncReply(
      getReservation(cancelledReservationConfirmed.reservationId()).thenCompose(reservation ->
        refund(reservation.walletId(), reservation.price(), commandId)
      )
    );
  }

  private CompletionStage<Reservation> getReservation(String reservationId) {
    return componentClient.forValueEntity(reservationId)
      .call(ReservationEntity::get)
      .execute();
  }

  private CompletionStage<Response> refund(String walletId, BigDecimal amount, String commandId) {
    return componentClient.forEventSourcedEntity(walletId)
      .call(WalletEntity::deposit)
      .params(new WalletCommand.DepositFunds(amount, commandId))
      .execute();
  }
}
