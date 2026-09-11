package com.skyrush.social;

import com.skyrush.shared.GameException;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Achievements, daily challenge and real activity")
public class SocialController {
  private final AchievementService achievements;
  private final DailyService daily;
  private final ActivityService activity;

  public SocialController(
      AchievementService achievements, DailyService daily, ActivityService activity) {
    this.achievements = achievements;
    this.daily = daily;
    this.activity = activity;
  }

  @GetMapping("/achievements")
  public List<AchievementService.Achievement> achievements() {
    return achievements.current();
  }

  @GetMapping("/daily-challenge")
  public DailyService.Daily daily() {
    return daily.current();
  }

  @GetMapping("/activity")
  public ActivityService.View activity() {
    return activity.current();
  }

  @PostMapping("/presence/heartbeat")
  public ActivityService.View heartbeat(@RequestBody(required = false) Map<String, Object> body) {
    if (body != null && !body.isEmpty())
      throw new GameException(400, "INVALID_REQUEST", "Heartbeat accepts no player fields");
    return activity.heartbeat();
  }
}
