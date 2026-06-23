package de.itsec.api.controllers.v1;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.TerminBookingRequestDto;
import de.itsec.api.data.dto.request.TerminCreateRequestDto;
import de.itsec.api.data.dto.response.TerminDto;
import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import de.itsec.api.services.TerminService;
import de.itsec.api.services.UserService;
import jakarta.validation.Valid;

/**
 * User-facing appointment endpoints. There are intentionally no endpoints for managing a {@link
 * de.itsec.api.data.termin.Praxis}.
 */
@RestController
@RequestMapping("/api/v1/termine")
public class TerminController {

  private final TerminService terminService;
  private final UserService userService;

  @Autowired
  public TerminController(TerminService terminService, UserService userService) {
    this.terminService = terminService;
    this.userService = userService;
  }

  /**
   * Lists free, future slots. Without {@code praxisId} all praxen are returned; each slot includes
   * its praxis, so the client can pick one without a separate praxis lookup.
   */
  @GetMapping("/free")
  public List<TerminDto> getFreeSlots(
      @RequestParam(required = false) String plz,
      @RequestParam(required = false) UUID praxisId,
      @RequestParam(required = false) LocalDateTime from) {
    return terminService.getFreeSlots(praxisId, plz, from).stream().map(TerminDto::from).toList();
  }

  /**
   * Filtered slot list. All query params are optional: {@code praxisId}, {@code postalCode}, {@code
   * status} ({@code FREE}/{@code BOOKED}/{@code CANCELLED}) and a {@code from}/{@code to} start-time
   * range. Returns the public view, which never exposes other users' notes.
   */
  @GetMapping("/search")
  public List<TerminDto> search(
      @RequestParam(required = false) UUID praxisId,
      @RequestParam(required = false) String postalCode,
      @RequestParam(required = false) TerminStatus status,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to) {
    return terminService.filter(praxisId, postalCode, status, from, to).stream()
        .map(TerminDto::publicView)
        .toList();
  }

  /**
   * Lists the current user's own appointments. All query params are optional and let the user narrow
   * down to e.g. their booked appointments at a praxis on a given day: {@code praxisId}, {@code
   * status} ({@code FREE}/{@code BOOKED}/{@code CANCELLED}) and a {@code from}/{@code to} start-time
   * range. Notes are included because these are the caller's own appointments.
   */
  @GetMapping("/mine")
  public List<TerminDto> getMyAppointments(
      @RequestParam(required = false) UUID praxisId,
      @RequestParam(required = false) TerminStatus status,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to,
      Principal principal) {
    UUID userId = currentUserId(principal);
    return terminService.getAppointmentsForUser(userId, praxisId, status, from, to).stream()
        .map(TerminDto::from)
        .toList();
  }

  /** Creates a new free slot at a praxis. Restricted to admin/staff. */
  @PostMapping
  @Secured({"ROLE_ADMIN", "ROLE_STAFF"})
  public ResponseEntity<TerminDto> createSlot(@Valid @RequestBody TerminCreateRequestDto request) {
    Termin slot =
        terminService.createSlot(
            request.praxisId(), request.startTime(), request.endTime(), request.vaccine());
    return new ResponseEntity<>(TerminDto.from(slot), HttpStatus.CREATED);
  }

  /** Books a free slot for the current user. */
  @PostMapping("/{slotId}/book")
  public ResponseEntity<TerminDto> book(
      @PathVariable UUID slotId,
      @RequestBody(required = false) TerminBookingRequestDto request,
      Principal principal) {
    UUID userId = currentUserId(principal);
    String note = request != null ? request.note() : null;
    Termin booked = terminService.book(slotId, userId, note);
    return ResponseEntity.ok(TerminDto.from(booked));
  }

  /** Cancels one of the current user's appointments, releasing the slot. */
  @PostMapping("/{slotId}/cancel")
  public ResponseEntity<Void> cancel(@PathVariable UUID slotId, Principal principal) {
    UUID userId = currentUserId(principal);
    terminService.cancel(slotId, userId);
    return ResponseEntity.noContent().build();
  }

  private UUID currentUserId(Principal principal) {
    User user = userService.getUserByUsername(principal.getName());
    return user.getId();
  }
}
