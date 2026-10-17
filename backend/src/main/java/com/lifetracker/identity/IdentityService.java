package com.lifetracker.identity;

import com.lifetracker.shared.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
  private final UserRepository users;
  private final TokenRepository tokens;
  private final PasswordEncoder passwords;
  private final JavaMailSender mail;
  private final FindByIndexNameSessionRepository<? extends Session> sessions;
  private final String baseUrl, from;

  public IdentityService(
      UserRepository users,
      TokenRepository tokens,
      PasswordEncoder passwords,
      JavaMailSender mail,
      FindByIndexNameSessionRepository<? extends Session> sessions,
      @Value("${app.base-url}") String baseUrl,
      @Value("${app.mail-from}") String from) {
    this.users = users;
    this.tokens = tokens;
    this.passwords = passwords;
    this.mail = mail;
    this.sessions = sessions;
    this.baseUrl = baseUrl;
    this.from = from;
  }

  public static String email(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public static void validatePassword(String password) {
    int bytes = password.getBytes(StandardCharsets.UTF_8).length;
    if (bytes < 12 || bytes > 72) throw ApiException.bad("Password must contain 12–72 UTF-8 bytes");
  }

  @Transactional
  public void register(String email, String password) {
    validatePassword(password);
    String normalized = email(email);
    if (users.findByEmail(normalized).isPresent()) return;
    User u = new User();
    u.email = normalized;
    u.passwordHash = passwords.encode(password);
    users.saveAndFlush(u);
    issue(u, "VERIFY", Duration.ofHours(24), "verify-email");
  }

  @Transactional
  public void request(String email, boolean reset) {
    users
        .findByEmail(email(email))
        .ifPresent(
            u -> {
              if (reset ? u.verifiedAt != null : u.verifiedAt == null)
                issue(
                    u,
                    reset ? "RESET" : "VERIFY",
                    reset ? Duration.ofMinutes(30) : Duration.ofHours(24),
                    reset ? "reset-password" : "verify-email");
            });
  }

  private void issue(User u, String purpose, Duration duration, String page) {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    AccountToken t = new AccountToken();
    t.userId = u.id;
    t.purpose = purpose;
    t.tokenHash = hash(raw);
    t.expiresAt = Instant.now().plus(duration);
    tokens.save(t);
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(u.email);
    message.setSubject(
        purpose.equals("VERIFY")
            ? "Verify your Life Tracker email"
            : "Reset your Life Tracker password");
    message.setText(
        "Open this link to continue:\n"
            + baseUrl
            + "/"
            + page
            + "#token="
            + raw
            + "\n\nIf you did not request this, ignore this message.");
    mail.send(message); // failure rolls back; resend safely recovers existing pending accounts
  }

  @Transactional
  public void consume(String raw, String purpose, String password) {
    AccountToken t =
        tokens
            .findByTokenHash(hash(raw))
            .orElseThrow(() -> ApiException.bad("This link is invalid or expired"));
    if (!t.purpose.equals(purpose) || t.consumedAt != null || !t.expiresAt.isAfter(Instant.now()))
      throw ApiException.bad("This link is invalid or expired");
    User u = users.findById(t.userId).orElseThrow(ApiException::missing);
    if (purpose.equals("VERIFY")) u.verifiedAt = Instant.now();
    else {
      validatePassword(password);
      u.passwordHash = passwords.encode(password);
      sessions.findByPrincipalName(u.email).keySet().forEach(sessions::deleteById);
    }
    t.consumedAt = Instant.now();
    tokens.flush();
    tokens.invalidateAll(u.id, purpose);
  }

  static String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
