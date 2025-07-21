package org.ngarcia.serviceEFactura.utils;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;

public class XMLSigner {

    static {
        Security.removeProvider("BC"); // Elimina si ya existe
        Security.addProvider(new BouncyCastleProvider()); // Registra Bouncy Castle
    }

    //public static String signXML(String unsignedXML, InputStream keystoreStream, String keystorePass) throws Exception {
    public static String signXML(String unsignedXML, KeyStore ks, String keystorePass) throws Exception {

        // Configuración inicial de Apache Santuario
        org.apache.xml.security.Init.init();

        // 1. Parsear XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(unsignedXML.getBytes()));

        // 2. Cargar certificado y clave privada
        //KeyStore ks = KeyStore.getInstance("PKCS12");

        //try {
            //ks.load(keystoreStream, keystorePass.toCharArray());
        //} catch (Exception e) {
        //    System.out.println(e.getMessage());;
        //}
        PrivateKey privateKey = (PrivateKey) ks.getKey(ks.aliases().nextElement(), keystorePass.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(ks.aliases().nextElement());

        // 3. Firmar el XML
        XMLSignature sig = new XMLSignature(doc, null, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        Element root = doc.getDocumentElement();
        root.appendChild(sig.getElement());

        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        sig.addDocument("", transforms,  "http://www.w3.org/2001/04/xmlenc#sha256");

        sig.addKeyInfo(cert);
        sig.sign(privateKey);

        // 4. Convertir a String
        return documentToString(doc);
    }

    private static String documentToString(Document doc) throws Exception {
        javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = tf.newTransformer();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(os));
        return os.toString();
    }
}
