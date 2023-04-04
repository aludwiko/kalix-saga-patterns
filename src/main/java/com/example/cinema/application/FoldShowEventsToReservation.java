package com.example.cinema.application;

import com.example.cinema.domain.ShowEvent.SeatReservationCancelled;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Subscribe.EventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class FoldShowEventsToReservation extends Action {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public FoldShowEventsToReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect<String> onEvent(SeatReserved reserved) {
    return effects().forward(createReservation(reserved.reservationId(), reserved.showId()));
  }

  public Effect<String> onEvent(SeatReservationPaid paid) {
    return effects().forward(deleteReservation(paid.reservationId()));
  }

  public Effect<String> onEvent(SeatReservationCancelled cancelled) {
    return effects().forward(deleteReservation(cancelled.reservationId()));
  }

  private DeferredCall<Any, String> createReservation(String reservationId, String showId) {
    return componentClient.forValueEntity(reservationId).call(ReservationEntity::create).params(showId);
  }

  private DeferredCall<Any, String> deleteReservation(String reservationId) {
    return componentClient.forValueEntity(reservationId).call(ReservationEntity::delete);
  }
}
