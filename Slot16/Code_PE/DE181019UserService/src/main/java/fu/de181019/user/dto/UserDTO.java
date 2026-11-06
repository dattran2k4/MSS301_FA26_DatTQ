package fu.de181019.user.dto;

import java.math.BigDecimal;

public class UserDTO {
  private Long userId;
  private String username;
  private String email;
  private String role;
  private BigDecimal ratePerPost;
  private Integer maxDailyPosts;
  private Integer level;
  private String status;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    this.userId = value;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String value) {
    this.username = value;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String value) {
    this.email = value;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String value) {
    this.role = value;
  }

  public BigDecimal getRatePerPost() {
    return ratePerPost;
  }

  public void setRatePerPost(BigDecimal value) {
    this.ratePerPost = value;
  }

  public Integer getMaxDailyPosts() {
    return maxDailyPosts;
  }

  public void setMaxDailyPosts(Integer value) {
    this.maxDailyPosts = value;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(Integer value) {
    this.level = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
