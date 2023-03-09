package com.example.cinema.application;

import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Subscribe.EventSourcedEntity(value = WalletEntity.class, ignoreUnknown = true)
public class ReservationPaymentConfirmation extends Action {

  private Logger logger = LoggerFactory.getLogger(ReservationPayment.class);

  private final KalixClient kalixClient;

  public ReservationPaymentConfirmation(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<String> confirmReservation(WalletCharged walletCharged) {
    logger.info("confirming reservation, triggered by " + walletCharged);

    String reservationId = walletCharged.reservationId();
    String showId = "show1";

    return effects().forward(confirmReservation(showId, reservationId));
  }

  public Effect<String> cancelReservation(WalletChargeRejected walletChargeRejected) {
    logger.info("cancelling reservation, triggered by " + walletChargeRejected);

    String reservationId = walletChargeRejected.reservationId();
    String showId = "show1";

    return effects().forward(cancelReservation(showId, reservationId));
  }

  private DeferredCall<Any, String> confirmReservation(String showId, String reservationId) {
    return kalixClient.patch("/cinema-show/" + showId + "/confirm-payment/" + reservationId, String.class);
  }

  private DeferredCall<Any, String> cancelReservation(String showId, String reservationId) {
    return kalixClient.patch("/cinema-show/" + showId + "/cancel-reservation/" + reservationId, String.class);
  }
}
