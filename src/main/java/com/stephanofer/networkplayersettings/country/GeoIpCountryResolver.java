package com.stephanofer.networkplayersettings.country;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.stephanofer.networkplayersettings.api.CountryFlag;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GeoIpCountryResolver implements AutoCloseable {

    private final Logger logger;
    private final DatabaseReader reader;

    private GeoIpCountryResolver(final Logger logger, final DatabaseReader reader) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.reader = reader;
    }

    public static GeoIpCountryResolver disabled(final Logger logger) {
        return new GeoIpCountryResolver(logger, null);
    }

    public static GeoIpCountryResolver open(final Path databasePath, final Logger logger) {
        Objects.requireNonNull(databasePath, "databasePath");
        Objects.requireNonNull(logger, "logger");

        if (!Files.isRegularFile(databasePath)) {
            logger.warning("GeoIP country detection disabled: database file not found at " + databasePath.toAbsolutePath());
            return disabled(logger);
        }

        try {
            final DatabaseReader reader = new DatabaseReader.Builder(databasePath.toFile())
                .withCache(new CHMCache())
                .build();
            logger.info("GeoIP country detection enabled using " + databasePath.toAbsolutePath());
            return new GeoIpCountryResolver(logger, reader);
        } catch (final IOException exception) {
            logger.log(Level.SEVERE, "GeoIP country detection disabled: failed to open " + databasePath.toAbsolutePath(), exception);
            return disabled(logger);
        }
    }

    public boolean enabled() {
        return this.reader != null;
    }

    public String resolveCountryCode(final InetAddress address) {
        if (this.reader == null || address == null || isNonPublicAddress(address)) {
            return CountryFlag.UNKNOWN_CODE;
        }

        try {
            final CountryResponse response = this.reader.country(address);
            return Optional.ofNullable(response.country().isoCode())
                .map(CountryFlag::normalizeCode)
                .orElse(CountryFlag.UNKNOWN_CODE);
        } catch (final AddressNotFoundException exception) {
            return CountryFlag.UNKNOWN_CODE;
        } catch (final Exception exception) {
            this.logger.log(Level.WARNING, "Failed to resolve GeoIP country for player address", exception);
            return CountryFlag.UNKNOWN_CODE;
        }
    }

    @Override
    public void close() {
        if (this.reader != null) {
            try {
                this.reader.close();
            } catch (final IOException exception) {
                this.logger.log(Level.WARNING, "Failed to close GeoIP database reader", exception);
            }
        }
    }

    private static boolean isNonPublicAddress(final InetAddress address) {
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress();
    }
}
