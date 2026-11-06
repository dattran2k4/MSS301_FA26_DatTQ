package fu.de181019.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().info(new Info().title("Media booking API").version("1.0"));
  }
}
