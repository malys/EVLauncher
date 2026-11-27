package com.evsuite.launcher.suite;

final class SuiteAppState {
    enum Action { NONE, OPEN, INSTALL, UPDATE }

    final String name;
    final String packageName;
    /**
     * SHA-256 of the one certificate a third-party app must carry, or null for a suite app,
     * which must share EVLauncher's own key instead.
     */
    final String thirdPartySigner;
    String installedVersion;
    long installedVersionCode = -1;
    boolean disabled;
    String localVersion;
    String changelog;
    String downloadUrl;
    Action action = Action.NONE;

    SuiteAppState(String name, String packageName) {
        this(name, packageName, null);
    }

    SuiteAppState(String name, String packageName, String thirdPartySigner) {
        this.name = name;
        this.packageName = packageName;
        this.thirdPartySigner = thirdPartySigner;
    }
}
