package com.lifetracker.identity;

import com.lifetracker.shared.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  record Credentials(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 100) String password) {}

  record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}

  record TokenRequest(@NotBlank @Size(max = 100) String token) {}

  record ResetRequest(
      @NotBlank @Size(max = 100) String token, @NotBlank @Size(max = 100) String password) {}

  private final IdentityService identity;
  private final UserDetailsService details;
  private final PasswordEncoder passwords;
  private final SecurityContextRepository contexts;
  private final RateLimiter limiter;

  public AuthController(
      IdentityService identity,
      UserDetailsService details,
      PasswordEncoder passwords,
      SecurityContextRepository contexts,
      RateLimiter limiter) {
    this.identity = identity;
    this.details = details;
    this.passwords = passwords;
    this.contexts = contexts;
    this.limiter = limiter;
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/me")
  Map<String, String> me(Authentication auth) {
    if (auth == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in");
    return Map.of("email", auth.getName());
  }

  private void limit(HttpServletRequest r) {
    limiter.check("auth:" + r.getRequestURI() + ":" + r.getRemoteAddr(), 15);
  }

  @PostMapping("/register")
  ResponseEntity<Void> register(@Valid @RequestBody Credentials c, HttpServletRequest r) {
    limit(r);
    identity.register(c.email, c.password);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/resend-verification")
  ResponseEntity<Void> resend(@Valid @RequestBody EmailRequest e, HttpServletRequest r) {
    limit(r);
    identity.request(e.email, false);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/forgot-password")
  ResponseEntity<Void> forgot(@Valid @RequestBody EmailRequest e, HttpServletRequest r) {
    limit(r);
    identity.request(e.email, true);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/verify-email")
  ResponseEntity<Void> verify(@Valid @RequestBody TokenRequest t, HttpServletRequest r) {
    limit(r);
    identity.consume(t.token, "VERIFY", null);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reset-password")
  ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest t, HttpServletRequest r) {
    limit(r);
    identity.consume(t.token, "RESET", t.password);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  Map<String, String> login(
      @Valid @RequestBody Credentials c, HttpServletRequest r, HttpServletResponse response) {
    limit(r);
    String email = IdentityService.email(c.email);
    try {
      var user = details.loadUserByUsername(email);
      if (!passwords.matches(c.password, user.getPassword()) || !user.isEnabled())
        throw new BadCredentialsException("Invalid credentials");
      r.getSession(true);
      r.changeSessionId();
      var auth =
          UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(auth);
      SecurityContextHolder.setContext(context);
      contexts.saveContext(context, r, response);
      new HttpSessionCsrfTokenRepository().saveToken(null, r, response);
      return Map.of("email", email);
    } catch (AuthenticationException ex) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials or email not verified");
    }
  }
}
