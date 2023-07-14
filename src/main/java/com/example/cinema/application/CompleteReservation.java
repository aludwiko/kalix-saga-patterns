package com.example.cinema.application;

import com.example.cinema.domain.Reservation;
import com.example.cinema.domain.ShowByReservation;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@Subscribe.EventSourcedEntity(value = WalletEntity.class, ignoreUnknown = true)
public class CompleteReservation extends Action {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public CompleteReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<String> confirmReservation(WalletCharged walletCharged) {
    logger.info("confirming reservation, triggered by " + walletCharged);

    String reservationId = walletCharged.expenseId();

    return effects().asyncReply(
      getShowIdBy(reservationId).thenCompose(showId ->
        confirmReservation(showId, reservationId)
      ));
  }

  public Effect<String> cancelReservation(WalletChargeRejected walletChargeRejected) {
    logger.info("cancelling reservation, triggered by " + walletChargeRejected);

    String reservationId = walletChargeRejected.expenseId();

    return effects().asyncReply(
      getShowIdBy(reservationId).thenCompose(showId ->
        cancelReservation(showId, reservationId)
      ));
  }

  private CompletionStage<String> confirmReservation(String showId, String reservationId) {
    return componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::confirmPayment)
      .params(reservationId)
      .execute();
  }

  private CompletionStage<String> cancelReservation(String showId, String reservationId) {
    return componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::cancelReservation)
      .params(reservationId)
      .execute();
  }

  //Value Entity as a read model
  private CompletionStage<String> getShowIdBy(String reservationId) {
    return componentClient.forValueEntity(reservationId).call(ReservationEntity::get).execute()
      .thenApply(Reservation::showId);
  }

  //View as a read model
  private CompletionStage<String> getShowIdBy2(String reservationId) {
    return componentClient.forView().call(ShowByReservationView::getShow).params(reservationId).execute()
      .thenApply(ShowByReservation::showId);
  }
}
