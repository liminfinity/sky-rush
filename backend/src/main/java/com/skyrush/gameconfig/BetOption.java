package com.skyrush.gameconfig;

import java.math.BigDecimal;

public record BetOption(String id, BigDecimal stake, int boosterMultiplier) {}
