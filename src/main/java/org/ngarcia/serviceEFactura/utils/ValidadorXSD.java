package org.ngarcia.serviceEFactura.utils;

import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.validation.Validator;
import java.net.URL;

public class ValidadorXSD {
   public void validarCFE(Document cfe) throws Exception {
      URL xsd = getClass().getResource("/xsd/CFE_v1.1.xsd"); // XSD oficial DGI
      SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = sf.newSchema(xsd);

      Validator validator = schema.newValidator();
      validator.validate(new DOMSource(cfe)); // Lanza excepción si no es válido
   }
}
