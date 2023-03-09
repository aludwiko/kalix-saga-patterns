package com.example.wallet.domain;

import java.math.BigDecimal;

public sealed interface WalletCommand {

  record CreateWallet(BigDecimal initialAmount) implements WalletCommand {
  }

  record ChargeWallet(BigDecimal amount, String reservationId) implements WalletCommand {
  }

  record DepositFunds(BigDecimal amount) implements WalletCommand {
  }
}
