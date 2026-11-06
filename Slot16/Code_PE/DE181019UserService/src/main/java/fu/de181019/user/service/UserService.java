package fu.de181019.user.service;

import fu.de181019.user.dto.*;

public interface UserService {
  UserDTO create(UserDTO dto);

  UserDTO update(long id, UserDTO dto);

  UserDTO get(long id);

  void deactivate(long id);

  PageDTO<UserDTO> list(int page, int size, String role, String status);
}
