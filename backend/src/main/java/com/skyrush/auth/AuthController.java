package com.skyrush.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  public record Login(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record Registration(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
      @NotBlank @Size(min = 1, max = 40) String displayName,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  private final AccountService accounts;
  private final HttpSessionSecurityContextRepository contexts;

  public AuthController(AccountService accounts, HttpSessionSecurityContextRepository contexts) {
    this.accounts = accounts;
    this.contexts = contexts;
  }

  @GetMapping("/csrf")
  @Operation(
      summary = "Get CSRF token before session writes; send headerName/token on every POST/PUT")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }

  @GetMapping("/me")
  @Operation(summary = "Get the authenticated account without security fields")
  public AccountService.Account me() {
    return accounts.me();
  }

  @PostMapping("/register")
  @Operation(summary = "Create a player account and sign in")
  public AccountService.Account register(
      @Valid @RequestBody Registration body, HttpServletRequest q, HttpServletResponse r) {
    checkPassword(body.password());
    return session(accounts.register(body.username(), body.displayName(), body.password()), q, r);
  }

  @PostMapping("/login")
  @Operation(summary = "Sign in using username and password")
  public AccountService.Account login(
      @Valid @RequestBody Login body, HttpServletRequest q, HttpServletResponse r) {
    checkPassword(body.password());
    return session(accounts.login(body.username(), body.password()), q, r);
  }

  private void checkPassword(String value) {
    if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new com.skyrush.shared.GameException(
          400, "INVALID_REQUEST", "Пароль должен занимать не более 72 байт UTF-8");
  }

  private AccountService.Account session(
      AccountService.Account account, HttpServletRequest q, HttpServletResponse r) {
    if (q.getSession(false) != null) q.getSession(false).invalidate();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            account.id().toString(),
            null,
            AuthorityUtils.createAuthorityList(
                account.evaluator() ? "ROLE_EVALUATOR" : "ROLE_PLAYER")));
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, q, r);
    return account;
  }

  @PostMapping("/logout")
  @Operation(summary = "Invalidate the authenticated server session")
  public Map<String, Boolean> logout(HttpServletRequest q, HttpServletResponse r) {
    new SecurityContextLogoutHandler()
        .logout(q, r, SecurityContextHolder.getContext().getAuthentication());
    return Map.of("loggedOut", true);
  }
}
