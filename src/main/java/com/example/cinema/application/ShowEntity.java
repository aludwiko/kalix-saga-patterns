package com.example.cinema.application;

import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.Show;
import com.example.cinema.domain.ShowCommand;
import com.example.cinema.domain.ShowCommand.CancelSeatReservation;
import com.example.cinema.domain.ShowCommand.ConfirmReservationPayment;
import com.example.cinema.domain.ShowCommand.CreateShow;
import com.example.cinema.domain.ShowCommand.ReserveSeat;
import com.example.cinema.domain.ShowCommandError;
import com.example.cinema.domain.ShowCreator;
import com.example.cinema.domain.ShowEvent;
import com.example.cinema.domain.ShowEvent.CancelledReservationConfirmed;
import com.example.cinema.domain.ShowEvent.SeatReservationCancelled;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.cinema.domain.ShowEvent.ShowCreated;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.function.Predicate;

import static com.example.cinema.domain.ShowCommandError.CANCELLING_CONFIRMED_RESERVATION;
import static com.example.cinema.domain.ShowCommandError.DUPLICATED_COMMAND;
import static kalix.javasdk.StatusCode.ErrorCode.BAD_REQUEST;
import static kalix.javasdk.StatusCode.ErrorCode.NOT_FOUND;

@Id("id")
@TypeId("cinema-show")
@RequestMapping("/cinema-show/{id}")
public class ShowEntity extends EventSourcedEntity<Show, ShowEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @PostMapping
  public Effect<String> create(@PathVariable String id, @RequestBody CreateShow createShow) {
    if (currentState() != null) {
      return effects().error("show already exists", BAD_REQUEST);
    } else {
      return ShowCreator.create(id, createShow).fold(
        error -> errorEffect(error, createShow),
        showCreated -> persistEffect(showCreated, "show created")
      );
    }
  }

  @PatchMapping("/reserve")
  public Effect<String> reserve(@RequestBody ReserveSeat reserveSeat) {
    if (currentState() == null) {
      return effects().error("show not exists", NOT_FOUND);
    } else {
      return currentState().process(reserveSeat).fold(
        error -> errorEffect(error, reserveSeat),
        showEvent -> persistEffect(showEvent, "reserved")
      );
    }
  }

  @PatchMapping("/cancel-reservation/{reservationId}")
  public Effect<String> cancelReservation(@PathVariable String reservationId) {
    if (currentState() == null) {
      return effects().error("show not exists", NOT_FOUND);
    } else {
      CancelSeatReservation cancelSeatReservation = new CancelSeatReservation(reservationId);
      return currentState().process(cancelSeatReservation).fold(
        error -> errorEffect(error, cancelSeatReservation, e -> e == DUPLICATED_COMMAND || e == CANCELLING_CONFIRMED_RESERVATION),
        showEvent -> persistEffect(showEvent, "reservation cancelled")
      );
    }
  }

  @PatchMapping("/confirm-payment/{reservationId}")
  public Effect<String> confirmPayment(@PathVariable String reservationId) {
    if (currentState() == null) {
      return effects().error("show not exists", NOT_FOUND);
    } else {
      ConfirmReservationPayment confirmReservationPayment = new ConfirmReservationPayment(reservationId);
      return currentState().process(confirmReservationPayment).fold(
        error -> errorEffect(error, confirmReservationPayment),
        showEvent -> persistEffect(showEvent, "payment confirmed")
      );
    }
  }

  private Effect<String> persistEffect(ShowEvent showEvent, String message) {
    return effects()
      .emitEvent(showEvent)
      .thenReply(__ -> message);
  }

  private Effect<String> errorEffect(ShowCommandError error, ShowCommand showCommand) {
    return errorEffect(error, showCommand, e -> e == DUPLICATED_COMMAND);
  }

  private Effect<String> errorEffect(ShowCommandError error, ShowCommand showCommand, Predicate<ShowCommandError> shouldBeSuccessful) {
    if (shouldBeSuccessful.test(error)) {
      return effects().reply("ok");
    } else {
      logger.error("processing command {} failed with {}", showCommand, error);
      return effects().error(error.name(), BAD_REQUEST);
    }
  }

  @GetMapping
  public Effect<ShowResponse> get() {
    if (currentState() == null) {
      return effects().error("show not exists", NOT_FOUND);
    } else {
      return effects().reply(ShowResponse.from(currentState()));
    }
  }

  @GetMapping("/seat-status/{seatNumber}")
  public Effect<SeatStatus> getSeatStatus(@PathVariable int seatNumber) {
    if (currentState() == null) {
      return effects().error("show not exists", NOT_FOUND);
    } else {
      return currentState().seats().get(seatNumber).fold(
        () -> effects().error("seat not exists", NOT_FOUND),
        seat -> effects().reply(seat.status())
      );
    }
  }

  @EventHandler
  public Show onEvent(ShowCreated showCreated) {
    return Show.create(showCreated);
  }

  @EventHandler
  public Show onEvent(SeatReserved seatReserved) {
    return currentState().apply(seatReserved);
  }

  @EventHandler
  public Show onEvent(SeatReservationCancelled seatReservationCancelled) {
    return currentState().apply(seatReservationCancelled);
  }

  @EventHandler
  public Show onEvent(SeatReservationPaid seatReservationPaid) {
    return currentState().apply(seatReservationPaid);
  }

  @EventHandler
  public Show onEvent(CancelledReservationConfirmed cancelledReservationConfirmed) {
    return currentState().apply(cancelledReservationConfirmed);
  }
}
