package fu.de181019.user.entity;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
@Entity
@Table(name="users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="user_id", nullable=false)
 private Long userId;
 @Column(name="username", nullable=false, length=30, unique=true)
 private String username;
 @Column(name="email", nullable=false, length=100)
 private String email;
 @Enumerated(EnumType.STRING)
 @Column(name="role", nullable=false, length=15)
 private Role role;
 @Column(name="rate_per_post", nullable=false, precision=18, scale=2)
 private BigDecimal ratePerPost;
 @Column(name="max_daily_posts", nullable=false)
 private Integer maxDailyPosts;
 @Column(name="level", nullable=false)
 private Integer level;
 @Enumerated(EnumType.STRING)
 @Column(name="status", nullable=false, length=15)
 private UserStatus status;
 public Long getUserId() { return userId; }
 public void setUserId(Long value) { this.userId=value; }
 public String getUsername() { return username; }
 public void setUsername(String value) { this.username=value; }
 public String getEmail() { return email; }
 public void setEmail(String value) { this.email=value; }
 public Role getRole() { return role; }
 public void setRole(Role value) { this.role=value; }
 public BigDecimal getRatePerPost() { return ratePerPost; }
 public void setRatePerPost(BigDecimal value) { this.ratePerPost=value; }
 public Integer getMaxDailyPosts() { return maxDailyPosts; }
 public void setMaxDailyPosts(Integer value) { this.maxDailyPosts=value; }
 public Integer getLevel() { return level; }
 public void setLevel(Integer value) { this.level=value; }
 public UserStatus getStatus() { return status; }
 public void setStatus(UserStatus value) { this.status=value; }
}
