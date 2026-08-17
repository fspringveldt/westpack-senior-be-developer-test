package com.example.savings.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.savings.config.AccountPolicyProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OffensiveNicknamePolicyTest {

  private final OffensiveNicknamePolicy policy =
      new OffensiveNicknamePolicy(
          new AccountPolicyProperties(5, List.of("BadWord", " wanker ", "")));

  @ParameterizedTest
  @ValueSource(
      strings = {
        "badword",
        "BADWORD",
        "my badword fund",
        "b-a-d-w-o-r-d",
        "B_a_d W_o_r_d",
        "wanker"
      })
  void detectsOffensiveNicknames(String nickname) {
    assertThat(policy.isOffensive(nickname)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Rainy day", "House deposit", "Bad debt fund", "words"})
  void acceptsCleanNicknames(String nickname) {
    assertThat(policy.isOffensive(nickname)).isFalse();
  }

  @Test
  void treatsAMissingNicknameAsClean() {
    assertThat(policy.isOffensive(null)).isFalse();
  }
}
