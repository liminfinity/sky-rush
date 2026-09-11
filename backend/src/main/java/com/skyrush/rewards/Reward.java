package com.skyrush.rewards;

import java.math.BigDecimal;

public record Reward(
    int fragmentsGranted, long fragmentsRedeemed, BigDecimal bonusBalanceGranted) {}
