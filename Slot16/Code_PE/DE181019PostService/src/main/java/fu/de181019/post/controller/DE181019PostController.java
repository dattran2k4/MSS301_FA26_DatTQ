package fu.de181019.post.controller;
import fu.de181019.post.dto.*;
import fu.de181019.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/posts")
public class DE181019PostController {
 private final PostService service;
 public DE181019PostController(PostService service) { this.service=service; }
 @PostMapping public ResponseEntity<ApiResponse<PostDTO>> create(@RequestBody PostDTO d) { return ResponseEntity.status(201).body(ApiResponse.ok(service.create(d))); }
 @PutMapping("/{postId}") public ApiResponse<PostDTO> update(@PathVariable long postId,@RequestBody PostDTO d) { return ApiResponse.ok(service.update(postId,d)); }
 @GetMapping("/{postId}") public ApiResponse<PostDetailDTO> get(@PathVariable long postId) { return ApiResponse.ok(service.get(postId)); }
 @DeleteMapping("/{postId}") public ApiResponse<Void> delete(@PathVariable long postId) { service.cancel(postId); return ApiResponse.ok(null); }
 @GetMapping public ApiResponse<PageDTO<PostDetailDTO>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) String advertiserName,@RequestParam(required=false) String status) { return ApiResponse.ok(service.list(page,size,advertiserName,status)); }
}
