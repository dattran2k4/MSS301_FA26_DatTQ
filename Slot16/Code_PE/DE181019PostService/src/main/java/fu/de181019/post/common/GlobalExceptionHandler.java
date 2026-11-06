package fu.de181019.post.common;
import fu.de181019.post.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(ApiException.class)
 public ResponseEntity<ApiResponse<Void>> business(ApiException e) {
  int http=switch(e.getStatus()) { case 2 -> 406; case 3 -> 226; case 4 -> 404; case 5 -> 400; default -> 500; };
  return ResponseEntity.status(http).body(ApiResponse.error(e.getStatus(),e.getMessage()));
 }
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,ConstraintViolationException.class})
 public ResponseEntity<ApiResponse<Void>> invalid(Exception e) { return ResponseEntity.status(406).body(ApiResponse.error(2,"Invalid request format or value")); }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
  org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed",e);
  return ResponseEntity.internalServerError().body(ApiResponse.error(0,"Server error"));
 }
}
