package com.example.cinema.application;

import com.example.wallet.domain.WalletCommand.ChargeWallet;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Id("id")
@TypeId("wallet-failure")
@RequestMapping("/wallet-failure/{id}")
public class WalletFailureEntity extends EventSourcedEntity<WalletFailureEntity.WalletFailureState, WalletFailureEntity.WalletFailureEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public record WalletFailureState(int numberOfFailures) {
    public WalletFailureState inc() {
      return new WalletFailureState(numberOfFailures + 1);
    }
  }

  interface WalletFailureEvent {
  }

  public record WalletChargeFailureOccurred(ChargeWallet source, String msg) implements WalletFailureEvent {
  }

  @Override
  public WalletFailureState emptyState() {
    return new WalletFailureState(0);
  }

  @PostMapping
  public Effect<String> registerChargeError(@RequestBody ChargeWallet source, @RequestParam String msg) {
    return effects().emitEvent(new WalletChargeFailureOccurred(source, msg))
      .thenReply(__ -> "registered");
  }

  @EventHandler
  public WalletFailureState apply(WalletChargeFailureOccurred __) {
    return currentState().inc();
  }
}
