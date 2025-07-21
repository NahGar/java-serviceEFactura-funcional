package org.ngarcia.serviceEFactura.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GeneradorQR {

   public byte[] generarQR(String selloDigital) throws IOException, WriterException {
      QRCodeWriter qrWriter = new QRCodeWriter();
      BitMatrix matrix = qrWriter.encode(
              selloDigital,
              BarcodeFormat.QR_CODE,
              200,  // Ancho (mínimo 200px para 22mm a 200dpi)
              200   // Alto
      );

      ByteArrayOutputStream png = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", png);
      return png.toByteArray();
   }

   public String construirSelloDigital(String ruc, String tipoCFE,
                                       String serie, String nroCFE,
                                       String monto, String fecha, String hash) {
      return String.format(
              "https://www.efactura.dgi.gub.uy/consultaQR/cfe?%s,%s,%s,%s,%s,%s,%s",
              ruc, tipoCFE, serie, nroCFE, monto, fecha, hash
      );
   }
}