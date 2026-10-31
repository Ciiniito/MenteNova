package com.mentenova.dto;

import com.mentenova.entity.Usuario;

public record UsuarioSesionDTO(
        Long idUsuario,
        String nombres,
        String nombreCompleto,
        String iniciales,
        String rol,
        String rolEtiqueta) {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String RECEPCIONISTA = "RECEPCIONISTA";
    public static final String PSICOLOGO = "PSICOLOGO";
    public static final String PACIENTE = "PACIENTE";

    public static UsuarioSesionDTO de(Usuario usuario) {
        String rol = usuario.getRol().getNombre();
        return new UsuarioSesionDTO(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getNombres() + " " + usuario.getApellidos(),
                inicial(usuario.getNombres()) + inicial(usuario.getApellidos()),
                rol,
                etiquetaRol(rol));
    }

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }

    public boolean puedeVerReportes() {
        return ADMINISTRADOR.equals(rol) || RECEPCIONISTA.equals(rol);
    }

    public boolean puedeVerIngresos() {
        return esAdministrador();
    }

    public boolean esPersonal() {
        return !PACIENTE.equals(rol);
    }

    public boolean puedeGestionarPacientes() {
        return ADMINISTRADOR.equals(rol) || RECEPCIONISTA.equals(rol);
    }

    public boolean puedeConsultarPacientes() {
        return puedeGestionarPacientes() || PSICOLOGO.equals(rol);
    }

    private static String inicial(String texto) {
        return texto == null || texto.isBlank() ? "" : texto.strip().substring(0, 1).toUpperCase();
    }

    private static String etiquetaRol(String rol) {
        return switch (rol) {
            case ADMINISTRADOR -> "Administrador";
            case RECEPCIONISTA -> "Recepcionista";
            case PSICOLOGO -> "Psicólogo";
            case PACIENTE -> "Paciente";
            default -> rol;
        };
    }
}
