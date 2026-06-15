package de.itsec.api.controllers.v1;

import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.TerminBookingRequestDto;
import de.itsec.api.data.dto.response.TerminDto;
import de.itsec.api.data.termin.Termin;
import de.itsec.api.services.TerminService;
import de.itsec.api.services.UserService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
  public List<TerminDto> getFreeSlots(@RequestParam(required = false) UUID praxisId) {
    return terminService.getFreeSlots(praxisId).stream().map(TerminDto::from).toList();
  }

  /** Lists the current user's own appointments. */
  @GetMapping("/mine")
  public List<TerminDto> getMyAppointments(Principal principal) {
    UUID userId = currentUserId(principal);
    return terminService.getAppointmentsForUser(userId).stream().map(TerminDto::from).toList();
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
