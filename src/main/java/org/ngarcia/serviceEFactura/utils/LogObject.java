package org.ngarcia.serviceEFactura.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import jakarta.xml.soap.SOAPMessage;

//import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;

public class LogObject {

    public static void log(Object o) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Formato legible
        String json = null;
        try {
            json = mapper.writeValueAsString(o);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public static void logSOAP(SOAPMessage soapMessage) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out); // Escribe el mensaje SOAP en un stream
            String soapXml = out.toString("UTF-8"); // Convierte a String
            System.out.println("SOAP Message:\n" + soapXml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
