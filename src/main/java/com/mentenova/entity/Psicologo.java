package com.mentenova.entity;

import jakarta.persistence.*;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(name = "psicologo")
public class Psicologo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_psicologo")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "numero_colegiatura", length = 30, nullable = false, unique = true)
    private String numeroColegiatura;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToMany
    @JoinTable(name="psicologo_especialidad",
        joinColumns=@JoinColumn(name="id_psicologo"),
        inverseJoinColumns=@JoinColumn(name="id_especialidad"))
    private Set<Especialidad> especialidades = new LinkedHashSet<>();

    public Set<Especialidad> getEspecialidades() { return especialidades; }
    public void setEspecialidades(Set<Especialidad> valor) { this.especialidades=valor; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNumeroColegiatura() { return numeroColegiatura; }
    public void setNumeroColegiatura(String numeroColegiatura) { this.numeroColegiatura = numeroColegiatura; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
