package com.example.cinema.application;

import com.example.cinema.domain.Reservation;
import io.grpc.Status;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.valueentity.ValueEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("reservation")
@RequestMapping("/reservation/{id}")
public class ReservationEntity extends ValueEntity<Reservation> {

  @GetMapping
  public Effect<Reservation> get() {
    if (currentState() == null) {
      return effects().error("reservation not found", Status.Code.NOT_FOUND);
    } else {
      return effects().reply(currentState());
    }
  }

  @PostMapping("/{showId}")
  public Effect<String> create(@PathVariable String showId) {
    String reservationId = commandContext().entityId();
    return effects().updateState(new Reservation(reservationId, showId)).thenReply("reservation created");
  }

  @DeleteMapping
  public Effect<String> delete() {
    return effects().deleteEntity().thenReply("reservation deleted");
  }
}
