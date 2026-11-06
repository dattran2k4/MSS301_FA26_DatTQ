package fu.de181019.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

public class PostDTO {
  private Long postId;
  private String advertiserName;
  private String advertiserEmail;
  private String advertiserPhone;

  @JsonFormat(
      pattern = "dd/MM/yyyy",
      timezone = "UTC",
      lenient = com.fasterxml.jackson.annotation.OptBoolean.FALSE)
  private Date startDate;

  @JsonFormat(
      pattern = "dd/MM/yyyy",
      timezone = "UTC",
      lenient = com.fasterxml.jackson.annotation.OptBoolean.FALSE)
  private Date endDate;

  private Integer numberOfPosts;
  private BigDecimal totalCost;
  private String status;
  private Long userId;

  public Long getPostId() {
    return postId;
  }

  public void setPostId(Long value) {
    this.postId = value;
  }

  public String getAdvertiserName() {
    return advertiserName;
  }

  public void setAdvertiserName(String value) {
    this.advertiserName = value;
  }

  public String getAdvertiserEmail() {
    return advertiserEmail;
  }

  public void setAdvertiserEmail(String value) {
    this.advertiserEmail = value;
  }

  public String getAdvertiserPhone() {
    return advertiserPhone;
  }

  public void setAdvertiserPhone(String value) {
    this.advertiserPhone = value;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date value) {
    this.startDate = value;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date value) {
    this.endDate = value;
  }

  public Integer getNumberOfPosts() {
    return numberOfPosts;
  }

  public void setNumberOfPosts(Integer value) {
    this.numberOfPosts = value;
  }

  public BigDecimal getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(BigDecimal value) {
    this.totalCost = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long value) {
    this.userId = value;
  }
}
