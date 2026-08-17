package com.example.savings.api;

import com.example.savings.service.AccountLimitExceededException;
import com.example.savings.service.AccountNotFoundException;
import com.example.savings.service.OffensiveNicknameException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centralized exception handling for the savings API. Translates domain exceptions, validation
 * failures, and infrastructure issues into standardized RFC 7807 (Problem Details for HTTP APIs)
 * responses. This ensures consistent error reporting across all endpoints and facilitates client
 * error handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Validation failed");
    problem.setDetail("The request body is invalid");
    List<String> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.naturalOrder())
            .toList();
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ProblemDetail handleMalformedRequest(Exception exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Malformed request");
    problem.setDetail("The request could not be read");
    return problem;
  }

  @ExceptionHandler(OffensiveNicknameException.class)
  public ProblemDetail handleOffensiveNickname(OffensiveNicknameException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Offensive account nick name");
    problem.setDetail(exception.getMessage());
    return problem;
  }

  @ExceptionHandler(AccountLimitExceededException.class)
  public ProblemDetail handleAccountLimit(AccountLimitExceededException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("Account limit reached");
    problem.setDetail(exception.getMessage());
    problem.setProperty("limit", exception.getLimit());
    return problem;
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ProblemDetail handleNotFound(AccountNotFoundException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("Account not found");
    problem.setDetail(exception.getMessage());
    return problem;
  }

  /**
   * Handles transient database failures (connection timeouts, query timeouts, lost connections).
   * Returns 503 Service Unavailable with Retry-After header to signal clients they should retry.
   * This graceful degradation prevents cascading failures in client applications.
   */
  @ExceptionHandler({
    DataAccessResourceFailureException.class,
    CannotCreateTransactionException.class,
    QueryTimeoutException.class,
    TransientDataAccessException.class
  })
  public ResponseEntity<ProblemDetail> handleDatabaseUnavailable(Exception exception) {
    log.error("Database unavailable while serving request", exception);
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
    problem.setTitle("Service temporarily unavailable");
    problem.setDetail("The account store is currently unavailable, please retry shortly");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header(HttpHeaders.RETRY_AFTER, "30")
        .body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception exception) {
    log.error("Unexpected error while serving request", exception);
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problem.setTitle("Internal server error");
    problem.setDetail("An unexpected error occurred");
    return problem;
  }
}
