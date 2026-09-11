package com.skyrush.rounds;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = {"skyrush.completion-enabled"},
    havingValue = "true",
    matchIfMissing = true)
public class RoundCompletionJob {
  private static final Logger LOG = LoggerFactory.getLogger(RoundCompletionJob.class);
  private final RoundRepository rounds;
  private final RoundService service;
  private final Clock clock;

  public RoundCompletionJob(RoundRepository rounds, RoundService service, Clock clock) {
    this.rounds = rounds;
    this.service = service;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${skyrush.completion-interval-ms:250}")
  public void completeDueRounds() {
    for (var id : rounds.unfinished()) {
      try {
        service.settle(id);
      } catch (RuntimeException ex) {
        LOG.error("Unable to complete round {}", id, ex);
      }
    }
  }
}
