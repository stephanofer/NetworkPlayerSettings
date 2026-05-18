# Public country asset API

NetworkPlayerSettings now publishes `NetworkAssetService` through Bukkit `ServicesManager` so other plugins can resolve country head assets without touching files, databases, or network calls during gameplay.

## Quick path

1. Add `depend: [NetworkPlayerSettings]` or `softdepend: [NetworkPlayerSettings]` in your plugin when you need country assets after enable.
2. Look up `NetworkAssetService` from Bukkit `ServicesManager` after both plugins are enabled.
3. Combine your existing `PlayerSettingsService.countryCode(UUID)` call with `NetworkAssetService.countryAsset(...)`.

## Usage

```java
final PlayerSettingsService playerSettings = Bukkit.getServicesManager().load(PlayerSettingsService.class);
final NetworkAssetService assets = Bukkit.getServicesManager().load(NetworkAssetService.class);

final String countryCode = playerSettings.countryCode(player.getUniqueId());
final CountryAsset asset = assets.countryAsset(countryCode);
```

## Behavior summary

| Topic | Decision |
|-------|----------|
| Runtime source | `plugins/NetworkPlayerSettings/assets/countries.yml` |
| Missing file | Copied once from bundled `assets/countries.yml` during startup |
| Lookup cost | Memory-only immutable cache; no DB/file/network work during lookups |
| Fallback | `countryAsset(...)` never returns `null`; invalid or unknown input falls back to `XX` |
| Startup safety | Plugin enable fails if catalog is malformed, missing `XX`, has collisions, blank names, or invalid base64 |

## Checklist

- [ ] Service lookup happens after `NetworkPlayerSettings` is enabled.
- [ ] Consumer code handles `CountryAsset` directly instead of re-reading the YAML file.
- [ ] Consumer code treats `XX` as the safe default asset.
