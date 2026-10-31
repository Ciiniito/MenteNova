package com.mentenova.entity;
import jakarta.persistence.*;
@Entity
@Table(name="especialidad")
public class Especialidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_especialidad")
    private Short id;
    @Column(nullable=false, length=100, unique=true)
    private String nombre;
    @Column(length=250)
    private String descripcion;
    @Column(nullable=false)
    private Boolean activo = true;
    public Short getId() { return id; }
    public void setId(Short valor) { this.id=valor; }
    public String getNombre() { return nombre; }
    public void setNombre(String valor) { this.nombre=valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String valor) { this.descripcion=valor; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean valor) { this.activo=valor; }
}
