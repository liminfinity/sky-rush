package com.skyrush.users;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
  private final CurrentUserService users;

  public UserController(CurrentUserService users) {
    this.users = users;
  }

  @GetMapping("/api/users/demo")
  @Operation(
      summary = "Get current player (legacy URI)",
      description =
          "Compatibility endpoint: returns the authenticated player, never a selectable demo identity.")
  public User demo() {
    return users.get();
  }
}
