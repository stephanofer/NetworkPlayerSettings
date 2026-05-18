package com.stephanofer.networkplayersettings.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stephanofer.networkplayersettings.api.CountryAsset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CountryAssetCatalogTest {

    private static final String VALID_BASE64 = "eyJ0ZXh0dXJlcyI6e319";

    @Test
    void resolvesKnownCodesAliasesAndFallbacks() {
        final CountryAsset unknown = new CountryAsset("XX", "Unknown", VALID_BASE64, Set.of("unknown"));
        final CountryAsset argentina = new CountryAsset("AR", "Argentina", VALID_BASE64, Set.of("argentina", "south-america"));

        final CountryAssetCatalog catalog = new CountryAssetCatalog(List.of(unknown, argentina));

        assertSame(argentina, catalog.find("AR"));
        assertSame(argentina, catalog.find("ar"));
        assertSame(argentina, catalog.find("Argentina"));
        assertSame(unknown, catalog.find(null));
        assertSame(unknown, catalog.find("   "));
        assertSame(unknown, catalog.find("???"));
    }

    @Test
    void rejectsAliasCollisionsAndMissingFallback() {
        final CountryAsset unknown = new CountryAsset("XX", "Unknown", VALID_BASE64, Set.of("unknown"));
        final CountryAsset argentina = new CountryAsset("AR", "Argentina", VALID_BASE64, Set.of("shared"));
        final CountryAsset armenia = new CountryAsset("AM", "Armenia", VALID_BASE64, Set.of("shared"));
        final CountryAsset aliasCollisionWithCode = new CountryAsset("BR", "Brazil", VALID_BASE64, Set.of("ar"));

        final IllegalArgumentException aliasCollision = assertThrows(
            IllegalArgumentException.class,
            () -> new CountryAssetCatalog(List.of(unknown, argentina, armenia))
        );
        assertTrue(aliasCollision.getMessage().contains("shared"));

        final IllegalArgumentException aliasCodeCollision = assertThrows(
            IllegalArgumentException.class,
            () -> new CountryAssetCatalog(List.of(unknown, argentina, aliasCollisionWithCode))
        );
        assertTrue(aliasCodeCollision.getMessage().contains("ar"));

        final IllegalArgumentException missingFallback = assertThrows(
            IllegalArgumentException.class,
            () -> new CountryAssetCatalog(List.of(argentina))
        );
        assertTrue(missingFallback.getMessage().contains("XX"));
    }

    @Test
    void exposesImmutableCanonicalMapAndCopiesAliasesDefensively() {
        final CountryAsset unknown = new CountryAsset("xx", "Unknown", VALID_BASE64, Set.of("unknown"));
        final CountryAsset argentina = new CountryAsset(" ar ", "Argentina", VALID_BASE64, Set.of("Argentina"));

        final CountryAssetCatalog catalog = new CountryAssetCatalog(List.of(unknown, argentina));

        assertEquals(Set.of("argentina"), argentina.aliases());
        assertThrows(UnsupportedOperationException.class, () -> argentina.aliases().add("mutate"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.countryAssets().put("BR", unknown));
    }
}
