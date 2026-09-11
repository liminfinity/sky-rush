package com.skyrush.wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record Wallet(UUID userId, BigDecimal bonusBalance) {}
