package fu.de181019.user.repository;
import fu.de181019.user.entity.User;
import org.springframework.data.jpa.repository.*;
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
 boolean existsByUsername(String username);
 boolean existsByUsernameAndUserIdNot(String username,Long userId);
}
