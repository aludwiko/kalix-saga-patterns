package com.example.wallet.application;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.FundsDeposited;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

import static kalix.javasdk.StatusCode.ErrorCode.BAD_REQUEST;
import static kalix.javasdk.StatusCode.ErrorCode.NOT_FOUND;

@Id("id")
@TypeId("wallet")
@RequestMapping("/wallet/{id}")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PostMapping("/create/{initialBalance}")
  public Effect<String> create(@PathVariable String id, @PathVariable int initialBalance) {
    if (currentState() != null) {
      logger.warn("wallet already exists");
      return effects().error("wallet already exists", BAD_REQUEST);
    } else {
      return effects()
          .emitEvent(new WalletCreated(id, BigDecimal.valueOf(initialBalance)))
          .thenReply(__ -> {
            logger.info("wallet {} created, init balance {}", id, initialBalance);
            return "wallet created";
          });
    }
  }

  @GetMapping
  public Effect<Wallet> get() {
    if (currentState() == null) {
      return effects().error("wallet not created", NOT_FOUND);
    } else {
      return effects().reply(currentState());
    }
  }

  @PatchMapping("/charge")
  public Effect<String> charge(@RequestBody ChargeWallet chargeWallet) {
    if (currentState() == null) {
      logger.error("wallet not exists");
      return effects().error("wallet not exists", NOT_FOUND);
    } else {
      return currentState().process(chargeWallet).fold(
          error -> {
            logger.error("processing command {} failed with {}", chargeWallet, error);
            return effects().error(error.name(), BAD_REQUEST);
          },
          event -> effects().emitEvent(event).thenReply(__ -> {
            logger.info("charging wallet completed {}", chargeWallet);
            return "wallet charged";
          })
      );
    }
  }

  @PatchMapping("/deposit/{amount}")
  public Effect<String> deposit(@PathVariable int amount) {
    if (currentState() == null) {
      logger.error("wallet not exists");
      return effects().error("wallet not exists", NOT_FOUND);
    } else {
      DepositFunds depositFunds = new DepositFunds(BigDecimal.valueOf(amount));
      return currentState().process(depositFunds).fold(
          error -> {
            logger.error("processing command {} failed with {}", depositFunds, error);
            return effects().error(error.name(), BAD_REQUEST);
          },
          event -> effects().emitEvent(event).thenReply(__ -> {
            logger.info("deposit funds completed {}", depositFunds);
            return "funds deposited";
          })
      );
    }
  }

  @EventHandler
  public Wallet onEvent(WalletCreated walletCreated) {
    return Wallet.create(walletCreated);
  }

  @EventHandler
  public Wallet onEvent(WalletCharged walletCharged) {
    return currentState().apply(walletCharged);
  }

  @EventHandler
  public Wallet onEvent(FundsDeposited fundsDeposited) {
    return currentState().apply(fundsDeposited);
  }

  @EventHandler
  public Wallet onEvent(WalletChargeRejected walletCharged) {
    return currentState().apply(walletCharged);
  }
}
