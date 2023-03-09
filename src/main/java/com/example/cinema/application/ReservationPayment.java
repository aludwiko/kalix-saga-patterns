package com.example.cinema.application;

import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Subscribe.EventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ReservationPayment extends Action {

  private Logger logger = LoggerFactory.getLogger(ReservationPayment.class);

  private final KalixClient kalixClient;

  public ReservationPayment(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public Effect<String> chargeForReservation(SeatReserved seatReserved) {
    logger.info("charging for reservation, triggered by " + seatReserved);
    String reservationId = seatReserved.reservationId();
    var chargeWallet = new ChargeWallet(seatReserved.price(), reservationId);

    var chargeCall = kalixClient.patch("/wallet/" + seatReserved.walletId() + "/charge", chargeWallet, String.class);

    return effects().forward(chargeCall);
  }
}
