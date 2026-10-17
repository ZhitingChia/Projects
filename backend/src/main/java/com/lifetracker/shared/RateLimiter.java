package com.lifetracker.shared;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Bounded single-instance limiter. Do not trust forwarded IP headers from arbitrary clients. */
@Component
public class RateLimiter {
  private record Window(long minute, int count) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public synchronized void check(String key, int limit) {
    long minute = Instant.now().getEpochSecond() / 60;
    if (windows.size() > 10000) windows.entrySet().removeIf(e -> e.getValue().minute < minute);
    if (windows.size() > 20000)
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Please try again shortly");
    Window old = windows.get(key);
    int count = old == null || old.minute != minute ? 1 : old.count + 1;
    windows.put(key, new Window(minute, count));
    if (count > limit)
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again in a minute.");
  }
}
