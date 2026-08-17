package com.example.savings.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.savings.service.AccountLimitExceededException;
import com.example.savings.service.AccountNotFoundException;
import com.example.savings.service.AccountService;
import com.example.savings.service.OffensiveNicknameException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

  private static final String BASE_PATH = "/api/v1/savings-accounts";

  @Mock private AccountService accountService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AccountController(accountService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void createsAnAccount() throws Exception {
    AccountResponse created =
        new AccountResponse(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            "Ada Lovelace",
            "Rainy day",
            Instant.now());
    when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(created);

    MockHttpServletResponse response =
        post(
            """
                {"customerName":"Ada Lovelace","accountNickName":"Rainy day"}""");

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getHeader("Location")).isEqualTo(BASE_PATH + "/" + created.id());
    assertThat(response.getContentAsString())
        .contains(created.accountNumber(), "Ada Lovelace", "Rainy day");
  }

  @Test
  void rejectsAMissingCustomerName() throws Exception {
    MockHttpServletResponse response =
        post(
            """
                {"accountNickName":"Rainy day"}""");

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("customerName", "mandatory");
  }

  @Test
  void rejectsABlankCustomerName() throws Exception {
    MockHttpServletResponse response =
        post(
            """
                {"customerName":"   ","accountNickName":"Rainy day"}""");

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("customerName");
  }

  @Test
  void rejectsANickNameShorterThanFiveCharacters() throws Exception {
    MockHttpServletResponse response =
        post(
            """
                {"customerName":"Ada Lovelace","accountNickName":"tiny"}""");

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("accountNickName", "between 5 and 30");
  }

  @Test
  void rejectsANickNameLongerThanThirtyCharacters() throws Exception {
    MockHttpServletResponse response =
        post(
            """
                {"customerName":"Ada Lovelace","accountNickName":"%s"}"""
                .formatted("n".repeat(31)));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("accountNickName", "between 5 and 30");
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    MockHttpServletResponse response = post("{ not json ");

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("Malformed request");
  }

  @Test
  void returnsConflictWhenTheCustomerReachedTheAccountLimit() throws Exception {
    when(accountService.createAccount(any(CreateAccountRequest.class)))
        .thenThrow(new AccountLimitExceededException("Ada Lovelace", 5));

    MockHttpServletResponse response =
        post(
            """
                {"customerName":"Ada Lovelace"}""");

    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(response.getContentAsString()).contains("Account limit reached");
  }

  @Test
  void returnsBadRequestForAnOffensiveNickName() throws Exception {
    when(accountService.createAccount(any(CreateAccountRequest.class)))
        .thenThrow(new OffensiveNicknameException());

    MockHttpServletResponse response =
        post(
            """
                {"customerName":"Ada Lovelace","accountNickName":"badword fund"}""");

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("Offensive account nick name");
  }

  @Test
  void returnsServiceUnavailableWhenTheDatabaseIsDown() throws Exception {
    when(accountService.createAccount(any(CreateAccountRequest.class)))
        .thenThrow(new DataAccessResourceFailureException("connection refused"));

    MockHttpServletResponse response =
        post(
            """
                {"customerName":"Ada Lovelace"}""");

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getHeader("Retry-After")).isEqualTo("30");
    assertThat(response.getContentAsString()).contains("Service temporarily unavailable");
  }

  @Test
  void getsAnAccountById() throws Exception {
    UUID id = UUID.randomUUID();
    AccountResponse account =
        new AccountResponse(id, UUID.randomUUID().toString(), "Ada Lovelace", null, Instant.now());
    when(accountService.getAccount(id)).thenReturn(account);

    MockHttpServletResponse response =
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + id)).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains(account.accountNumber(), "Ada Lovelace");
  }

  @Test
  void getsAnAccountByAccountNumber() throws Exception {
    String accountNumber = UUID.randomUUID().toString();
    AccountResponse account =
        new AccountResponse(
            UUID.randomUUID(), accountNumber, "Ada Lovelace", "Rainy day", Instant.now());
    when(accountService.getAccountByNumber(eq(accountNumber))).thenReturn(account);

    MockHttpServletResponse response =
        mockMvc
            .perform(MockMvcRequestBuilders.get(BASE_PATH + "/by-account-number/" + accountNumber))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains(accountNumber);
  }

  @Test
  void returnsNotFoundForAnUnknownAccount() throws Exception {
    UUID id = UUID.randomUUID();
    when(accountService.getAccount(id)).thenThrow(new AccountNotFoundException(id.toString()));

    MockHttpServletResponse response =
        mockMvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + id)).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getContentAsString()).contains("Account not found");
  }

  @Test
  void returnsBadRequestForAMalformedAccountId() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(MockMvcRequestBuilders.get(BASE_PATH + "/not-a-uuid"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(400);
  }

  private MockHttpServletResponse post(String body) throws Exception {
    return mockMvc
        .perform(
            MockMvcRequestBuilders.post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse();
  }
}
