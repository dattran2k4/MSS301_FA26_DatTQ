package fu.de181019.gateway.config;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.*;
@Configuration
public class CorsConfig {
 @Bean public CorsWebFilter corsWebFilter() {
  CorsConfiguration c=new CorsConfiguration(); c.addAllowedOrigin("*"); c.addAllowedMethod("*"); c.addAllowedHeader("*");
  UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return new CorsWebFilter(s);
 }
}
