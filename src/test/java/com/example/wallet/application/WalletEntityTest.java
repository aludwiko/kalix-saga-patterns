package com.example.wallet.application;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.cinema.domain.DomainGenerators.randomWalletId;
import static org.assertj.core.api.Assertions.assertThat;

class WalletEntityTest {


  @Test
  public void shouldCreateWallet() {
    //given
    var walletId = randomWalletId();
    var initialAmount = 100;
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(WalletEntity::new);

    //when
    EventSourcedResult<String> result = testKit.call(wallet -> wallet.create(walletId, initialAmount));

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.getNextEventOfType(WalletCreated.class).initialAmount()).isEqualTo(BigDecimal.valueOf(initialAmount));
    assertThat(testKit.getState().id()).isEqualTo(walletId);
    assertThat(testKit.getState().balance()).isEqualTo(BigDecimal.valueOf(initialAmount));
  }

  @Test
  public void shouldChargeWallet() {
    //given
    var walletId = randomWalletId();
    var initialAmount = 100;
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(WalletEntity::new);
    testKit.call(wallet -> wallet.create(walletId, initialAmount));
    var chargeWallet = new WalletCommand.ChargeWallet(new BigDecimal(10), "r1");

    //when
    EventSourcedResult<String> result = testKit.call(wallet -> wallet.charge(chargeWallet));

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.getNextEventOfType(WalletCharged.class)).isEqualTo(new WalletCharged(walletId, chargeWallet.amount(), chargeWallet.expenseId()));
    assertThat(testKit.getState().id()).isEqualTo(walletId);
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }
}