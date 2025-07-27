package org.ngarcia.serviceEFactura.services;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import org.ngarcia.serviceEFactura.config.AppConfig;
import uy.gub.dgi.cfe.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

public class ClienteDGIService {

   public static String enviarCFE(String signedXML, KeyStore ks, String alias, X509Certificate cert) {
      try {
         // Crear el servicio y el puerto
         WSEFactura servicio = new WSEFactura();
         WSEFacturaSoapPort port = servicio.getWSEFacturaSoapPort();

         // —————— FORZAR USO DE TU SSLContext ——————
         SSLSocketFactory myFactory = SSLContext.getDefault().getSocketFactory();
         BindingProvider bp = (BindingProvider) port;
         bp.getRequestContext().put("com.sun.xml.ws.transport.https.client.SSLSocketFactory",myFactory);

         // Configurar la URL del servicio web
         bp.getRequestContext().put("com.sun.xml.ws.transport.https.sslContext", SSLContext.getDefault());

         bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 "https://efactura.dgi.gub.uy:6443/ePrueba/ws_eprueba");
         bp.getRequestContext().put("SIGNED_XML", signedXML);

         // Crear el objeto de solicitud
         ObjectFactory factory = new ObjectFactory();
         WSEFacturaEFACRECEPCIONSOBRE request = factory.createWSEFacturaEFACRECEPCIONSOBRE();
         Data data = factory.createData();
         data.setXmlData(signedXML);
         request.setDatain(data);

         // Crear la cadena de manejadores y agregar el WS-Security Header Handler
         List<Handler> handlerChain = new ArrayList<>();

         String keystorePass = AppConfig.getKeystorePassword();
         PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePass.toCharArray());
         handlerChain.add(new WSSecurityHeaderSOAPHandler(cert, privateKey));
         handlerChain.add(new FileLoggingSOAPHandler());
         //no mover logging porque deja loguear
         handlerChain.add(new LoggingSOAPHandler());

         // Establecer la cadena de manejadores en el puerto
         ((BindingProvider) port).getBinding().setHandlerChain(handlerChain);

         // Invocar el servicio web
         WSEFacturaEFACRECEPCIONSOBREResponse response = port.efacrecepcionsobre(request);

         // Procesar la respuesta
         //System.out.println("RESPONSE " + response.getDataout().getXmlData());
         return response.getDataout().getXmlData();

      } catch (Exception e) {
         System.out.println("ERROR:" + e.getMessage());
         e.printStackTrace();
         throw new RuntimeException("Error al enviar CFE a la DGI: " + e.getMessage(), e);
      }
   }

   public static String enviarReporte(String signedXML, KeyStore ks, String alias, X509Certificate cert) {
      try {
         WSEFactura servicio = new WSEFactura();
         WSEFacturaSoapPort port = servicio.getWSEFacturaSoapPort();

         // Configuración SSL (igual que para CFE)
         SSLSocketFactory myFactory = SSLContext.getDefault().getSocketFactory();
         BindingProvider bp = (BindingProvider) port;
         bp.getRequestContext().put("com.sun.xml.ws.transport.https.client.SSLSocketFactory", myFactory);
         bp.getRequestContext().put("com.sun.xml.ws.transport.https.sslContext", SSLContext.getDefault());
         bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 "https://efactura.dgi.gub.uy:6443/ePrueba/ws_eprueba");

         // Crear solicitud específica para reportes
         ObjectFactory factory = new ObjectFactory();
         WSEFacturaEFACRECEPCIONREPORTE request = factory.createWSEFacturaEFACRECEPCIONREPORTE();
         Data data = factory.createData();
         data.setXmlData(signedXML);
         request.setDatain(data);

         // Configurar handlers (igual que para CFE)
         List<Handler> handlerChain = new ArrayList<>();
         String keystorePass = AppConfig.getKeystorePassword();
         PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePass.toCharArray());
         handlerChain.add(new WSSecurityHeaderSOAPHandler(cert, privateKey));
         handlerChain.add(new FileLoggingSOAPHandler());
         handlerChain.add(new LoggingSOAPHandler());
         bp.getBinding().setHandlerChain(handlerChain);

         // DEBUG: Imprimir XML firmado antes de enviar
         System.out.println("=== XML FIRMADO PARA REPORTE ===");
         System.out.println(signedXML);
         System.out.println("===============================");

         // Invocar servicio específico para reportes
         WSEFacturaEFACRECEPCIONREPORTEResponse response = port.efacrecepcionreporte(request);

         return response.getDataout().getXmlData();
      } catch (Exception e) {
         throw new RuntimeException("Error al enviar reporte a la DGI: " + e.getMessage(), e);
      }
   }

}
