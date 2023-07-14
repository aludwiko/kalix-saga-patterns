package com.example.wallet.application;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletCommandError;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.FundsDeposited;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.ForwardHeaders;
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

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static kalix.javasdk.StatusCode.ErrorCode.BAD_REQUEST;
import static kalix.javasdk.StatusCode.ErrorCode.NOT_FOUND;

@Id("id")
@TypeId("wallet")
@RequestMapping("/wallet/{id}")
@ForwardHeaders("skip-failure-simulation")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Wallet emptyState() {
    return Wallet.EMPTY_WALLET;
  }

  @PostMapping("/create/{initialBalance}")
  public Effect<String> create(@PathVariable String id, @PathVariable int initialBalance) {
    WalletCommand.CreateWallet createWallet = new WalletCommand.CreateWallet(id, BigDecimal.valueOf(initialBalance));
    return currentState().process(createWallet).fold(
      error -> errorEffect(error, createWallet),
      event -> persistEffect(event, "wallet created", createWallet)
    );
  }

  @PatchMapping("/charge")
  public Effect<String> charge(@RequestBody ChargeWallet chargeWallet) {
    if (chargeWallet.expenseId().equals("42") && commandContext().metadata().get("skip-failure-simulation").isEmpty()) {
      logger.info("charging failed");
      return effects().error("Unexpected error for expenseId=42", INVALID_ARGUMENT);
    } else {
      return currentState().process(chargeWallet).fold(
        error -> errorEffect(error, chargeWallet),
        event -> persistEffect(event, "wallet charged", chargeWallet)
      );
    }
  }

  @PatchMapping("/deposit")
  public Effect<String> deposit(@RequestBody DepositFunds depositFunds) {
    return currentState().process(depositFunds).fold(
      error -> errorEffect(error, depositFunds),
      event -> persistEffect(event, "funds deposited", depositFunds)
    );
  }

  @GetMapping
  public Effect<WalletResponse> get() {
    if (currentState().isEmpty()) {
      return effects().error("wallet not created", NOT_FOUND);
    } else {
      return effects().reply(WalletResponse.from(currentState()));
    }
  }

  private Effect<String> persistEffect(WalletEvent event, String replyMessage, WalletCommand walletCommand) {
    return effects()
      .emitEvent(event)
      .thenReply(__ -> {
        logger.info("processing command {} completed", walletCommand);
        return replyMessage;
      });
  }

  private Effect<String> errorEffect(WalletCommandError error, WalletCommand walletCommand) {
    if (error.equals(WalletCommandError.DUPLICATED_COMMAND)) {
      logger.debug("Ignoring duplicated command {}", walletCommand);
      return effects().reply("Ignoring duplicated command");
    } else {
      logger.warn("processing {} failed with {}", walletCommand, error);
      return effects().error(error.name(), BAD_REQUEST);
    }
  }

  @EventHandler
  public Wallet onEvent(WalletCreated walletCreated) {
    return currentState().apply(walletCreated);
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
