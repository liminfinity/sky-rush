package com.skyrush.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiDocumentation implements WebMvcConfigurer {
  @Bean
  OpenAPI skyRushApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("SkyRush gameplay API")
                .version("1.0")
                .description(
                    "Session-authenticated gameplay. GET /api/auth/csrf, then Authorize with its token. Login/register and fetch a fresh token after login. "
                        + "Create a round with a unique requestId, poll its state, and cash out after level 1. HttpOnly session cookie identifies the player."))
        .components(
            new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(
                    "csrf",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                        .name("X-CSRF-TOKEN")))
        .addSecurityItem(
            new io.swagger.v3.oas.models.security.SecurityRequirement().addList("csrf"));
  }

  @Bean
  OpenApiCustomizer gameplayErrors() {
    return api -> {
      api.getComponents()
          .getSchemas()
          .putAll(
              io.swagger.v3.core.converter.ModelConverters.getInstance()
                  .read(ApiErrorHandler.ApiError.class));
      api.getPaths()
          .forEach(
              (path, item) -> {
                if (!path.startsWith("/api/")) return;
                item.readOperations()
                    .forEach(
                        operation -> {
                          for (var entry :
                              java.util.Map.of(
                                      "401",
                                      "Authentication required",
                                      "403",
                                      "CSRF token required or evaluator access only",
                                      "400",
                                      "Invalid request",
                                      "404",
                                      "User or round not found",
                                      "409",
                                      "Invalid state, conflicting command, or insufficient balance",
                                      "503",
                                      "Demo disabled or invalid game configuration")
                                  .entrySet()) {
                            operation
                                .getResponses()
                                .addApiResponse(
                                    entry.getKey(),
                                    new ApiResponse()
                                        .description(entry.getValue())
                                        .content(
                                            new Content()
                                                .addMediaType(
                                                    "application/json",
                                                    new MediaType()
                                                        .schema(
                                                            new Schema<>()
                                                                .$ref(
                                                                    "#/components/schemas/ApiError")))));
                          }
                        });
              });
    };
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                return true;
              }
            })
        .addPathPatterns("/api/**");
  }
}
