package de.itsec.api.controllers.v1;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.itsec.api.PermissionRoles;
import de.itsec.api.data.authentication.User;
import de.itsec.api.data.dto.request.TerminCreateRequestDto;
import de.itsec.api.data.dto.response.TerminDto;
import de.itsec.api.data.termin.Termin;
import de.itsec.api.data.termin.TerminStatus;
import de.itsec.api.services.StaffPraxisService;
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
  private final StaffPraxisService staffPraxisService;

  @Autowired
  public TerminController(
      TerminService terminService,
      UserService userService,
      StaffPraxisService staffPraxisService) {
    this.terminService = terminService;
    this.userService = userService;
    this.staffPraxisService = staffPraxisService;
  }

  /**
   * Lists free, future slots. Without {@code praxisId} all praxen are returned; each slot includes
   * its praxis, so the client can pick one without a separate praxis lookup.
   */
  @GetMapping("/free")
  public Page<TerminDto> getFreeSlots(
      @RequestParam(required = false) String plz,
      @RequestParam(required = false) UUID praxisId,
      @RequestParam(required = false) LocalDateTime from,
      @PageableDefault(sort = "startTime") Pageable pageable) {
    return terminService.getFreeSlots(praxisId, plz, from, pageable).map(TerminDto::from);
  }

  /**
   * Filtered slot list including booked/cancelled slots — an operator view, restricted to admin and
   * staff. Admins may search any praxis; a staff member is always scoped to their own praxis (a
   * foreign {@code praxisId} is rejected). All other params are optional: {@code postalCode}, {@code
   * status} ({@code FREE}/{@code BOOKED}/{@code CANCELLED}) and a {@code from}/{@code to} start-time
   * range. Regular users browse free slots via {@code /free}.
   */
  @GetMapping("/search")
  @Secured({"ROLE_ADMIN", "ROLE_STAFF"})
  public Page<TerminDto> search(
      @RequestParam(required = false) UUID praxisId,
      @RequestParam(required = false) String postalCode,
      @RequestParam(required = false) TerminStatus status,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to,
      @PageableDefault(sort = "startTime") Pageable pageable,
      Principal principal) {
    User caller = userService.getUserByUsername(principal.getName());
    UUID scopedPraxisId = scopePraxisForSearch(caller, praxisId);
    return terminService
        .filter(scopedPraxisId, postalCode, status, from, to, pageable)
        .map(TerminDto::from);
  }

  /**
   * Lists the current user's own appointments. All query params are optional and let the user narrow
   * down to e.g. their booked appointments at a praxis on a given day: {@code praxisId}, {@code
   * status} ({@code FREE}/{@code BOOKED}/{@code CANCELLED}) and a {@code from}/{@code to} start-time
   * range.
   */
  @GetMapping("/mine")
  public Page<TerminDto> getMyAppointments(
      @RequestParam(required = false) UUID praxisId,
      @RequestParam(required = false) TerminStatus status,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to,
      @PageableDefault(sort = "startTime") Pageable pageable,
      Principal principal) {
    UUID userId = currentUserId(principal);
    return terminService
        .getAppointmentsForUser(userId, praxisId, status, from, to, pageable)
        .map(TerminDto::from);
  }

  /**
   * Creates a new free slot at a praxis. Restricted to admin/staff; a staff member may only create
   * slots for the praxis they are assigned to.
   */
  @PostMapping
  @Secured({"ROLE_ADMIN", "ROLE_STAFF"})
  public ResponseEntity<TerminDto> createSlot(
      @Valid @RequestBody TerminCreateRequestDto request, Principal principal) {
    User caller = userService.getUserByUsername(principal.getName());
    authorizePraxisAccess(caller, request.praxisId());
    Termin slot =
        terminService.createSlot(
            request.praxisId(), request.startTime(), request.endTime(), request.vaccine());
    return new ResponseEntity<>(TerminDto.from(slot), HttpStatus.CREATED);
  }

  /** Books a free slot for the current user. */
  @PostMapping("/{slotId}/book")
  public ResponseEntity<TerminDto> book(@PathVariable UUID slotId, Principal principal) {
    UUID userId = currentUserId(principal);
    Termin booked = terminService.book(slotId, userId);
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

  /**
   * Ensures the caller may act on {@code praxisId}: admins may act on any praxis, a staff member only
   * on the praxis they are assigned to.
   */
  private void authorizePraxisAccess(User caller, UUID praxisId) {
    if (isAdmin(caller)) {
      return;
    }
    UUID assigned = requireStaffPraxis(caller);
    if (!assigned.equals(praxisId)) {
      throw new AccessDeniedException("Staff may only manage slots for their own praxis");
    }
  }

  /**
   * Resolves the praxis a search is scoped to: admins keep the requested {@code praxisId} (possibly
   * {@code null} for all praxen), a staff member is forced to their own praxis and may not pass a
   * foreign {@code praxisId}.
   */
  private UUID scopePraxisForSearch(User caller, UUID requestedPraxisId) {
    if (isAdmin(caller)) {
      return requestedPraxisId;
    }
    UUID assigned = requireStaffPraxis(caller);
    if (requestedPraxisId != null && !requestedPraxisId.equals(assigned)) {
      throw new AccessDeniedException("Staff may only search their own praxis");
    }
    return assigned;
  }

  private UUID requireStaffPraxis(User caller) {
    return staffPraxisService
        .praxisIdFor(caller.getId())
        .orElseThrow(() -> new AccessDeniedException("Staff is not assigned to any praxis"));
  }

  private boolean isAdmin(User caller) {
    return caller.getRoles().stream()
        .anyMatch(role -> PermissionRoles.ADMIN.getName().equals(role.getName()));
  }
}
