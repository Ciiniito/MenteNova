package com.mentenova.dto.paciente;

public record PacienteFilaDTO(
        Long id,
        String nombreCompleto,
        String iniciales,
        String documento,
        String telefono,
        boolean activo,
        long citasRegistradas) {

    public static PacienteFilaDTO de(Long id, String nombres, String apellidos, String tipoDocumento,
                                     String numeroDocumento, String telefono, boolean activo, long citasRegistradas) {
        return new PacienteFilaDTO(
                id,
                FormatoPaciente.nombreCompleto(nombres, apellidos),
                FormatoPaciente.iniciales(nombres, apellidos),
                FormatoPaciente.documento(tipoDocumento, numeroDocumento),
                telefono,
                activo,
                citasRegistradas);
    }

    public PacienteFilaDTO sinDocumento() {
        return new PacienteFilaDTO(id, nombreCompleto, iniciales, null, telefono, activo, citasRegistradas);
    }
}
