package org.ngarcia.serviceEFactura.SOAP;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.Set;

public class CDataInjectorHandler implements SOAPHandler<SOAPMessageContext> {
    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        if ((Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            try {
                SOAPMessage m = ctx.getMessage();
                SOAPBody body = m.getSOAPBody();
                NodeList list = body.getElementsByTagNameNS("http://dgi.gub.uy","xmlData");
                if (list.getLength()>0) {
                    SOAPElement x = (SOAPElement) list.item(0);
                    // Coge el XML firmado que pusimos en el context:
                    String signed = (String) ctx.get("SIGNED_XML");
                    // limpia y mete el CDATA
                    while(x.hasChildNodes()) x.removeChild(x.getFirstChild());
                    x.appendChild(x.getOwnerDocument().createCDATASection(signed));
                    m.saveChanges();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    @Override
    public Set<QName> getHeaders() {
        return Set.of();
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return false;
    }

    @Override
    public void close(MessageContext context) {

    }
}

