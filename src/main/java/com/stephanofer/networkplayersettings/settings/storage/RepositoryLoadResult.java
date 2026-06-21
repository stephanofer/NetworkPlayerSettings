package com.stephanofer.networkplayersettings.settings.storage;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;

public record RepositoryLoadResult(PlayerSettingsSnapshot snapshot, boolean created) {
}
