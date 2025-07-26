package org.ngarcia.serviceEFactura.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.ngarcia.serviceEFactura.config.AppConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

@Startup
@Singleton
@Priority(1)
public class SSLBootstrap {
   //private static final String KEYSTORE_PATH = "certs/certprueba-Prueba.0.pfx";
   //private static final String TRUSTSTORE_PATH = "certs/truststore.jks"; // Ruta relativa al classpath
   //private static final String KEYSTORE_PASS = "Prueba.0";
   //private static final String TRUSTSTORE_PASS = "123456";

   @PostConstruct
   public void initDefaultSSLContext() {
      try {
         // Usar AppConfig
         String KEYSTORE_PATH = AppConfig.getKeystorePath();
         String TRUSTSTORE_PATH = AppConfig.getTruststorePath();
         String KEYSTORE_PASS = AppConfig.getKeystorePassword();
         String TRUSTSTORE_PASS = AppConfig.getTruststorePassword();

         // Cargar keystore
         InputStream keystoreStream = getClass().getClassLoader().getResourceAsStream(KEYSTORE_PATH);
         KeyStore keyStore = KeyStore.getInstance("PKCS12");
         keyStore.load(keystoreStream, KEYSTORE_PASS.toCharArray());

         // Cargar truststore
         InputStream truststoreStream = getClass().getClassLoader().getResourceAsStream(TRUSTSTORE_PATH);
         //KeyStore trustStore = KeyStore.getInstance("JKS");
         KeyStore trustStore = KeyStore.getInstance("PKCS12");
         trustStore.load(truststoreStream, TRUSTSTORE_PASS.toCharArray()); // Usar la contraseña correcta

         // Configurar KeyManagerFactory
         KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         kmf.init(keyStore, KEYSTORE_PASS.toCharArray());

         // Configurar TrustManagerFactory
         TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         tmf.init(trustStore); // Inicializar con el trustStore

         // Crear SSLContext
         SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
         sslContext.init(
                 kmf.getKeyManagers(),
                 tmf.getTrustManagers(), // Usar los TrustManagers del truststore
                 new SecureRandom()
         );

         SSLContext.setDefault(sslContext);
         System.out.println("✅ Default SSLContext configurado con éxito");

      } catch (Exception e) {
         throw new RuntimeException("Error configurando SSLContext", e);
      }
   }
}

