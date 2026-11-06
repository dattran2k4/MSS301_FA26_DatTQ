package com.mss301.watch.config;
import com.mss301.watch.client.SagaCallContext;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration public class InternalClientConfig {
 @Bean RequestInterceptor internalAuthentication(@Value("${internal.api-key}") String key) {
  return request -> {
   request.header("X-Internal-Key",key);
   String[] context=SagaCallContext.get();
   if (context!=null) {request.header("X-Saga-Id",context[0]);request.header("X-Saga-Phase",context[1]);}
  };
 }
}
