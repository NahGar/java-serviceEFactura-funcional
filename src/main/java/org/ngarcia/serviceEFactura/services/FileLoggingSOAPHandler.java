package org.ngarcia.serviceEFactura.services;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.soap.SOAPMessage;

import javax.xml.namespace.QName;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class FileLoggingSOAPHandler implements SOAPHandler<SOAPMessageContext> {
    // Carpeta 'logs/soap' en el proyecto, al mismo nivel que src/
    private static final Path OUT_DIR = Paths.get(System.getProperty("user.dir"), "logs", "soap");

    public FileLoggingSOAPHandler() {
        try {
            // Asegura que exista el directorio
            Files.createDirectories(OUT_DIR);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear directorio de logs: " + OUT_DIR, e);
        }
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        SOAPMessage msg = context.getMessage();
        String direction = outbound ? "REQUEST" : "RESPONSE";
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String fileName = String.format("%s_%s.xml", direction, ts);
        Path filePath = OUT_DIR.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            msg.writeTo(fos);
            System.out.println("[FileLogging] Saved " + direction + ": " + filePath);
        } catch (Exception e) {
            System.err.println("Error saving SOAP " + direction + ": " + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        // Registrar faults igual que mensajes normales
        return handleMessage(context);
    }

    @Override
    public void close(MessageContext context) {
        // No hay limpieza que hacer
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }
}
