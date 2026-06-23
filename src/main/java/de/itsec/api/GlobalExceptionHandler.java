package de.itsec.api;

import de.itsec.api.exceptions.AbstractPublicException;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errorMessage =
        ex.getBindingResult().getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(400, "Bad Request", errorMessage));
  }

  @ExceptionHandler(AbstractPublicException.class)
  public ResponseEntity<ErrorResponse> handleInvalidArgumentsException(AbstractPublicException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
  }

  private record ErrorResponse(int status, String error, String message) {}
}
