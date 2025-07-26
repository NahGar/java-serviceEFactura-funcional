package org.ngarcia.serviceEFactura.config;

import java.util.ResourceBundle;

public class AppConfig {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("application");

    public static String getKeystorePassword() {
        return bundle.getString("keystore.password");
    }

    public static String getTruststorePassword() {
        return bundle.getString("truststore.password");
    }

    public static String getKeystorePath() {
        return bundle.getString("keystore.path");
    }

    public static String getTruststorePath() {
        return bundle.getString("truststore.path");
    }
}