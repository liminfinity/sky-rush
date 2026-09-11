package com.skyrush.shared;

import com.skyrush.gamemath.GameMath;
import com.skyrush.gamemath.JavaRandomProvider;
import com.skyrush.gamemath.RandomProvider;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RuntimeConfiguration {
  @Bean
  public Clock gameClock() {
    return Clock.systemUTC();
  }

  @Bean
  public GameMath gameMath() {
    return new GameMath();
  }

  @Bean
  public RandomProvider randomProvider(@Value("${skyrush.random-seed:}") String seed) {
    return seed.isBlank() ? new JavaRandomProvider() : new JavaRandomProvider(Long.parseLong(seed));
  }
}
