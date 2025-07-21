package org.ngarcia.serviceEFactura.services;

// — WS-Security + SOAP (Jakarta EE) —
import jakarta.xml.soap.*;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.MessageContext;

// — XML-DSig (JSR-105) —
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dom.DOMStructure;

// — W3C DOM —
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

// — Crypto estándar —
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import javax.xml.namespace.QName;

import static org.bouncycastle.asn1.iana.IANAObjectIdentifiers.security;

public class WSSecurityHeaderSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    private final X509Certificate certificate;
    private final PrivateKey privateKey;
    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    public WSSecurityHeaderSOAPHandler(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean isOutbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (isOutbound) {
            try {
                SOAPMessage soapMessage = context.getMessage();
                SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();

                // ——— 1) ENVUELVE xmlData EN CDATA ———
                SOAPBody body = envelope.getBody();
                NodeList xmlDataList = body.getElementsByTagNameNS("http://dgi.gub.uy", "xmlData");
                if (xmlDataList.getLength() > 0) {
                    SOAPElement xmlDataElem = (SOAPElement) xmlDataList.item(0);
                    String originalSignedXML = (String) context.get("SIGNED_XML");
                    while (xmlDataElem.hasChildNodes()) {
                        xmlDataElem.removeChild(xmlDataElem.getFirstChild());
                    }
                    CDATASection cdata = xmlDataElem.getOwnerDocument().createCDATASection(originalSignedXML);
                    xmlDataElem.appendChild(cdata);
                    soapMessage.saveChanges();
                }

                // ——— 2) AÑADE WS-SECURITY HEADER ———
                // declarar namespaces
                envelope.addNamespaceDeclaration("wsse", WSSE_NS);
                envelope.addNamespaceDeclaration("wsu", WSU_NS);
                envelope.addNamespaceDeclaration("ds", DS_NS);

                String bodyId = "id-1234567890ABCDEF";
                body.addAttribute(
                        new QName(WSU_NS, "Id", "wsu"),
                        bodyId
                );

                SOAPHeader header = envelope.getHeader();
                if (header == null) header = envelope.addHeader();

                // crear el elemento <wsse:Security>
                SOAPHeaderElement security = header.addHeaderElement(
                        new QName(WSSE_NS, "Security", "wsse")
                );

                // Indicar que está entendido y no hacer MustUnderstand
                security.setMustUnderstand(false);

                security.setAttributeNS(envelope.getNamespaceURI(),
                        envelope.getPrefix() + ":mustUnderstand", "0"
                );

                // crear <wsse:BinarySecurityToken>
                SOAPElement binarySecurityToken = security.addChildElement(
                        new QName(WSSE_NS, "BinarySecurityToken", "wsse")
                );
                binarySecurityToken.setAttribute("EncodingType",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
                );
                binarySecurityToken.setAttribute("ValueType",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
                );
                binarySecurityToken.setAttributeNS(WSU_NS, "wsu:Id", "X509-5DFA3742A71FBC99CA17475800568446");
                binarySecurityToken.setTextContent(Base64.getEncoder().encodeToString(certificate.getEncoded()));

                addSignature(envelope, bodyId, security);

                soapMessage.saveChanges();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void addSignature(SOAPEnvelope envelope, String bodyId, SOAPHeaderElement security) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, MarshalException, XMLSignatureException {
        Document doc = envelope.getOwnerDocument();

        // 1) Preparamos el XMLSignatureFactory
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // 2) Creamos la referencia al cuerpo firmado (URI="#id-…")
        Reference ref = fac.newReference(
                "#" + bodyId,
                fac.newDigestMethod(DigestMethod.SHA1, null),
                Collections.singletonList(
                        fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)
                ),
                null,
                null
        );

        // 3) Construimos el SignedInfo
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(
                        CanonicalizationMethod.EXCLUSIVE,
                        (C14NMethodParameterSpec) new ExcC14NParameterSpec(Collections.singletonList("dgi"))
                ),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref)
        );

        // 4) Creamos el KeyInfo que apunta al BinarySecurityToken
        //    usando tu clase SecurityTokenReference
        SecurityTokenReference str = new SecurityTokenReference(
                "X509-5DFA3742A71FBC99CA17475800568446", // el mismo wsu:Id del BST
                doc
        );
        DOMStructure ks = (DOMStructure) str.getElement();
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(ks));

        // 5) Montamos el XMLSignature
        XMLSignature signature = fac.newXMLSignature(si, ki, null,
                "SIG-5DFA3742A71FBC99CA17475800568446", // Id del Signature
                null
        );

        // 6) Firmamos “en DOM” y lo insertamos justo bajo <wsse:Security>
        DOMSignContext dsc = new DOMSignContext(
                privateKey,                          // tu clave privada
                security                             // el elemento <wsse:Security>
        );
        // Indicamos namespacePrefix para ds
        dsc.setDefaultNamespacePrefix("ds");

        // Ejecutamos la firma: añade internamente el <ds:Signature>
        signature.sign(dsc);
    }

    @Override
    public Set<QName> getHeaders() {
        // Declarar que este handler procesa wsse:Security
        return Collections.singleton(
                new QName(WSSE_NS, "Security", "wsse")
        );
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {}

    // Clase auxiliar para crear SecurityTokenReference como KeyInfo
    static class SecurityTokenReference {
        private final String referenceURI;
        private final Document doc;

        public SecurityTokenReference(String referenceURI, Document doc) {
            this.referenceURI = referenceURI;
            this.doc = doc;
        }

        public javax.xml.crypto.XMLStructure getElement() {
            return new javax.xml.crypto.dom.DOMStructure(createElement());
        }

        private org.w3c.dom.Element createElement() {
            org.w3c.dom.Element str = doc.createElementNS(
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                    "wsse:SecurityTokenReference");

            org.w3c.dom.Element reference = doc.createElementNS(
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                    "wsse:Reference");
            reference.setAttribute("URI", "#" + referenceURI);
            reference.setAttribute("ValueType",
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3");

            str.appendChild(reference);
            return str;
        }
    }
}