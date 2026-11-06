package fu.de181019.post.client;
import fu.de181019.post.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="user-service",url="${user.service.url}")
public interface UserClient {
 @GetMapping("/api/users/{userId}") ApiResponse<UserDTO> get(@PathVariable("userId") long userId);
}
