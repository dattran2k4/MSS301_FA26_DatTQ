package fu.de181019.post.service.impl;
import fu.de181019.post.dto.*;
import fu.de181019.post.entity.*;
import fu.de181019.post.repository.PostRepository;
import fu.de181019.post.service.PostService;
import fu.de181019.post.client.UserClient;
import fu.de181019.post.common.*;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import static fu.de181019.post.common.Checks.*;
@Service
@Transactional
public class PostServiceImpl implements PostService {
 private final PostRepository repository;
 private final UserClient client;
 private final Validator validator;
 public PostServiceImpl(PostRepository repository,UserClient client,Validator validator) { this.repository=repository; this.client=client; this.validator=validator; }
 private record EmailValue(@Email String value) {}
 private Post find(long id) { return repository.findById(id).orElseThrow(()->new ApiException(4,"Post not found")); }
 private UserDTO fetch(long id) {
  try {
   ApiResponse<UserDTO> response=client.get(id);
   if(response!=null && response.status()==4) throw new ApiException(4,"User not found");
   if(response==null || response.status()!=1 || response.data()==null) throw new IllegalStateException("Invalid UserService response");
   return response.data();
  } catch(feign.FeignException.NotFound e) { throw new ApiException(4,"User not found"); }
 }
 private void apply(PostDTO d,Post p) {
  if(d.getAdvertiserName()!=null) p.setAdvertiserName(d.getAdvertiserName());
  if(d.getAdvertiserEmail()!=null) p.setAdvertiserEmail(d.getAdvertiserEmail());
  if(d.getAdvertiserPhone()!=null) p.setAdvertiserPhone(d.getAdvertiserPhone());
  if(d.getStartDate()!=null) p.setStartDate(d.getStartDate());
  if(d.getEndDate()!=null) p.setEndDate(d.getEndDate());
  if(d.getNumberOfPosts()!=null) p.setNumberOfPosts(d.getNumberOfPosts());
  if(d.getUserId()!=null) p.setUserId(d.getUserId());
  if(d.getStatus()!=null) p.setStatus(enumeration(PostStatus.class,d.getStatus()));
 }
 private static LocalDate date(Date d) {
  return d instanceof java.sql.Date sql?sql.toLocalDate():Instant.ofEpochMilli(d.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
 }
 private void validate(Post p) {
  text(p.getAdvertiserName(),100,"advertiserName"); text(p.getAdvertiserEmail(),100,"advertiserEmail"); text(p.getAdvertiserPhone(),20,"advertiserPhone");
  require(validator.validate(new EmailValue(p.getAdvertiserEmail())).isEmpty(),"Invalid advertiserEmail");
  require(p.getStartDate()!=null && p.getEndDate()!=null,"startDate and endDate are required");
  require(date(p.getEndDate()).isAfter(date(p.getStartDate())),"endDate must be after startDate");
  require(p.getNumberOfPosts()!=null && p.getNumberOfPosts()>=1,"numberOfPosts must be >= 1");
  require(p.getUserId()!=null && p.getUserId()>0,"userId must be positive");
 }
 private void price(Post p,boolean requireActive) {
  UserDTO user=fetch(p.getUserId());
  if(requireActive && !"ACTIVE".equals(user.getStatus())) throw new ApiException(5,"User is not ACTIVE");
  require(p.getNumberOfPosts()<=user.getMaxDailyPosts(),"numberOfPosts exceeds maxDailyPosts");
  long days=ChronoUnit.DAYS.between(date(p.getStartDate()),date(p.getEndDate()));
  BigDecimal cost=user.getRatePerPost().multiply(BigDecimal.valueOf(days));
  require(cost.precision()-cost.scale()<=16,"totalCost exceeds decimal(18,2)");
  p.setTotalCost(cost);
 }
 private PostDTO dto(Post p) { PostDTO d=new PostDTO(); BeanUtils.copyProperties(p,d); d.setStatus(p.getStatus().name()); return d; }
 private PostDetailDTO detail(Post p) { PostDetailDTO d=new PostDetailDTO(); BeanUtils.copyProperties(p,d); d.setStatus(p.getStatus().name()); d.setUser(fetch(p.getUserId())); return d; }
 @Override public PostDTO create(PostDTO d) { Post p=new Post(); p.setStatus(PostStatus.SCHEDULED); apply(d,p); validate(p); price(p,true); return dto(repository.saveAndFlush(p)); }
 @Override public PostDTO update(long id,PostDTO d) {
  Post p=find(id); boolean changedUser=d.getUserId()!=null && !d.getUserId().equals(p.getUserId());
  apply(d,p); validate(p); price(p,changedUser); return dto(repository.saveAndFlush(p));
 }
 @Override @Transactional(readOnly=true) public PostDetailDTO get(long id) { return detail(find(id)); }
 @Override public void cancel(long id) { Post p=find(id); p.setStatus(PostStatus.CANCELLED); repository.save(p); }
 @Override @Transactional(readOnly=true) public PageDTO<PostDetailDTO> list(int page,int size,String advertiserName,String status) {
  var pageable=Checks.page(page,size,"postId"); PostStatus s=status==null?null:enumeration(PostStatus.class,status);
  Specification<Post> spec=(root,query,cb)-> {
   var predicates=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
   if(advertiserName!=null && !advertiserName.isBlank()) {
    String literal=advertiserName.replace("!","!!").replace("%","!%").replace("_","!_");
    predicates.add(cb.like(cb.lower(root.get("advertiserName")),"%"+literal.toLowerCase(java.util.Locale.ROOT)+"%",'!'));
   }
   if(s!=null) predicates.add(cb.equal(root.get("status"),s));
   return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
  };
  return PageDTO.from(repository.findAll(spec,pageable).map(this::detail));
 }
}
