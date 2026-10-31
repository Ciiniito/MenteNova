package com.mentenova.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity
@Table(name="excepcion_disponibilidad")
public class ExcepcionDisponibilidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_excepcion")
    private Long id;
    @ManyToOne(optional=false)
    @JoinColumn(name="id_psicologo",nullable=false)
    private Psicologo psicologo;
    @Column(nullable=false)
    private LocalDate fecha;
    @Column(name="hora_inicio",nullable=false)
    private LocalTime horaInicio;
    @Column(name="hora_fin",nullable=false)
    private LocalTime horaFin;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=15)
    private TipoExcepcion tipo;
    @Column(length=250)
    private String motivo;
    public Long getId() { return id; }
    public void setId(Long valor) { this.id=valor; }
    public Psicologo getPsicologo() { return psicologo; }
    public void setPsicologo(Psicologo valor) { this.psicologo=valor; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate valor) { this.fecha=valor; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime valor) { this.horaInicio=valor; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime valor) { this.horaFin=valor; }
    public TipoExcepcion getTipo() { return tipo; }
    public void setTipo(TipoExcepcion valor) { this.tipo=valor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String valor) { this.motivo=valor; }
}
