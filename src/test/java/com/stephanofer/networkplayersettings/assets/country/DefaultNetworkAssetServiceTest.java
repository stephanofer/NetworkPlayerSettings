package com.stephanofer.networkplayersettings.assets.country;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stephanofer.networkplayersettings.assets.api.CountryAsset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultNetworkAssetServiceTest {

    @Test
    void replacesCatalogWithoutReplacingTheService() {
        final CountryAsset unknown = new CountryAsset("XX", "Unknown", VALID_BASE64, Set.of("unknown"));
        final CountryAsset argentina = new CountryAsset("AR", "Argentina", VALID_BASE64, Set.of("argentina"));
        final DefaultNetworkAssetService service = new DefaultNetworkAssetService(new CountryAssetCatalog(List.of(unknown)));

        service.replaceCatalog(new CountryAssetCatalog(List.of(unknown, argentina)));

        assertEquals(argentina, service.countryAsset("AR"));
    }

    private static final String VALID_BASE64 = "eyJ0ZXh0dXJlcyI6e319";

    @Test
    void returnsKnownAssetsAndFallbackSafely() {
        final CountryAsset unknown = new CountryAsset("XX", "Unknown", VALID_BASE64, Set.of("unknown"));
        final CountryAsset argentina = new CountryAsset("AR", "Argentina", VALID_BASE64, Set.of("argentina"));

        final DefaultNetworkAssetService service = new DefaultNetworkAssetService(new CountryAssetCatalog(List.of(unknown, argentina)));

        assertSame(argentina, service.countryAsset("AR"));
        assertSame(argentina, service.countryAsset("argentina"));
        assertSame(unknown, service.countryAsset("???"));
        assertSame(unknown, service.unknownCountryAsset());
    }

    @Test
    void exposesImmutableCanonicalAssetsOnly() {
        final CountryAsset unknown = new CountryAsset("XX", "Unknown", VALID_BASE64, Set.of("unknown"));
        final CountryAsset argentina = new CountryAsset("AR", "Argentina", VALID_BASE64, Set.of("argentina"));

        final DefaultNetworkAssetService service = new DefaultNetworkAssetService(new CountryAssetCatalog(List.of(unknown, argentina)));

        assertEquals(Set.of("XX", "AR"), service.countryAssets().keySet());
        assertThrows(UnsupportedOperationException.class, () -> service.countryAssets().put("BR", argentina));
    }
}
