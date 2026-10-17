package com.lifetracker.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.lifetracker.identity.IdentityService;
import com.lifetracker.knowledge.storage.ImageValidator;
import com.lifetracker.knowledge.validation.Isbn;
import com.lifetracker.shared.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ValidationTest {
  @Test
  void isbnChecksumsAndNormalization() {
    assertEquals("0306406152", Isbn.normalize("0-306-40615-2"));
    assertEquals("9780306406157", Isbn.normalize("978 0 306 40615 7"));
    assertEquals("9780306406157", Isbn.to13("0306406152"));
    assertThrows(ApiException.class, () -> Isbn.normalize("9780306406158"));
    assertThrows(ApiException.class, () -> Isbn.normalize("123"));
    assertNull(Isbn.normalize(""));
  }

  @Test
  void passwordsUseUtf8ByteBounds() {
    assertThrows(ApiException.class, () -> IdentityService.validatePassword("too short"));
    assertThrows(ApiException.class, () -> IdentityService.validatePassword("界".repeat(25)));
    assertDoesNotThrow(() -> IdentityService.validatePassword("a long personal passphrase"));
  }

  @Test
  void imagesAreDecodedAndReencoded() throws Exception {
    BufferedImage image = new BufferedImage(12, 18, BufferedImage.TYPE_INT_RGB);
    var out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    var safe = new ImageValidator().validate(out.toByteArray(), "image/png");
    assertEquals(12, safe.width());
    assertEquals(18, safe.height());
    assertEquals("#000000", safe.color());
    assertThrows(
        ApiException.class, () -> new ImageValidator().validate(out.toByteArray(), "image/jpeg"));
    assertThrows(
        ApiException.class,
        () -> new ImageValidator().validate("<svg/>".getBytes(), "image/svg+xml"));
  }
}
