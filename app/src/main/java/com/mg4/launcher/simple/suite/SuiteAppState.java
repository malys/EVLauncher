package com.mg4.launcher.simple.suite;

import java.io.File;

final class SuiteAppState {
    enum Action { NONE, OPEN, INSTALL, UPDATE }

    final String name;
    final String packageName;
    String installedVersion;
    long installedVersionCode = -1;
    String localVersion;
    long localVersionCode = -1;
    String changelog;
    String downloadUrl;
    File verifiedApk;
    boolean invalidApk;
    Action action = Action.NONE;

    SuiteAppState(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }
}
