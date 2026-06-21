package com.stephanofer.networkplayersettings.settings.country;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeoIpCountryResolver implements AutoCloseable {

    private final Logger logger;
    private final DatabaseReader reader;

    protected GeoIpCountryResolver(final Logger logger, final DatabaseReader reader) {
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
        return resolveCountry(address).countryCode();
    }

    public CountryLookup resolveCountry(final InetAddress address) {
        if (this.reader == null || address == null || isNonPublicAddress(address)) {
            return CountryLookup.unknown(UnknownReason.NON_PUBLIC_ADDRESS);
        }

        try {
            final CountryResponse response = this.reader.country(address);
            return Optional.ofNullable(response.country().isoCode())
                .map(CountryFlag::normalizeCode)
                .filter(code -> !CountryFlag.UNKNOWN_CODE.equals(code))
                .map(CountryLookup::detected)
                .orElseGet(() -> CountryLookup.unknown(UnknownReason.NOT_FOUND));
        } catch (final AddressNotFoundException exception) {
            return CountryLookup.unknown(UnknownReason.NOT_FOUND);
        } catch (final Exception exception) {
            this.logger.log(Level.WARNING, "Failed to resolve GeoIP country for player address", exception);
            return CountryLookup.unknown(UnknownReason.ERROR);
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

    public record CountryLookup(String countryCode, UnknownReason unknownReason) {

        public CountryLookup {
            countryCode = CountryFlag.normalizeCode(countryCode);
            unknownReason = CountryFlag.UNKNOWN_CODE.equals(countryCode)
                ? Objects.requireNonNullElse(unknownReason, UnknownReason.NOT_FOUND)
                : null;
        }

        public static CountryLookup detected(final String countryCode) {
            final String normalized = CountryFlag.normalizeCode(countryCode);
            if (CountryFlag.UNKNOWN_CODE.equals(normalized)) {
                return unknown(UnknownReason.NOT_FOUND);
            }
            return new CountryLookup(normalized, null);
        }

        public static CountryLookup unknown(final UnknownReason reason) {
            return new CountryLookup(CountryFlag.UNKNOWN_CODE, Objects.requireNonNull(reason, "reason"));
        }

        public boolean detectedRealCountry() {
            return this.unknownReason == null && !CountryFlag.UNKNOWN_CODE.equals(this.countryCode);
        }
    }

    public enum UnknownReason {
        NON_PUBLIC_ADDRESS,
        NOT_FOUND,
        ERROR
    }
}
