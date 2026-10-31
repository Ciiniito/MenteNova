package com.mentenova.dto;
import jakarta.validation.constraints.*;
public class EspecialidadForm {
    @NotBlank(message="El nombre es obligatorio.") @Size(max=100,message="El nombre admite hasta 100 caracteres.")
    private String nombre;
    @Size(max=250,message="La descripción admite hasta 250 caracteres.")
    private String descripcion;
    @NotNull(message="Selecciona el estado.")
    private Boolean activo = true;
    public String getNombre() { return nombre; }
    public void setNombre(String valor) { this.nombre=valor == null ? null : valor.strip(); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String valor) { this.descripcion=valor == null ? null : valor.strip(); }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean valor) { this.activo=valor; }
}
