package fu.de181019.user.controller;
import fu.de181019.user.dto.*;
import fu.de181019.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users")
public class DE181019UserController {
 private final UserService service;
 public DE181019UserController(UserService service) { this.service=service; }
 @PostMapping public ResponseEntity<ApiResponse<UserDTO>> create(@RequestBody UserDTO d) { return ResponseEntity.status(201).body(ApiResponse.ok(service.create(d))); }
 @PutMapping("/{userId}") public ApiResponse<UserDTO> update(@PathVariable long userId,@RequestBody UserDTO d) { return ApiResponse.ok(service.update(userId,d)); }
 @GetMapping("/{userId}") public ApiResponse<UserDTO> get(@PathVariable long userId) { return ApiResponse.ok(service.get(userId)); }
 @DeleteMapping("/{userId}") public ApiResponse<Void> delete(@PathVariable long userId) { service.deactivate(userId); return ApiResponse.ok(null); }
 @GetMapping public ApiResponse<PageDTO<UserDTO>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) String role,@RequestParam(required=false) String status) { return ApiResponse.ok(service.list(page,size,role,status)); }
}
