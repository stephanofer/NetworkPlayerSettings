package com.stephanofer.networkplayersettings.settings.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stephanofer.networkplayersettings.settings.api.NickStyleRenderRequest;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.settings.api.SettingKey;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class DefaultPlayerStyleServiceTest {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    @Test
    void rendersPersistedOfflineNickStyleWhenPermissionIsGranted() {
        final UUID playerId = UUID.randomUUID();
        final AtomicReference<String> checkedPermission = new AtomicReference<>();
        final DefaultPlayerStyleService service = service(snapshot(playerId, "gold"));

        final Component rendered = service.formattedNick(new NickStyleRenderRequest(
            playerId,
            "Vendimia",
            permission -> {
                checkedPermission.set(permission);
                return true;
            }
        )).join();

        assertEquals("networkplayersettings.nick.gold", checkedPermission.get());
        assertEquals("[Vendimia]", PLAIN_TEXT.serialize(rendered));
    }

    @Test
    void fallsBackToUsernameWhenOfflinePlayerLostTheRequiredPermission() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerStyleService service = service(snapshot(playerId, "gold"));

        final Component rendered = service.formattedNick(new NickStyleRenderRequest(playerId, "Vendimia", permission -> false)).join();

        assertEquals("Vendimia", PLAIN_TEXT.serialize(rendered));
    }

    @Test
    void fallsBackToUsernameWhenPersistedStyleNoLongerExists() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerStyleService service = service(snapshot(playerId, "removed-style"));

        final Component rendered = service.formattedNick(new NickStyleRenderRequest(playerId, "Vendimia", permission -> true)).join();

        assertEquals("Vendimia", PLAIN_TEXT.serialize(rendered));
    }

    @Test
    void doesNotResolvePermissionsForPublicStyles() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerStyleService service = service(snapshot(playerId, "public"));

        final Component rendered = service.formattedNick(new NickStyleRenderRequest(
            playerId,
            "Vendimia",
            permission -> {
                throw new AssertionError("public styles must not resolve permissions");
            }
        )).join();

        assertEquals("Vendimia", PLAIN_TEXT.serialize(rendered));
    }

    @Test
    void propagatesOfflineSettingsLoadFailures() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerStyleService service = service(CompletableFuture.failedFuture(new IllegalStateException("database unavailable")));

        final CompletionException exception = assertThrows(
            CompletionException.class,
            () -> service.formattedNick(new NickStyleRenderRequest(playerId, "Vendimia", permission -> true)).join()
        );

        assertEquals("database unavailable", exception.getCause().getMessage());
    }

    @Test
    void propagatesOfflinePermissionResolutionFailures() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerStyleService service = service(snapshot(playerId, "gold"));

        final CompletionException exception = assertThrows(
            CompletionException.class,
            () -> service.formattedNick(new NickStyleRenderRequest(
                playerId,
                "Vendimia",
                permission -> {
                    throw new IllegalStateException("permission service unavailable");
                }
            )).join()
        );

        assertEquals("permission service unavailable", exception.getCause().getMessage());
    }

    @Test
    void rejectsBlankOfflineUsername() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new NickStyleRenderRequest(UUID.randomUUID(), " ", permission -> true)
        );
    }

    @Test
    void replacesBothCatalogsWithoutReplacingTheService() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerStyleService service = service(snapshot(playerId, "replacement"));
        service.replaceCatalogs(
            new StylePatternCatalog(StylePatternType.NICK, List.of(new StylePattern(
                StylePatternType.NICK, "replacement", "Replacement", "basic", "", "[<name>]", "Preview"
            ))),
            catalog(StylePatternType.CHAT)
        );

        final Component rendered = service.formattedNick(new NickStyleRenderRequest(
            playerId, "Vendimia", permission -> true
        )).join();

        assertEquals("[Vendimia]", PLAIN_TEXT.serialize(rendered));
        assertEquals("replacement", service.nickPatterns().getFirst().id());
    }

    private static DefaultPlayerStyleService service(final PlayerSettingsSnapshot snapshot) {
        return service(CompletableFuture.completedFuture(snapshot));
    }

    private static DefaultPlayerStyleService service(final CompletableFuture<PlayerSettingsSnapshot> snapshot) {
        final PlayerSettingsService settingsService = (PlayerSettingsService) Proxy.newProxyInstance(
            PlayerSettingsService.class.getClassLoader(),
            new Class<?>[] {PlayerSettingsService.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "load" -> snapshot;
                case "getCachedOrDefault" -> snapshot.join();
                case "toString" -> "OfflineStyleTestSettingsService";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
        return new DefaultPlayerStyleService(
            settingsService,
            catalog(StylePatternType.NICK),
            catalog(StylePatternType.CHAT),
            new StylePatternRenderer()
        );
    }

    private static StylePatternCatalog catalog(final StylePatternType type) {
        final String placeholder = "<" + type.requiredPlaceholder() + ">";
        return new StylePatternCatalog(type, List.of(
            new StylePattern(type, "gold", "Gold", "basic", "networkplayersettings.nick.gold", "[" + placeholder + "]", "Preview"),
            new StylePattern(type, "public", "Public", "basic", "", "<green>" + placeholder + "</green>", "Preview")
        ));
    }

    private static PlayerSettingsSnapshot snapshot(final UUID playerId, final String nickStyle) {
        return new PlayerSettingsSnapshot(playerId, Map.of(SettingKey.NICK_STYLE, nickStyle));
    }
}
