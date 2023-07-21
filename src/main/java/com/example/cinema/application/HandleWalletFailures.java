package com.example.cinema.application;

import com.example.cinema.application.WalletFailureEntity.WalletChargeFailureOccurred;
import com.example.cinema.domain.Reservation;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.CompletionStage;

@Profile("choreography")
@Subscribe.EventSourcedEntity(value = WalletFailureEntity.class)
public class HandleWalletFailures extends Action {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public HandleWalletFailures(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<Response> handle(WalletChargeFailureOccurred walletChargeFailureOccurred) {
    logger.info("handling failure: " + walletChargeFailureOccurred);

    String reservationId = walletChargeFailureOccurred.source().expenseId();

    return effects().asyncReply(getShowIdBy(reservationId).thenCompose(showId ->
      cancelReservation(reservationId, showId)
    ));
  }

  private CompletionStage<Response> cancelReservation(String reservationId, String showId) {
    return componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::cancelReservation)
      .params(reservationId)
      .execute();
  }

  private CompletionStage<String> getShowIdBy(String reservationId) {
    return componentClient.forValueEntity(reservationId).call(ReservationEntity::get).execute()
      .thenApply(Reservation::showId);
  }
}
