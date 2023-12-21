package com.example.cinema.application;

import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.ShowByReservation;
import com.example.cinema.domain.ShowCommand;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.application.WalletResponse;
import com.example.wallet.domain.WalletCommand;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.client.ComponentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

@Component
public class Calls {

  @Autowired
  private WebClient webClient;

  @Autowired
  private ComponentClient componentClient;

  private Duration timeout = Duration.ofSeconds(10);

  public void createShow(String showId, String title) {
    int maxSeats = 100;

    execute(componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::create)
      .params(new ShowCommand.CreateShow(title, maxSeats)));
  }

  public SeatStatus getSeatStatus(String showId, int seatNumber) {
    return execute(componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::getSeatStatus)
      .params(seatNumber));
  }

  public void reserveSeat(String showId, String walletId, String reservationId, int seatNumber) {
    execute(componentClient.forEventSourcedEntity(showId)
      .call(ShowEntity::reserve)
      .params(new ShowCommand.ReserveSeat(walletId, reservationId, seatNumber)));
  }

  public ResponseEntity<ShowByReservation> getShowByReservation(String reservationId) {
    return webClient.get().uri("/show/by-reservation-id/" + reservationId)
      .retrieve()
      .toEntity(ShowByReservation.class)
      .onErrorResume(WebClientResponseException.class, error -> {
        if (error.getStatusCode().is4xxClientError()) {
          return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else {
          return Mono.error(error);
        }
      })
      .block();
  }

  public void createWallet(String walletId, int amount) {
    execute(componentClient.forEventSourcedEntity(walletId)
      .call(WalletEntity::create)
      .params(amount));
  }

  public WalletResponse getWallet(String walletId) {
    return execute(componentClient.forEventSourcedEntity(walletId)
      .call(WalletEntity::get));
  }

  public void chargeWallet(String walletId, WalletCommand.ChargeWallet chargeWallet) {
    execute(componentClient.forEventSourcedEntity(walletId)
      .call(WalletEntity::charge)
      .params(chargeWallet)
      .withMetadata(Metadata.EMPTY.add("skip-failure-simulation", "true")));
  }

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
