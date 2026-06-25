package de.itsec.api.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * Per-account login lockout. After too many failed attempts an account is locked for a cooldown
 * window. This is the authoritative brute-force control: the BFF rate limit is best effort and IP
 * based, and a successful first login step confirms valid credentials, so the account itself must
 * be protected. Keyed on the submitted username (lowercased), so non-existent accounts lock too and
 * a lockout cannot be used to tell which usernames exist.
 */
@Service
public class LoginAttemptService {

  private static final int MAX_ATTEMPTS = 5;

  // expireAfterWrite is refreshed on every recorded failure, so an attacker who keeps
  // trying stays locked; the entry clears after the window of no attempts.
  private final Cache<String, Integer> failures =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

  public boolean isLocked(String username) {
    Integer count = failures.getIfPresent(key(username));
    return count != null && count >= MAX_ATTEMPTS;
  }

  public void recordFailure(String username) {
    failures.asMap().merge(key(username), 1, Integer::sum);
  }

  public void recordSuccess(String username) {
    failures.invalidate(key(username));
  }

  private static String key(String username) {
    return username == null ? "" : username.toLowerCase();
  }
}
