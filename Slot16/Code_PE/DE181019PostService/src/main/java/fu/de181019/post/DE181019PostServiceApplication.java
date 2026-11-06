package fu.de181019.post;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@EnableFeignClients
@SpringBootApplication
public class DE181019PostServiceApplication {
 public static void main(String[] args) { SpringApplication.run(DE181019PostServiceApplication.class, args); }
}
