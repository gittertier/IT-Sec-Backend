package de.itsec.api.authfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private static final long MIN_AUTH_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
  private final ObjectMapper objectMapper = new ObjectMapper();

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

      UsernamePasswordAuthenticationToken authRequest =
          new UsernamePasswordAuthenticationToken(username, password);

      setDetails(request, authRequest);
      padToMinimum(start);
      return this.getAuthenticationManager().authenticate(authRequest);
    } catch (IOException e) {
      padToMinimum(start);
      throw new AuthenticationServiceException("Invalid JSON login payload", e);
    }
  }

  private void padToMinimum(long startNanos) {
    long elapsed = System.nanoTime() - startNanos;
    long remaining = MIN_AUTH_NANOS - elapsed;
    System.out.println(remaining);
    if (remaining > 0) {
      try {
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(remaining));
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
