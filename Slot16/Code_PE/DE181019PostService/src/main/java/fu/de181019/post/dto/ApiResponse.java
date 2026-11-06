package fu.de181019.post.dto;

import java.time.Instant;

public record ApiResponse<T>(int status, String message, T data, String timestamp) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(1, "Successful", data, Instant.now().toString());
  }

  public static ApiResponse<Void> error(int status, String message) {
    return new ApiResponse<>(status, message, null, Instant.now().toString());
  }
}
