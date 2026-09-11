package com.skyrush.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrorHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);

  public record ApiError(String code, String message, int status, String path, Instant timestamp) {}

  @ExceptionHandler(GameException.class)
  public ResponseEntity<ApiError> game(GameException ex, HttpServletRequest request) {
    return error(ex.status(), ex.code(), ex.getMessage(), request);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> invalid(Exception ex, HttpServletRequest request) {
    return error(
        400,
        "INVALID_REQUEST",
        "Invalid request. Use the documented fields and values; calculated fields and timestamps are not accepted.",
        request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiError> missing(Exception ex, HttpServletRequest request) {
    return error(404, "NOT_FOUND", "Endpoint not found", request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> method(Exception ex, HttpServletRequest request) {
    return error(405, "METHOD_NOT_ALLOWED", "HTTP method not allowed", request);
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> mediaType(Exception ex, HttpServletRequest request) {
    return error(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json for request bodies", request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
    LOG.error("Unexpected API failure", ex);
    return error(500, "INTERNAL_ERROR", "Unexpected server error", request);
  }

  private ResponseEntity<ApiError> error(
      int status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(new ApiError(code, message, status, request.getRequestURI(), Instant.now()));
  }
}
