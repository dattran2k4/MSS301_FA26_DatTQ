package fu.de181019.post.common;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
public final class Checks {
 private Checks() {}
 public static void require(boolean valid,String message) { if(!valid) throw new ApiException(2,message); }
 public static void text(String s,int max,String name) { require(s!=null && !s.isBlank() && s.length()<=max,name+" is required and must be at most "+max+" characters"); }
 public static <E extends Enum<E>> E enumeration(Class<E> type,String value) {
  try { return Enum.valueOf(type,value); } catch(Exception e) { throw new ApiException(2,"Invalid "+type.getSimpleName()); }
 }
 public static PageRequest page(int page,int size,String id) { require(page>=0 && size>=1 && size<=100,"page must be >= 0 and size must be 1–100"); return PageRequest.of(page,size,Sort.by(id)); }
 public static void money(BigDecimal value) { require(value!=null && value.signum()>0 && value.scale()<=2 && value.precision()-value.scale()<=16,"ratePerPost must be positive and fit decimal(18,2)"); }
}
