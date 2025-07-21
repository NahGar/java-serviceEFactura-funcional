package org.ngarcia.serviceEFactura.services;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.soap.SOAPMessage;

import javax.xml.namespace.QName;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class FileLoggingSOAPHandler
        implements SOAPHandler<SOAPMessageContext> {

    private static final String OUT_DIR = "logs/soap";

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        SOAPMessage msg = context.getMessage();
        String direction = outbound ? "REQUEST" : "RESPONSE";
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String fileName = String.format("%s_%s.xml", direction, ts);

        try (FileOutputStream fos = new FileOutputStream(OUT_DIR + "/" + fileName)) {
            msg.writeTo(fos);
            System.out.println("[FileLogging] Saved " + direction + ": " + fileName);
        } catch (Exception e) {
            System.err.println("Error saving SOAP " + direction + ": " + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return handleMessage(context);
    }

    @Override
    public void close(MessageContext context) {
        // nothing to clean up
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }
}
