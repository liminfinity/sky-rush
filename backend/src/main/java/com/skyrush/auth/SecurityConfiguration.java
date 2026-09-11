package com.skyrush.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  HttpSessionSecurityContextRepository contexts() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, HttpSessionSecurityContextRepository contexts)
      throws Exception {
    return http.securityContext(c -> c.securityContextRepository(contexts))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/csrf",
                        "/actuator/health/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("EVALUATOR")
                    .anyRequest()
                    .authenticated())
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (q, r, x) -> {
                          r.setCharacterEncoding("UTF-8");
                          r.setStatus(401);
                          r.setContentType("application/json");
                          r.getWriter()
                              .write(
                                  "{\"code\":\"UNAUTHENTICATED\",\"message\":\"Войдите в аккаунт\"}");
                        })
                    .accessDeniedHandler(
                        (q, r, x) -> {
                          r.setCharacterEncoding("UTF-8");
                          r.setStatus(403);
                          r.setContentType("application/json");
                          r.getWriter()
                              .write(
                                  "{\"code\":\"FORBIDDEN\",\"message\":\"Нет доступа или сессия устарела\"}");
                        }))
        .build(); // Default session-backed CSRF protection remains enabled, including
    // login/register.
  }
}
