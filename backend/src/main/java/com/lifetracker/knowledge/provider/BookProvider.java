package com.lifetracker.knowledge.provider;

import com.lifetracker.knowledge.dto.Metadata;
import java.util.List;

public interface BookProvider {
  String name();

  List<Metadata> search(String title, String author, String isbn, int page);

  Metadata details(String editionId);

  Description description(String editionId);

  record Description(String text, String source, boolean workLevel) {}
}
