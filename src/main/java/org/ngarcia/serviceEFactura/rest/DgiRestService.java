package org.ngarcia.serviceEFactura.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.ngarcia.serviceEFactura.services.DGIService; // Servicio reestructurado

@Path("/dgi")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON) // Ahora acepta JSON como entrada
public class DgiRestService {

    @Inject
    private DGIService dgiService; // Inyección de dependencia

    @POST
    @Path("/enviar-cfe")
    public Response enviarCFE(XmlRequest request) { // Usamos un DTO para la entrada
        try {
            String signedXML = dgiService.signAndSendToDGI(request.getUnsignedXml());
            return Response.ok()
                    .entity(new DgiResponse("success", signedXML))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new DgiResponse("error", e.getMessage()))
                    .build();
        }
    }

    // DgiRestService.java - Agregar este método
    @POST
    @Path("/enviar-reporte")
    public Response enviarReporte(XmlRequest request) {
        try {
            String signedXML = dgiService.signAndSendReportToDGI(request.getUnsignedXml());
            return Response.ok()
                    .entity(new DgiResponse("success", signedXML))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new DgiResponse("error", e.getMessage()))
                    .build();
        }
    }

    // DTO para entrada
    public static class XmlRequest {
        private String unsignedXml;

        public String getUnsignedXml() {
            return unsignedXml;
        }

        public void setUnsignedXml(String unsignedXml) {
            this.unsignedXml = unsignedXml;
        }
    }

    // DTO para salida
    public static class DgiResponse {
        private String status;
        private String data;

        public DgiResponse(String status, String data) {
            this.status = status;
            this.data = data;
        }

        // Getters y setters
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}