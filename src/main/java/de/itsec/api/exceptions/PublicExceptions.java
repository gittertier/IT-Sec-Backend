package de.itsec.api.exceptions;

import lombok.experimental.StandardException;

public class PublicExceptions {

  private PublicExceptions() {}

  @StandardException
  public static class IllegalArgumentsException extends AbstractPublicException {}

  public static class UsernameAlreadyExistsException extends AbstractPublicException {
    public UsernameAlreadyExistsException() {
      super("Username already exists");
    }
  }

  public static class TerminNotFoundException extends AbstractPublicException {
    public TerminNotFoundException() {
      super("Appointment slot not found");
    }
  }

  public static class SlotNotBookableException extends AbstractPublicException {
    public SlotNotBookableException() {
      super("Appointment slot is not available for booking");
    }
  }

  public static class BookingLimitReachedException extends AbstractPublicException {
    public BookingLimitReachedException(int limit) {
      super("Booking limit reached: a user may hold at most " + limit + " upcoming appointments");
    }
  }

  public static class NotYourTerminException extends AbstractPublicException {
    public NotYourTerminException() {
      super("Appointment does not belong to the current user");
    }
  }

  public static class PraxisNotFoundException extends AbstractPublicException {
    public PraxisNotFoundException() {
      super("Praxis not found");
    }
  }

  public static class InvalidSlotTimeException extends AbstractPublicException {
    public InvalidSlotTimeException() {
      super("Invalid slot time: startTime must be in the future and before endTime");
    }
  }

  public static class InvalidVerificationTokenException extends AbstractPublicException {
    public InvalidVerificationTokenException() {
      super("Verification Token is invalid");
    }
  }

  public static class VerificationTokenExpiredException extends AbstractPublicException {
    public VerificationTokenExpiredException() {
      super("Verification token expired");
    }
  }
}
