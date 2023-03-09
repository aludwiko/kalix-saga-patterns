package com.example.cinema.domain;

import java.math.BigDecimal;

public sealed interface ShowEvent {
  String showId();

  record ShowCreated(String showId, InitialShow initialShow) implements ShowEvent {
  }

  record SeatReserved(String showId, String walletId, String reservationId, int seatNumber, BigDecimal price) implements ShowEvent {
  }

  record SeatReservationPaid(String showId, String reservationId, int seatNumber) implements ShowEvent {
  }

  record SeatReservationCancelled(String showId, String reservationId, int seatNumber) implements ShowEvent {
  }
}
