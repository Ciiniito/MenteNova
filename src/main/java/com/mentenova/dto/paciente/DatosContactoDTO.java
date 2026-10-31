package com.mentenova.dto.paciente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DatosContactoDTO {

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^$|^[0-9+()\\s-]{6,20}$",
            message = "Ingrese un teléfono válido (números, espacios, +, - y paréntesis)")
    private String telefono;

    @Email(message = "Ingrese un correo válido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    private String correo;

    @Size(max = 250, message = "La dirección no puede exceder 250 caracteres")
    private String direccion;

    public static DatosContactoDTO de(PacienteDetalleDTO paciente) {
        DatosContactoDTO datos = new DatosContactoDTO();
        datos.setTelefono(paciente.telefono());
        datos.setCorreo(paciente.correo());
        datos.setDireccion(paciente.direccion());
        return datos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
