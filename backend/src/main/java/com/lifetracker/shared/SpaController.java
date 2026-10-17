package com.lifetracker.shared;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class SpaController {
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public SpaController(org.springframework.jdbc.core.JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping({
    "/",
    "/login",
    "/register",
    "/forgot-password",
    "/reset-password",
    "/verify-email",
    "/library"
  })
  String index() {
    return "forward:/index.html";
  }

  @GetMapping("/api/v1/health")
  @ResponseBody
  org.springframework.http.ResponseEntity<java.util.Map<String, String>> health() {
    try {
      jdbc.queryForObject("select 1", Integer.class);
      return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "UP"));
    } catch (org.springframework.dao.DataAccessException e) {
      return org.springframework.http.ResponseEntity.status(503)
          .body(java.util.Map.of("status", "DOWN"));
    }
  }
}
