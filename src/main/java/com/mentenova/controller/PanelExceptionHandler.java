package com.mentenova.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = {
        DashboardController.class, ReporteController.class, PacienteController.class, MisDatosController.class})
public class PanelExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PanelExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String errorDeDatos(DataAccessException ex) {
        log.error("Error al consultar datos para el dashboard o los reportes", ex);
        return "error-datos";
    }
}
