package com.lifetracker.knowledge.validation;

import com.lifetracker.shared.ApiException;

public final class Isbn {
  private Isbn() {}

  public static String normalize(String raw) {
    if (raw == null || raw.isBlank()) return null;
    String s = raw.replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
    boolean valid = false;
    if (s.matches("[0-9]{9}[0-9X]")) {
      int sum = 0;
      for (int i = 0; i < 10; i++) sum += (10 - i) * (s.charAt(i) == 'X' ? 10 : s.charAt(i) - '0');
      valid = sum % 11 == 0;
    } else if (s.matches("97[89][0-9]{10}")) {
      int sum = 0;
      for (int i = 0; i < 13; i++) sum += (s.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
      valid = sum % 10 == 0;
    }
    if (!valid) throw ApiException.bad("ISBN must have a valid ISBN-10 or ISBN-13 checksum");
    return s;
  }

  public static String to13(String isbn10) {
    String prefix = "978" + isbn10.substring(0, 9);
    int sum = 0;
    for (int i = 0; i < 12; i++) sum += (prefix.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
    return prefix + ((10 - sum % 10) % 10);
  }
}
