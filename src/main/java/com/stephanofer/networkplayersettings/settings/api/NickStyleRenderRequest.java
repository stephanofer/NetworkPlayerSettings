package com.stephanofer.networkplayersettings.settings.api;

import java.util.Objects;
import java.util.UUID;

public record NickStyleRenderRequest(
    UUID playerId,
    String username,
    StylePermissionChecker permissionChecker
) {

    public NickStyleRenderRequest {
        playerId = Objects.requireNonNull(playerId, "playerId");
        username = Objects.requireNonNull(username, "username").trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker");
    }
}
