package com.iitsm.service;

import com.iitsm.entity.*;
import com.iitsm.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetectionService {
  private final IncidentRepository incidents;
  private final DuplicateSuggestionRepository duplicates;

  public DuplicateDetectionService(IncidentRepository i, DuplicateSuggestionRepository d) {
    incidents = i;
    duplicates = d;
  }

  public static double similarity(String a, String b) {
    Set<String> left = tokens(a), right = tokens(b);
    if (left.isEmpty() || right.isEmpty()) return 0;
    Set<String> intersection = new HashSet<>(left);
    intersection.retainAll(right);
    Set<String> union = new HashSet<>(left);
    union.addAll(right);
    return (double) intersection.size() / union.size();
  }

  private static Set<String> tokens(String text) {
    Set<String> words = new HashSet<>();
    for (String w : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
      if (w.length() > 2 && !Set.of("the", "and", "for", "with", "this", "that", "not").contains(w))
        words.add(w);
    return words;
  }

  public void detect(Incident i) {
    incidents.findByCreatedAtAfter(Instant.now().minus(Duration.ofDays(30))).stream()
        .filter(x -> !x.getId().equals(i.getId()))
        .filter(x -> x.getStatus() != com.iitsm.model.Status.CLOSED)
        .forEach(
            x -> {
              double score =
                  .6
                          * similarity(
                              i.getTitle() + " " + i.getDescription(),
                              x.getTitle() + " " + x.getDescription())
                      + .25 * (i.getService().getId().equals(x.getService().getId()) ? 1 : 0)
                      + .15 * (i.getCategory().getId().equals(x.getCategory().getId()) ? 1 : 0);
              if (score >= .62)
                duplicates.save(new DuplicateSuggestion(i, x, Math.round(score * 1000) / 1000.0));
            });
  }
}
