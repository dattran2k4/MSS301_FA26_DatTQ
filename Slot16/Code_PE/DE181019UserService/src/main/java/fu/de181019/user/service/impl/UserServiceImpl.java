package fu.de181019.user.service.impl;

import static fu.de181019.user.common.Checks.*;

import fu.de181019.user.common.*;
import fu.de181019.user.dto.*;
import fu.de181019.user.entity.*;
import fu.de181019.user.repository.UserRepository;
import fu.de181019.user.service.UserService;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
  private final UserRepository repository;
  private final Validator validator;

  public UserServiceImpl(UserRepository repository, Validator validator) {
    this.repository = repository;
    this.validator = validator;
  }

  private record EmailValue(@Email String value) {}

  private User find(long id) {
    return repository.findById(id).orElseThrow(() -> new ApiException(4, "User not found"));
  }

  private void apply(UserDTO d, User u) {
    if (d.getUsername() != null) u.setUsername(d.getUsername());
    if (d.getEmail() != null) u.setEmail(d.getEmail());
    if (d.getRole() != null) u.setRole(enumeration(Role.class, d.getRole()));
    if (d.getRatePerPost() != null) u.setRatePerPost(d.getRatePerPost());
    if (d.getMaxDailyPosts() != null) u.setMaxDailyPosts(d.getMaxDailyPosts());
    if (d.getLevel() != null) u.setLevel(d.getLevel());
    if (d.getStatus() != null) u.setStatus(enumeration(UserStatus.class, d.getStatus()));
  }

  private void validate(User u) {
    text(u.getUsername(), 30, "username");
    text(u.getEmail(), 100, "email");
    require(validator.validate(new EmailValue(u.getEmail())).isEmpty(), "Invalid email");
    require(u.getRole() != null, "role is required");
    money(u.getRatePerPost());
    require(
        u.getMaxDailyPosts() != null && u.getMaxDailyPosts() >= 1 && u.getMaxDailyPosts() <= 10,
        "maxDailyPosts must be 1–10");
    require(u.getLevel() != null && u.getLevel() >= 1, "level must be >= 1");
  }

  private UserDTO dto(User u) {
    UserDTO d = new UserDTO();
    BeanUtils.copyProperties(u, d);
    d.setRole(u.getRole().name());
    d.setStatus(u.getStatus().name());
    return d;
  }

  private UserDTO save(User u) {
    boolean duplicate =
        u.getUserId() == null
            ? repository.existsByUsername(u.getUsername())
            : repository.existsByUsernameAndUserIdNot(u.getUsername(), u.getUserId());
    if (duplicate) throw new ApiException(3, "Duplicate username");
    try {
      return dto(repository.saveAndFlush(u));
    } catch (DataIntegrityViolationException e) {
      Throwable cause = e;
      while (cause.getCause() != null) cause = cause.getCause();
      if (cause instanceof java.sql.SQLException sql
          && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627))
        throw new ApiException(3, "Duplicate username");
      throw e;
    }
  }

  @Override
  public UserDTO create(UserDTO d) {
    User u = new User();
    u.setStatus(UserStatus.ACTIVE);
    apply(d, u);
    validate(u);
    return save(u);
  }

  @Override
  public UserDTO update(long id, UserDTO d) {
    User u = find(id);
    apply(d, u);
    validate(u);
    return save(u);
  }

  @Override
  @Transactional(readOnly = true)
  public UserDTO get(long id) {
    return dto(find(id));
  }

  @Override
  public void deactivate(long id) {
    User u = find(id);
    u.setStatus(UserStatus.DEACTIVATED);
    repository.save(u);
  }

  @Override
  @Transactional(readOnly = true)
  public PageDTO<UserDTO> list(int page, int size, String role, String status) {
    var pageable = Checks.page(page, size, "userId");
    Role r = role == null ? null : enumeration(Role.class, role);
    UserStatus s = status == null ? null : enumeration(UserStatus.class, status);
    Specification<User> spec =
        (root, query, cb) -> {
          var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
          if (r != null) predicates.add(cb.equal(root.get("role"), r));
          if (s != null) predicates.add(cb.equal(root.get("status"), s));
          return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return PageDTO.from(repository.findAll(spec, pageable).map(this::dto));
  }
}
