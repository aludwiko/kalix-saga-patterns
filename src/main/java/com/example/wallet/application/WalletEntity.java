package com.example.wallet.application;

import com.example.cinema.application.Response;
import com.example.cinema.application.Response.Failure;
import com.example.cinema.application.Response.Success;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletCommand.Refund;
import com.example.wallet.domain.WalletCommandError;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.FundsDeposited;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import com.example.wallet.domain.WalletEvent.WalletRefunded;
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
import java.util.function.Function;

import static com.example.wallet.domain.WalletCommandError.EXPENSE_NOT_FOUND;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
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
  public Effect<Response> create(@PathVariable int initialBalance) {
    String id = commandContext().entityId();
    WalletCommand.CreateWallet createWallet = new WalletCommand.CreateWallet(id, BigDecimal.valueOf(initialBalance));
    return currentState().process(createWallet).fold(
      error -> errorEffect(error, createWallet),
      event -> persistEffect(event, "wallet created", createWallet)
    );
  }

  @PatchMapping("/charge")
  public Effect<Response> charge(@RequestBody ChargeWallet chargeWallet) {
    if (chargeWallet.expenseId().equals("42") && commandContext().metadata().get("skip-failure-simulation").isEmpty()) {
      logger.info("charging failed");
      return effects().error("Unexpected error for expenseId=42", INVALID_ARGUMENT);
    } else {
      return currentState().process(chargeWallet).fold(
        error -> errorEffect(error, chargeWallet),
        event -> persistEffect(event, e -> {
          if (e instanceof WalletChargeRejected) {
            return Failure.of("wallet charge rejected");
          } else {
            return Success.of("wallet charged");
          }
        }, chargeWallet)
      );
    }
  }

  @PatchMapping("/refund")
  public Effect<Response> refund(@RequestBody Refund refund) {
    return currentState().process(refund).fold(
      error -> {
        if (error == EXPENSE_NOT_FOUND) {
          return effects().reply(Success.of("ignoring"));
        } else {
          return errorEffect(error, refund);
        }
      },
      event -> persistEffect(event, "funds deposited", refund)
    );
  }

  @PatchMapping("/deposit")
  public Effect<Response> deposit(@RequestBody DepositFunds depositFunds) {
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

  private Effect<Response> persistEffect(WalletEvent event, Function<WalletEvent, Response> eventToResponse, WalletCommand walletCommand) {
    return effects()
      .emitEvent(event)
      .thenReply(__ -> {
        logger.info("processing command {} completed", walletCommand);
        return eventToResponse.apply(event);
      });
  }

  private Effect<Response> persistEffect(WalletEvent event, String replyMessage, WalletCommand walletCommand) {
    return persistEffect(event, e -> Success.of(replyMessage), walletCommand);
  }

  private Effect<Response> errorEffect(WalletCommandError error, WalletCommand walletCommand) {
    if (error.equals(WalletCommandError.DUPLICATED_COMMAND)) {
      logger.debug("Ignoring duplicated command {}", walletCommand);
      return effects().reply(Success.of("Ignoring duplicated command"));
    } else {
      logger.warn("processing {} failed with {}", walletCommand, error);
      return effects().reply(Failure.of(error.name()));
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
  public Wallet onEvent(WalletRefunded walletRefunded) {
    return currentState().apply(walletRefunded);
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
