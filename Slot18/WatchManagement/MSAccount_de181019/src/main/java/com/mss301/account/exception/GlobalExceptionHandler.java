package com.mss301.account.exception;

import com.mss301.account.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// [TODO-5.4: trả lỗi dạng ApiResponse]
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(ApiResponse.error(ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("Internal error: " + ex.getMessage()));
    }
 @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
 public ResponseEntity<?> invalid(org.springframework.web.bind.MethodArgumentNotValidException ex) {
  return ResponseEntity.badRequest().body(ApiResponse.error(ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage()).collect(java.util.stream.Collectors.joining("; "))));
 }
 @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
 public ResponseEntity<?> malformed(Exception ex) { return ResponseEntity.badRequest().body(ApiResponse.error("Malformed request or parameter")); }
}
