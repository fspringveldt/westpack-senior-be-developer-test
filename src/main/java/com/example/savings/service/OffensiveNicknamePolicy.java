package com.example.savings.service;

import com.example.savings.config.AccountPolicyProperties;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Policy component that validates account nicknames against a configured list of offensive terms.
 * Nicknames are normalized before matching (lowercased and non-alphanumeric characters removed),
 * preventing users from circumventing filters with special characters or mixed casing.
 */
@Component
public class OffensiveNicknamePolicy {

  private final Set<String> offensiveTerms;

  public OffensiveNicknamePolicy(AccountPolicyProperties properties) {
    this.offensiveTerms =
        properties.offensiveNicknames().stream()
            .map(OffensiveNicknamePolicy::normalise)
            .filter(term -> !term.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Determines if the given nickname contains offensive language. Normalization ensures fuzzy
   * matching catches attempts to evade filters (e.g., "B_a_d Word" matches "badword"). Null
   * nicknames are treated as acceptable since the nickname field is optional.
   */
  public boolean isOffensive(String nickname) {
    if (nickname == null) {
      return false;
    }
    String candidate = normalise(nickname);
    return offensiveTerms.stream().anyMatch(candidate::contains);
  }

  private static String normalise(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }
}
