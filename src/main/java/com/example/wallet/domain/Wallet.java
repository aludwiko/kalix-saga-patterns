package com.example.wallet.domain;

import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletEvent.FundsDeposited;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import io.vavr.control.Either;

import java.math.BigDecimal;

import static com.example.wallet.domain.WalletCommandError.DEPOSIT_LE_ZERO;
import static com.example.wallet.domain.WalletCommandError.WALLET_ALREADY_EXISTS;

public record Wallet(String id, BigDecimal balance) {

  public static Wallet create(WalletCreated walletCreated) {
    return new Wallet(walletCreated.walletId(), walletCreated.initialAmount());
  }

  public Either<WalletCommandError, WalletEvent> process(WalletCommand command) {
    return switch (command) {
      case CreateWallet ignored -> Either.left(WALLET_ALREADY_EXISTS);
      case ChargeWallet charge -> handleCharge(charge);
      case DepositFunds depositFunds -> handleDeposit(depositFunds);
    };
  }

  private Either<WalletCommandError, WalletEvent> handleDeposit(DepositFunds depositFunds) {
    if (depositFunds.amount().compareTo(BigDecimal.ZERO) <= 0) {
      return Either.left(DEPOSIT_LE_ZERO);
    } else {
      return Either.right(new FundsDeposited(id, depositFunds.amount()));
    }
  }

  private Either<WalletCommandError, WalletEvent> handleCharge(ChargeWallet charge) {
    if (balance.compareTo(charge.amount()) < 0) {
      return Either.right(new WalletChargeRejected(id, charge.expenseId()));
    } else {
      return Either.right(new WalletCharged(id, charge.amount(), charge.expenseId()));
    }
  }

  public Wallet apply(WalletEvent event) {
    return switch (event) {
      case WalletCreated ignored -> throw new IllegalStateException("Wallet is already created, use Wallet.create instead.");
      case WalletCharged charged -> new Wallet(id, balance.subtract(charged.amount()));
      case FundsDeposited deposited -> new Wallet(id, balance.add(deposited.amount()));
      case WalletChargeRejected __ -> this;
    };
  }
}
