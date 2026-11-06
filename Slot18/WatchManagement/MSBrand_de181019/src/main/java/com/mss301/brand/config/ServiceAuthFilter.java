package com.mss301.brand.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Component
public class ServiceAuthFilter extends OncePerRequestFilter {
 @Value("${jwt.secret}") private String secret;
 @Value("${internal.api-key}") private String internalKey;
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  String path=req.getRequestURI();
  if (path.startsWith("/internal/")) {
   String key=req.getHeader("X-Internal-Key");
   if (key==null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),internalKey.getBytes(StandardCharsets.UTF_8))) { reject(res,401);return; }
   chain.doFilter(req,res); return;
  }
  if ("GET".equals(req.getMethod()) && "/api/watches".equals(path)) { chain.doFilter(req,res); return; }
  String token=req.getHeader("Authorization");
  Claims claims;
  try {
   if (token==null || !token.startsWith("Bearer ")) { reject(res,401);return; }
   claims=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build().parseSignedClaims(token.substring(7)).getPayload();
   if (!"access".equals(claims.get("type",String.class))) { reject(res,401);return; }
  } catch (RuntimeException ex) { reject(res,401);return; }
  String role=String.valueOf(claims.get("role",Integer.class));
  if ((path.startsWith("/api/sagas") || (!"GET".equals(req.getMethod()) && path.startsWith("/api/watches"))) && !"1".equals(role)) { reject(res,403);return; }
  chain.doFilter(new HttpServletRequestWrapper(req) {
   @Override public String getHeader(String name) {
    if ("X-User-Role".equalsIgnoreCase(name)) return role;
    if ("X-User-Id".equalsIgnoreCase(name)) return claims.getSubject();
    return super.getHeader(name);
   }
  },res);
 }
 private void reject(HttpServletResponse res,int status) throws IOException {
  res.setStatus(status);res.setContentType("application/json");
  res.getWriter().write("{\"success\":false,\"message\":\""+(status==401?"Authentication required":"Administrator required")+"\",\"data\":null}");
 }
}
