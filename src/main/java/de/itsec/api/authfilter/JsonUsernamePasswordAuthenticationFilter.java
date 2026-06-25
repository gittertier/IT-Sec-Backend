package de.itsec.api.authfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import de.itsec.api.services.LoginAttemptService;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private static final long MIN_AUTH_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private LoginAttemptService loginAttemptService;

  public void setLoginAttemptService(LoginAttemptService loginAttemptService) {
    this.loginAttemptService = loginAttemptService;
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

    long start = System.nanoTime();
    if (request.getContentType() == null
        || !request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
      padToMinimum(start);
      return super.attemptAuthentication(request, response);
    }

    try {
      Map<String, String> body = objectMapper.readValue(request.getInputStream(), Map.class);
      String username = body.get(getUsernameParameter());
      String password = body.get(getPasswordParameter());

      if (loginAttemptService != null && loginAttemptService.isLocked(username)) {
        padToMinimum(start);
        // Same generic failure as a wrong password (the failure handler returns 401
        // "bad credentials"), so a lockout never reveals that the account exists.
        throw new LockedException("Too many failed login attempts");
      }

      UsernamePasswordAuthenticationToken authRequest =
          new UsernamePasswordAuthenticationToken(username, password);
      setDetails(request, authRequest);
      padToMinimum(start);
      try {
        Authentication auth = this.getAuthenticationManager().authenticate(authRequest);
        if (loginAttemptService != null) {
          loginAttemptService.recordSuccess(username);
        }
        return auth;
      } catch (AuthenticationException e) {
        if (loginAttemptService != null) {
          loginAttemptService.recordFailure(username);
        }
        throw e;
      }
    } catch (IOException e) {
      padToMinimum(start);
      throw new AuthenticationServiceException("Invalid JSON login payload", e);
    }
  }

  private void padToMinimum(long startNanos) {
    long elapsed = System.nanoTime() - startNanos;
    long remaining = MIN_AUTH_NANOS - elapsed;
    if (remaining > 0) {
      try {
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(remaining));
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
