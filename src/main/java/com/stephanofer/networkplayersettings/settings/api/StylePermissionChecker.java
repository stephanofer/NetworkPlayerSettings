package com.stephanofer.networkplayersettings.settings.api;

@FunctionalInterface
public interface StylePermissionChecker {

    boolean hasPermission(String permission);
}
