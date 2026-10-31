package com.mentenova.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PsicologoForm {
    public interface Creacion {}
    public interface Edicion {}

    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(max = 80, message = "Los nombres admiten hasta 80 caracteres.")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios.")
    @Size(max = 100, message = "Los apellidos admiten hasta 100 caracteres.")
    private String apellidos;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Ingresa un correo válido.")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria.", groups = Creacion.class)
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    private String password;

    @NotBlank(message = "La colegiatura es obligatoria.")
    @Size(max = 30, message = "La colegiatura admite hasta 30 caracteres.")
    private String numeroColegiatura;

    @Size(max = 20, message = "El teléfono admite hasta 20 caracteres.")
    private String telefono;

    @NotNull(message = "Selecciona el estado de la cuenta.", groups = Edicion.class)
    private Boolean activo;

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = limpiar(nombres); }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = limpiar(apellidos); }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = limpiar(correo); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password == null || password.isEmpty() ? null : password; }
    public String getNumeroColegiatura() { return numeroColegiatura; }
    public void setNumeroColegiatura(String numeroColegiatura) { this.numeroColegiatura = limpiar(numeroColegiatura); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = limpiar(telefono); }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.strip();
    }
}
