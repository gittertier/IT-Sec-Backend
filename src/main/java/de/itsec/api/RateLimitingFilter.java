package de.itsec.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class RateLimitingFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_MINUTE = 5;

  private final Cache<String, AtomicInteger> requestCounts =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !("POST".equalsIgnoreCase(request.getMethod())
        && "/api/v1/public/login".equals(request.getServletPath()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    ContentCachingRequestWrapper wrappedRequest =
        request instanceof ContentCachingRequestWrapper wrapped
            ? wrapped
            : new ContentCachingRequestWrapper(request, 0);

    String clientIp = getClientIp(wrappedRequest);
    String username = extractUsername(wrappedRequest);

    AtomicInteger counter =
        requestCounts.get(clientIp + ":" + username, key -> new AtomicInteger(0));
    int currentCount = counter.incrementAndGet();

    if (currentCount > MAX_REQUESTS_PER_MINUTE) {
      response.setStatus(429);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      response
          .getWriter()
          .write(
              String.format(
                  """
                  {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Rate limit exceeded. Max %d requests per minute.",
                    "retryAfter": 60
                  }
                  """,
                  MAX_REQUESTS_PER_MINUTE));
      return;
    }

    chain.doFilter(request, response);
  }

  private String extractUsername(ContentCachingRequestWrapper request) {
    try {
      byte[] body = request.getContentAsByteArray();

      String json = new String(body, StandardCharsets.UTF_8);

      JsonNode node = objectMapper.readTree(json);

      return node.has("username") ? node.get("username").asText() : null;

    } catch (Exception _) {
      return null;
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
  }
}
