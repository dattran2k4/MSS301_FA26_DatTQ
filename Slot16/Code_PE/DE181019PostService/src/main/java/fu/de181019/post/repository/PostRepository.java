package fu.de181019.post.repository;

import fu.de181019.post.entity.Post;
import org.springframework.data.jpa.repository.*;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {}
