package com.mentenova.entity;
import jakarta.persistence.*;
import java.time.LocalTime;
@Entity
@Table(name="disponibilidad")
public class Disponibilidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_disponibilidad")
    private Long id;
    @ManyToOne(optional=false)
    @JoinColumn(name="id_psicologo", nullable=false)
    private Psicologo psicologo;
    @Column(name="dia_semana",nullable=false)
    private Short diaSemana;
    @Column(name="hora_inicio",nullable=false)
    private LocalTime horaInicio;
    @Column(name="hora_fin",nullable=false)
    private LocalTime horaFin;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=12)
    private Modalidad modalidad;
    @Column(nullable=false)
    private Boolean activo = true;
    public Long getId() { return id; }
    public void setId(Long valor) { this.id=valor; }
    public Psicologo getPsicologo() { return psicologo; }
    public void setPsicologo(Psicologo valor) { this.psicologo=valor; }
    public Short getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Short valor) { this.diaSemana=valor; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime valor) { this.horaInicio=valor; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime valor) { this.horaFin=valor; }
    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad valor) { this.modalidad=valor; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean valor) { this.activo=valor; }
}
