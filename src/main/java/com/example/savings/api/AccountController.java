package com.example.savings.api;

import com.example.savings.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/savings-accounts")
@Tag(name = "Savings Accounts", description = "Manage customer savings accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping
  @Operation(
      summary = "Create a new savings account",
      description =
          "Opens a new savings account for a customer. Both the account ID and account number are system-generated.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Account successfully created"),
        @ApiResponse(
            responseCode = "400",
            description =
                "Validation failed (invalid customer name, offensive nickname, or malformed request)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "409",
            description = "Customer has reached the account limit",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(
            responseCode = "503",
            description = "Database temporarily unavailable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
    AccountResponse created = accountService.createAccount(request);
    return ResponseEntity.created(URI.create("/api/v1/savings-accounts/" + created.id()))
        .body(created);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Retrieve an account by ID",
      description =
          "Fetches account details using the system-generated account ID. Responses are cached for 10 minutes.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Account found and returned"),
        @ApiResponse(responseCode = "400", description = "Invalid account ID format"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(
            responseCode = "503",
            description = "Database temporarily unavailable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  public AccountResponse getById(@PathVariable UUID id) {
    return accountService.getAccount(id);
  }

  @GetMapping("/by-account-number/{accountNumber}")
  @Operation(
      summary = "Retrieve an account by account number",
      description =
          "Fetches account details using the customer-facing account number. Responses are cached for 10 minutes.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Account found and returned"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(
            responseCode = "503",
            description = "Database temporarily unavailable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      })
  public AccountResponse getByAccountNumber(@PathVariable String accountNumber) {
    return accountService.getAccountByNumber(accountNumber);
  }
}
