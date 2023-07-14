package com.example.cinema.domain;

import static com.example.cinema.domain.ReservationStatus.CANCELLED;
import static com.example.cinema.domain.ReservationStatus.CONFIRMED;

public record FinishedReservation(String reservationId, int seatNumber, ReservationStatus status) {
  public boolean isConfirmed() {
    return status == CONFIRMED;
  }

  public boolean isCancelled() {
    return status == CANCELLED;
  }
}
