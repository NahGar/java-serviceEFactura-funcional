package org.ngarcia.serviceEFactura.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Configuración base para JAX-RS (REST).
 * Define la ruta base "/api" para todos los endpoints REST.
 * Reemplaza la necesidad de web.xml en Jakarta EE 9+.
 */
@ApplicationPath("/api")
public class RestConfig extends Application {
    // No es necesario sobrescribir métodos a menos que
    // quieras personalizar el registro de recursos manualmente.
}
