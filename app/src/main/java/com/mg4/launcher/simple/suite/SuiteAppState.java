package com.mg4.launcher.simple.suite;

final class SuiteAppState {
    enum Action { NONE, OPEN, INSTALL, UPDATE }

    final String name;
    final String packageName;
    String installedVersion;
    long installedVersionCode = -1;
    String localVersion;
    String changelog;
    String downloadUrl;
    Action action = Action.NONE;

    SuiteAppState(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }
}
