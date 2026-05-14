package com.stephanofer.networkplayersettings.repository;

import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;

public record RepositoryLoadResult(PlayerSettingsSnapshot snapshot, boolean created) {
}
