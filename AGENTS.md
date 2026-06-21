## Non negotiables

- I want the project to have as little over-engineering as possible. We want something lightweight, functional, simple, scalable, easy to maintain, auditable, and debuggable — meaning we shouldn't introduce unnecessary complexity that will cause us pain down the line. That said, this doesn't mean we're going to do things poorly — everything must have the best performance and efficiency, each component must fulfill its responsibilities correctly, and we want the fewest bugs and errors possible. No performance issues, no inefficiencies — we want something ultra-performant.'
- Remember that if you need information about anything, we have a docs/ directory where you'll find documentation for everything you might need regarding PaperMC, Adventure, zMenu, — that's what's available for now. So if you need any information to improve your output and do things correctly by following the docs, that would be great. Whenever you need to do something and you're not sure how to handle it at the level of these dependencies, go to docs/ and you'll find what you're looking for. Also, everything is fragmented — this is very important — DO NOT read an entire file all at once, it's fragmented so you don't waste tokens and context reading things you don't need for what you're currently working on.

## Directory Architecture

This project uses a feature-first package structure. When adding new code, place it where a future developer can understand the feature just by reading the directory name. Do not create generic technical buckets unless the code is truly platform/infrastructure code shared by multiple features.

### Core plugin: `src/main/java/com/stephanofer/networkplayersettings`

| Directory | Responsibility |
|---|---|
| `settings/` | Main player settings domain. New player preferences such as sounds, scoreboard, visibility, notifications, particles, or chat settings should start here. |
| `settings/api/` | Public contracts for consuming player settings from other plugins. Keep this small and stable. |
| `settings/application/` | Application logic for settings: cache, ready state, mutation flow, event dispatching, and orchestration. |
| `settings/storage/` | Persistence contracts and implementations for settings. |
| `settings/event/` | Events emitted by the settings domain. |
| `settings/language/` | Language setting domain: supported languages, persisted preference, and resolution logic. |
| `settings/country/` | Country setting domain: country code normalization and GeoIP detection. |
| `assets/` | Reusable network assets that are not themselves player settings. |
| `assets/api/` | Public contracts for consuming assets from other plugins. |
| `assets/country/` | Country asset catalog/loading/service implementation. |
| `platform/bukkit/` | Bukkit/Paper adapters: listeners, PlaceholderAPI integration, YAML loading, scheduler-facing glue. |
| `config/` | Typed configuration model. |

`NetworkPlayerSettingsPlugin.java` should remain the plugin entry point only. If startup grows, extract bootstrap/orchestration classes instead of turning `onEnable` into a giant method.

### zMenu addon: `networkplayersettings-zmenu/src/main/java/com/stephanofer/networkplayersettingszmenu`

| Directory | Responsibility |
|---|---|
| `settings/` | zMenu UI related to global player settings. |
| `settings/language/` | zMenu buttons/loaders specific to the language setting. |
| `settings/view/` | Menu/dialog loading and opening logic for settings views. |
| `command/` | Commands exposed by the zMenu addon. |
| `config/` | Typed configuration for the zMenu addon. |
| `i18n/` | Addon message bundles and message lookup helpers. |

`NetworkPlayerSettingsZMenuPlugin.java` should remain the addon entry point only. UI-specific behavior belongs under `settings/`, not in the root package.

### Rules for future features

- Add feature-specific code under the feature directory first, for example `settings/sounds`, `settings/scoreboard`, or `settings/visibility`.
- Keep public API close to the domain it exposes, for example `settings/api` or `assets/api`.
- Keep Bukkit/Paper glue in `platform/bukkit`; do not mix event listener wiring into pure domain classes unless there is a strong reason.
- Keep persistence in `settings/storage`; do not let feature code write SQL directly.
- Avoid creating broad folders like `manager`, `service`, `util`, or `common` unless the responsibility is truly shared and clearly named.
- When adding a new setting, define its `SettingKey`, default value, validation rules, persistence behavior, and event behavior before implementing UI.
