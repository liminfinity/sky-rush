package com.skyrush.rounds;

import com.skyrush.gameconfig.GameTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StartRoundRequest(
    @NotNull @Schema(
            description = "Unique command ID; reuse only to retry this exact start",
            example = "2c60592f-b44c-4da2-8695-e18a98b9124d")
        UUID requestId,
    @NotNull @Schema(example = "GREEN") GameTheme theme,
    @NotBlank @Size(max = 32) @Schema(example = "DOUBLE") String betOptionId) {}
