package com.example.cinema.application;

import com.example.cinema.domain.ShowByReservation;
import com.example.cinema.domain.ShowEvent.CancelledReservationConfirmed;
import com.example.cinema.domain.ShowEvent.SeatReservationCancelled;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.cinema.domain.ShowEvent.ShowCreated;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import kalix.javasdk.view.View;
import org.springframework.web.bind.annotation.GetMapping;

@ViewId("show_by_reservation_view")
@Table("show_by_reservation")
@Subscribe.EventSourcedEntity(value = ShowEntity.class)
public class ShowByReservationView extends View<ShowByReservation> {

  @GetMapping("/show/by-reservation-id/{reservationId}")
  @Query("SELECT * FROM show_by_reservation WHERE :reservationId = ANY(reservationIds)")
  public ShowByReservation getShow(String name) {
    return null;
  }

  public UpdateEffect<ShowByReservation> onEvent(ShowCreated created) {
    return effects().updateState(new ShowByReservation(created.showId()));
  }

  public UpdateEffect<ShowByReservation> onEvent(SeatReserved reserved) {
    return effects().updateState(viewState().add(reserved.reservationId()));
  }

  public UpdateEffect<ShowByReservation> onEvent(SeatReservationPaid paid) {
    return effects().ignore();
  }

  public UpdateEffect<ShowByReservation> onEvent(SeatReservationCancelled cancelled) {
    return effects().ignore();
  }

  public UpdateEffect<ShowByReservation> onEvent(CancelledReservationConfirmed confirmed) {
    return effects().ignore();
  }
}
