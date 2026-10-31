package com.mentenova.dto.paciente;

import java.util.List;

public record CriterioBusquedaPacientes(
        List<String> palabras,
        String documento,
        String telefono,
        Boolean activo,
        Long idPsicologo) {
}
