package fu.de181019.post.entity;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
@Entity
@Table(name="posts")
public class Post {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="post_id", nullable=false)
 private Long postId;
 @Column(name="advertiser_name", nullable=false, length=100)
 private String advertiserName;
 @Column(name="advertiser_email", nullable=false, length=100)
 private String advertiserEmail;
 @Column(name="advertiser_phone", nullable=false, length=20)
 private String advertiserPhone;
 @Temporal(TemporalType.DATE)
 @Column(name="start_date", nullable=false)
 private Date startDate;
 @Temporal(TemporalType.DATE)
 @Column(name="end_date", nullable=false)
 private Date endDate;
 @Column(name="number_of_posts", nullable=false)
 private Integer numberOfPosts;
 @Column(name="total_cost", nullable=false, precision=18, scale=2)
 private BigDecimal totalCost;
 @Enumerated(EnumType.STRING)
 @Column(name="status", nullable=false, length=15)
 private PostStatus status;
 @Column(name="user_id", nullable=false)
 private Long userId;
 public Long getPostId() { return postId; }
 public void setPostId(Long value) { this.postId=value; }
 public String getAdvertiserName() { return advertiserName; }
 public void setAdvertiserName(String value) { this.advertiserName=value; }
 public String getAdvertiserEmail() { return advertiserEmail; }
 public void setAdvertiserEmail(String value) { this.advertiserEmail=value; }
 public String getAdvertiserPhone() { return advertiserPhone; }
 public void setAdvertiserPhone(String value) { this.advertiserPhone=value; }
 public Date getStartDate() { return startDate; }
 public void setStartDate(Date value) { this.startDate=value; }
 public Date getEndDate() { return endDate; }
 public void setEndDate(Date value) { this.endDate=value; }
 public Integer getNumberOfPosts() { return numberOfPosts; }
 public void setNumberOfPosts(Integer value) { this.numberOfPosts=value; }
 public BigDecimal getTotalCost() { return totalCost; }
 public void setTotalCost(BigDecimal value) { this.totalCost=value; }
 public PostStatus getStatus() { return status; }
 public void setStatus(PostStatus value) { this.status=value; }
 public Long getUserId() { return userId; }
 public void setUserId(Long value) { this.userId=value; }
}
