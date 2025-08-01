package org.ngarcia.serviceEFactura.services;

import org.apache.xml.security.Init;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.ngarcia.serviceEFactura.config.AppConfig;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SignXML {

   // URI oficial de WSU-Utility
   private static final String WSU_NS =
           "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

   static {
      Init.init();
      // Registrar el namespace ds automáticamente
       try {
           ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, "ds");
       } catch (XMLSecurityException e) {
          throw new RuntimeException("Error configuring XML Security", e);
       }
   }

   public static void sign(Element elementToSign, KeyStore ks, X509Certificate cert, String alias) {

      try {
         // 1) Declarar namespace WSU y asignar wsu:Id único
         //String id = "CFE-" + UUID.randomUUID();
         //cfeElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:wsu", WSU_NS);
         //cfeElement.setAttributeNS(WSU_NS, "wsu:Id", id);

         // 2) Obtener contraseña desde configuración
         String keystorePass = AppConfig.getKeystorePassword();
         PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePass.toCharArray());

         // 3) Crear XMLSignature con RSA-SHA256 y c14n exclusiva
         XMLSignature sig = new XMLSignature(elementToSign.getOwnerDocument(),"",
                 XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256, Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

         // 4) Transforms: primero enveloped, luego c14n (con o sin comentarios)
         Transforms transforms = new Transforms(elementToSign.getOwnerDocument());
         transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);

         // 5) Añadir referencia: URI="#id" con SHA-256
         //sig.addDocument("#" + id, transforms, "http://www.w3.org/2001/04/xmlenc#sha256");
         sig.addDocument("", transforms, "http://www.w3.org/2001/04/xmlenc#sha256");

         // 6) Construir KeyInfo con certificado
         KeyInfo ki = sig.getKeyInfo();
         if (ki == null) ki = new KeyInfo(elementToSign.getOwnerDocument());

         // añadir clave pública
         ki.addKeyValue(cert.getPublicKey());
         // añadir datos X509
         org.apache.xml.security.keys.content.X509Data x509Data =
                 new org.apache.xml.security.keys.content.X509Data(elementToSign.getOwnerDocument());
         x509Data.addCertificate(cert);
         ki.add(x509Data);

         System.out.println("ABC"+elementToSign.getParentNode().getNodeType());
         // 7) Insertar la firma
         if (elementToSign.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {

            // Elemento raíz: insertar firma como primer hijo
            Node firstChild = elementToSign.getFirstChild();
            if (firstChild != null) {
               elementToSign.insertBefore(sig.getElement(), firstChild);
            } else {
               elementToSign.appendChild(sig.getElement());
            }
         } else {
            // Elemento no raíz: insertar como hermano siguiente
            Node parent = elementToSign.getParentNode();
            Node next = elementToSign.getNextSibling();
            parent.insertBefore(sig.getElement(), next);
         }

         // 8) Firmar
         sig.sign(privateKey);

         // --- depuración: imprimir la firma en consola ---
         //TransformerFactory tf = TransformerFactory.newInstance();
         //Transformer transformer = tf.newTransformer();
         //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
         //StringWriter sw = new StringWriter();
         //transformer.transform(new DOMSource(sig.getElement()), new StreamResult(sw));
         //System.out.println(">>> ELEMENTO Signature:\n" + sw.toString());

      } catch (Exception e) {
         System.out.println("Error en SingXML: " + e.getMessage());;
      }
   }
}