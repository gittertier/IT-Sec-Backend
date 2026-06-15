package de.itsec.api.data.termin;

/** Lifecycle state of a {@link Termin} slot. */
public enum TerminStatus {
  /** Slot is open and can be booked by a user. */
  FREE,
  /** Slot is currently held by a (pseudonymous) user. */
  BOOKED,
  /** Slot was cancelled and is no longer bookable. */
  CANCELLED
}
