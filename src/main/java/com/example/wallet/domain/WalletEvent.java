package com.example.wallet.domain;

import java.math.BigDecimal;

public sealed interface WalletEvent {

  record WalletCreated(String walletId, BigDecimal initialAmount) implements WalletEvent {
  }

  record WalletCharged(String walletId, BigDecimal amount, String reservationId) implements WalletEvent {
  }

  record FundsDeposited(String walletId, BigDecimal amount) implements WalletEvent {
  }

  record WalletChargeRejected(String walletId, String reservationId) implements WalletEvent {
  }
}
