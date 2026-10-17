package com.lifetracker.identity;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email ->
        users
            .findByEmail(email)
            .map(
                u ->
                    org.springframework.security.core.userdetails.User.withUsername(u.email)
                        .password(u.passwordHash)
                        .disabled(u.verifiedAt == null)
                        .roles("USER")
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, SecurityContextRepository contexts)
      throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/v1/auth/**", "/api/v1/health")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .csrf(
            c ->
                c.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .securityContext(c -> c.securityContextRepository(contexts))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/problem+json");
                          res.getWriter().write("{\"status\":401,\"detail\":\"Please sign in\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/problem+json");
                          res.getWriter()
                              .write(
                                  "{\"status\":403,\"detail\":\"Your security token expired. Please"
                                      + " retry.\"}");
                        }))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; img-src 'self' data: blob:"
                                + " https://books.google.com https://books.googleusercontent.com"
                                + " https://covers.openlibrary.org; style-src 'self'"
                                + " 'unsafe-inline'; script-src 'self'; connect-src 'self';"
                                + " frame-ancestors 'none'; base-uri 'self'")))
        .logout(
            l ->
                l.logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
    return http.build();
  }
}
