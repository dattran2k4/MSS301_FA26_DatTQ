package fu.de181019.user.dto;
import java.util.List;
import org.springframework.data.domain.Page;
public record PageDTO<T>(int size,int page,int totalPages,long totalElements,boolean first,boolean last,List<T> content) {
 public static <T> PageDTO<T> from(Page<T> p) { return new PageDTO<>(p.getSize(),p.getNumber(),p.getTotalPages(),p.getTotalElements(),p.isFirst(),p.isLast(),p.getContent()); }
}
