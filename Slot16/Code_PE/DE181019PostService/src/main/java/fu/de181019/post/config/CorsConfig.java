package fu.de181019.post.config;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;
@Configuration
public class CorsConfig {
 @Bean public CorsFilter corsFilter() {
  CorsConfiguration c=new CorsConfiguration(); c.addAllowedOrigin("*"); c.addAllowedMethod("*"); c.addAllowedHeader("*");
  UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return new CorsFilter(s);
 }
}
