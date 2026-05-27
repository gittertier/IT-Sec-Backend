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
}
