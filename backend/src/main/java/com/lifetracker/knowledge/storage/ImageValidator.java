package com.lifetracker.knowledge.storage;

import com.lifetracker.shared.ApiException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.springframework.stereotype.Component;

@Component
public class ImageValidator {
  public record SafeImage(byte[] bytes, int width, int height, String color) {}

  public SafeImage validate(byte[] raw, String declaredType) {
    if (raw.length == 0 || raw.length > 5 * 1024 * 1024)
      throw ApiException.bad("Image must be between 1 byte and 5 MiB");
    if (!Set.of("image/png", "image/jpeg").contains(Objects.toString(declaredType, "")))
      throw ApiException.bad("Choose a PNG or JPEG image");
    ImageIO.setUseCache(false);
    try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw ApiException.bad("Invalid image");
      var reader = readers.next();
      try {
        reader.setInput(stream, true, true);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!Set.of("png", "jpeg", "jpg").contains(format))
          throw ApiException.bad("Unsupported image content");
        if (!(format.equals("png") ? "image/png" : "image/jpeg").equals(declaredType))
          throw ApiException.bad("Image content does not match its type");
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w < 1 || h < 1 || (long) w * h > 16_000_000)
          throw ApiException.bad("Image must be at most 16 megapixels");
        BufferedImage image = reader.read(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        if (out.size() > 20 * 1024 * 1024) throw ApiException.bad("Decoded image is too large");
        long r = 0, g = 0, b = 0, count = 0;
        for (int y = 0; y < h; y += Math.max(1, h / 40))
          for (int x = 0; x < w; x += Math.max(1, w / 40)) {
            int rgb = image.getRGB(x, y);
            if ((rgb >>> 24) < 128) continue;
            r += (rgb >> 16) & 255;
            g += (rgb >> 8) & 255;
            b += rgb & 255;
            count++;
          }
        String color =
            count == 0
                ? "#806047"
                : String.format("#%02x%02x%02x", r / count, g / count, b / count);
        return new SafeImage(out.toByteArray(), w, h, color);
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw ApiException.bad("Image could not be decoded");
    }
  }
}
