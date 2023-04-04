package com.example.cinema.application;

import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    var chargeWallet = new ChargeWallet(seatReserved.price(), expenseId);

    var chargeCall = componentClient.forEventSourcedEntity(seatReserved.walletId()).call(WalletEntity::charge).params(chargeWallet);

    return effects().forward(chargeCall);
  }
}
