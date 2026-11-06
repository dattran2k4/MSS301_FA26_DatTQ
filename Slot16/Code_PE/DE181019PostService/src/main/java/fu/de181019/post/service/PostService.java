package fu.de181019.post.service;

import fu.de181019.post.dto.*;

public interface PostService {
  PostDTO create(PostDTO dto);

  PostDTO update(long id, PostDTO dto);

  PostDetailDTO get(long id);

  void cancel(long id);

  PageDTO<PostDetailDTO> list(int page, int size, String advertiserName, String status);
}
