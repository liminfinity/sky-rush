package com.skyrush.users;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String displayName, Instant createdAt) {}
