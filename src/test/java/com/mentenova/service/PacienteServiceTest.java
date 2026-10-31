package com.mentenova.service;

import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.paciente.CitaPacienteDTO;
import com.mentenova.dto.paciente.CriterioBusquedaPacientes;
import com.mentenova.dto.paciente.DatosContactoDTO;
import com.mentenova.dto.paciente.PacienteDetalleDTO;
import com.mentenova.dto.paciente.PacienteFilaDTO;
import com.mentenova.dto.paciente.PacienteFormDTO;
import com.mentenova.entity.Paciente;
import com.mentenova.entity.Rol;
import com.mentenova.entity.Usuario;
import com.mentenova.repository.PacienteConsultaRepository;
import com.mentenova.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    private static final UsuarioSesionDTO RECEPCION =
            new UsuarioSesionDTO(2L, "Rosa", "Rosa Huamán Quispe", "RH", "RECEPCIONISTA", "Recepcionista");
    private static final UsuarioSesionDTO PSICOLOGO =
            new UsuarioSesionDTO(3L, "Laura", "Laura Méndez Ríos", "LM", "PSICOLOGO", "Psicólogo");
    private static final UsuarioSesionDTO PACIENTE =
            new UsuarioSesionDTO(7L, "María", "María González Pérez", "MG", "PACIENTE", "Paciente");

    @Mock
    private PacienteRepository pacientes;

    @Mock
    private PacienteConsultaRepository consultas;

    private PacienteService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new PacienteService(pacientes, consultas, reloj);
    }

    @Test
    void registraNormalizandoLosDatosYConElUsuarioDeAuditoria() {
        when(pacientes.saveAndFlush(any(Paciente.class))).thenAnswer(invocacion -> {
            Paciente paciente = invocacion.getArgument(0);
            paciente.setId(30L);
            return paciente;
        });

        Long id = servicio.registrar(RECEPCION, formulario());

        assertThat(id).isEqualTo(30L);
        ArgumentCaptor<Paciente> guardado = ArgumentCaptor.forClass(Paciente.class);
        verify(pacientes).saveAndFlush(guardado.capture());
        Paciente paciente = guardado.getValue();
        assertThat(paciente.getNombres()).isEqualTo("María");
        assertThat(paciente.getTipoDocumento()).isEqualTo("DNI");
        assertThat(paciente.getNumeroDocumento()).isEqualTo("70000099");
        assertThat(paciente.getTelefono()).isEqualTo("+51 987 654 321");
        assertThat(paciente.getCorreo()).isEqualTo("maria@correo.com");
        assertThat(paciente.getDireccion()).isNull();
        assertThat(paciente.getUsuario()).isNull();
        InOrder orden = inOrder(consultas, pacientes);
        orden.verify(consultas).registrarUsuarioAuditoria(2L);
        orden.verify(pacientes).saveAndFlush(any(Paciente.class));
    }

    @Test
    void rechazaUnDocumentoQueYaExiste() {
        when(pacientes.existsByTipoDocumentoAndNumeroDocumento("DNI", "70000099")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, formulario()))
                .isInstanceOfSatisfying(ValidacionPacienteException.class, e -> assertThat(e.getErrores())
                        .containsEntry("numeroDocumento", "Ya existe un paciente registrado con ese documento."));
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void exigeElTipoCuandoSeIngresaUnNumeroDeDocumento() {
        PacienteFormDTO form = formulario();
        form.setTipoDocumento(" ");

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class,
                        e -> assertThat(e.getErrores()).containsKey("tipoDocumento"));
    }

    @Test
    void exigeElNumeroCuandoSeEligeUnTipoDeDocumento() {
        PacienteFormDTO form = formulario();
        form.setTipoDocumento("CE");
        form.setNumeroDocumento(null);

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class,
                        e -> assertThat(e.getErrores()).containsKey("numeroDocumento"));
    }

    @Test
    void validaElFormatoDelDni() {
        PacienteFormDTO form = formulario();
        form.setNumeroDocumento("12345");

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class, e -> assertThat(e.getErrores())
                        .containsEntry("numeroDocumento", "El DNI debe tener 8 dígitos."));
    }

    @Test
    void permiteRegistrarUnPacienteSinDocumento() {
        PacienteFormDTO form = formulario();
        form.setTipoDocumento(null);
        form.setNumeroDocumento(null);
        when(pacientes.saveAndFlush(any(Paciente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.registrar(RECEPCION, form);

        verify(pacientes, never()).existsByTipoDocumentoAndNumeroDocumento(anyString(), anyString());
        verify(pacientes).saveAndFlush(any(Paciente.class));
    }

    @Test
    void rechazaUnaFechaDeNacimientoFutura() {
        PacienteFormDTO form = formulario();
        form.setFechaNacimiento(LocalDate.of(2030, 1, 1));

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class,
                        e -> assertThat(e.getErrores()).containsKey("fechaNacimiento"));
    }

    @Test
    void soloVinculaCuentasConRolPaciente() {
        PacienteFormDTO form = formulario();
        form.setCorreoCuenta("recepcion@demo.mentenova.local");
        when(pacientes.buscarUsuarioPorCorreo("recepcion@demo.mentenova.local"))
                .thenReturn(Optional.of(usuario(4L, "RECEPCIONISTA")));

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class, e -> assertThat(e.getErrores())
                        .containsEntry("correoCuenta", "La cuenta debe tener el rol Paciente."));
    }

    @Test
    void noVinculaUnaCuentaQueYaPerteneceAOtroPaciente() {
        PacienteFormDTO form = formulario();
        form.setCorreoCuenta("paciente@demo.mentenova.local");
        when(pacientes.buscarUsuarioPorCorreo("paciente@demo.mentenova.local"))
                .thenReturn(Optional.of(usuario(9L, "PACIENTE")));
        when(pacientes.findByUsuario_Id(9L)).thenReturn(Optional.of(paciente(5L)));

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class, e -> assertThat(e.getErrores())
                        .containsEntry("correoCuenta", "Esa cuenta ya está vinculada a otro paciente."));
    }

    @Test
    void vinculaLaCuentaDeAccesoDelPaciente() {
        PacienteFormDTO form = formulario();
        form.setCorreoCuenta("  Pedro.Salinas@demo.mentenova.local ");
        Usuario cuenta = usuario(9L, "PACIENTE");
        when(pacientes.buscarUsuarioPorCorreo("pedro.salinas@demo.mentenova.local")).thenReturn(Optional.of(cuenta));
        when(pacientes.findByUsuario_Id(9L)).thenReturn(Optional.empty());
        when(pacientes.saveAndFlush(any(Paciente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.registrar(RECEPCION, form);

        ArgumentCaptor<Paciente> guardado = ArgumentCaptor.forClass(Paciente.class);
        verify(pacientes).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getUsuario()).isSameAs(cuenta);
    }

    @Test
    void alEditarPermiteConservarSuPropioDocumento() {
        Paciente existente = paciente(5L);
        when(pacientes.findById(5L)).thenReturn(Optional.of(existente));
        when(pacientes.existsByTipoDocumentoAndNumeroDocumentoAndIdNot("DNI", "70000099", 5L)).thenReturn(false);
        when(pacientes.saveAndFlush(existente)).thenReturn(existente);

        servicio.actualizar(RECEPCION, 5L, formulario());

        assertThat(existente.getNumeroDocumento()).isEqualTo("70000099");
        verify(pacientes, never()).existsByTipoDocumentoAndNumeroDocumento(anyString(), anyString());
    }

    @Test
    void buscaSinDistinguirTildesNiMayusculas() {
        when(consultas.contar(any())).thenReturn(0L);
        when(consultas.listar(any(), anyInt(), anyInt())).thenReturn(List.of());

        servicio.buscar(RECEPCION, "  MARÍA  González ", "todos", 1);

        ArgumentCaptor<CriterioBusquedaPacientes> criterio = ArgumentCaptor.forClass(CriterioBusquedaPacientes.class);
        verify(consultas).contar(criterio.capture());
        assertThat(criterio.getValue().palabras()).containsExactly("maria", "gonzalez");
        assertThat(criterio.getValue().documento()).isNull();
        assertThat(criterio.getValue().activo()).isNull();
        assertThat(criterio.getValue().idPsicologo()).isNull();
    }

    @Test
    void buscaPorNumeroDeDocumento() {
        when(consultas.contar(any())).thenReturn(0L);
        when(consultas.listar(any(), anyInt(), anyInt())).thenReturn(List.of());

        servicio.buscar(RECEPCION, "70000001", null, 1);

        ArgumentCaptor<CriterioBusquedaPacientes> criterio = ArgumentCaptor.forClass(CriterioBusquedaPacientes.class);
        verify(consultas).contar(criterio.capture());
        assertThat(criterio.getValue().documento()).isEqualTo("70000001");
        assertThat(criterio.getValue().activo()).isTrue();
    }

    @Test
    void unaPaginaFueraDeRangoMuestraLaUltima() {
        when(consultas.contar(any())).thenReturn(25L);
        when(consultas.listar(any(), eq(10), eq(20))).thenReturn(List.of());

        PaginaDTO<PacienteFilaDTO> pagina = servicio.buscar(RECEPCION, null, null, 9);

        assertThat(pagina.pagina()).isEqualTo(3);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    void elPsicologoSoloBuscaEntreSusPacientesYSinVerDocumentos() {
        when(consultas.idPsicologoDeUsuario(3L)).thenReturn(Optional.of(11L));
        when(consultas.contar(any())).thenReturn(1L);
        when(consultas.listar(any(), anyInt(), anyInt())).thenReturn(List.of(
                PacienteFilaDTO.de(5L, "María", "González Pérez", "DNI", "70000001", "+51 987 654 321", true, 4)));

        PaginaDTO<PacienteFilaDTO> pagina = servicio.buscar(PSICOLOGO, null, null, 1);

        ArgumentCaptor<CriterioBusquedaPacientes> criterio = ArgumentCaptor.forClass(CriterioBusquedaPacientes.class);
        verify(consultas).contar(criterio.capture());
        assertThat(criterio.getValue().idPsicologo()).isEqualTo(11L);
        assertThat(pagina.contenido().get(0).documento()).isNull();
    }

    @Test
    void elPsicologoNoVeLaFichaDeUnPacienteSinCitasConEl() {
        when(consultas.idPsicologoDeUsuario(3L)).thenReturn(Optional.of(11L));
        when(consultas.tieneCitasCon(5L, 11L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.detalle(PSICOLOGO, 5L, null)).isInstanceOf(AccessDeniedException.class);
        verify(pacientes, never()).findById(any());
    }

    @Test
    void elPsicologoVeLaFichaSinDocumentoDireccionNiPagos() {
        CitaPacienteDTO cita = new CitaPacienteDTO(100L, LocalDate.of(2026, 10, 20), LocalTime.of(9, 0),
                LocalTime.of(10, 0), "Laura Méndez Ríos", "PRESENCIAL", "ATENDIDA", "Atendida", "PAGADO");
        when(consultas.idPsicologoDeUsuario(3L)).thenReturn(Optional.of(11L));
        when(consultas.tieneCitasCon(5L, 11L)).thenReturn(true);
        when(pacientes.findById(5L)).thenReturn(Optional.of(paciente(5L)));
        when(consultas.historial(5L, 11L, null)).thenReturn(List.of(cita));
        when(consultas.proximaCita(5L, 11L)).thenReturn(Optional.empty());
        when(consultas.ultimoPsicologo(5L, 11L)).thenReturn(Optional.of("Laura Méndez Ríos"));
        when(consultas.contarCitas(5L, 11L)).thenReturn(1L);

        PacienteDetalleDTO detalle = servicio.detalle(PSICOLOGO, 5L, null);

        assertThat(detalle.documento()).isNull();
        assertThat(detalle.direccion()).isNull();
        assertThat(detalle.fechaNacimiento()).isNull();
        assertThat(detalle.edad()).isEqualTo(29);
        assertThat(detalle.muestraPagos()).isFalse();
        assertThat(detalle.historial().get(0).estadoPago()).isNull();
        assertThat(detalle.psicologoAsignado()).isEqualTo("Laura Méndez Ríos");
        assertThat(detalle.cambios()).isEmpty();
    }

    @Test
    void elPacienteNoAccedeALaBandeja() {
        assertThatThrownBy(() -> servicio.buscar(PACIENTE, null, null, 1)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void elPacienteSoloActualizaSuContacto() {
        Paciente propio = paciente(5L);
        when(pacientes.findByUsuario_Id(7L)).thenReturn(Optional.of(propio));
        when(pacientes.saveAndFlush(propio)).thenReturn(propio);
        DatosContactoDTO contacto = new DatosContactoDTO();
        contacto.setTelefono(" +51 999 888 777 ");
        contacto.setCorreo("NUEVO@Correo.com");

        servicio.actualizarContacto(PACIENTE, contacto);

        assertThat(propio.getTelefono()).isEqualTo("+51 999 888 777");
        assertThat(propio.getCorreo()).isEqualTo("nuevo@correo.com");
        assertThat(propio.getNumeroDocumento()).isEqualTo("70000001");
        verify(consultas).registrarUsuarioAuditoria(7L);
    }

    @Test
    void sinFichaVinculadaElPacienteNoPuedeActualizarDatos() {
        when(pacientes.findByUsuario_Id(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarContacto(PACIENTE, new DatosContactoDTO()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void laExcepcionConservaElOrdenDeLosErrores() {
        PacienteFormDTO form = new PacienteFormDTO();

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionPacienteException.class, e -> assertThat(e.getErrores().keySet())
                        .containsExactly("nombres", "apellidos", "telefono"));
    }

    private PacienteFormDTO formulario() {
        PacienteFormDTO form = new PacienteFormDTO();
        form.setNombres("  María  ");
        form.setApellidos("González Pérez");
        form.setTipoDocumento("dni");
        form.setNumeroDocumento(" 7000 0099 ");
        form.setFechaNacimiento(LocalDate.of(1997, 3, 14));
        form.setTelefono(" +51 987 654 321 ");
        form.setCorreo("  MARIA@Correo.COM ");
        form.setDireccion("   ");
        return form;
    }

    private Paciente paciente(Long id) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setNombres("María");
        paciente.setApellidos("González Pérez");
        paciente.setTipoDocumento("DNI");
        paciente.setNumeroDocumento("70000001");
        paciente.setFechaNacimiento(LocalDate.of(1997, 3, 14));
        paciente.setTelefono("+51 987 654 321");
        paciente.setDireccion("Av. Arequipa 1234");
        paciente.setActivo(true);
        return paciente;
    }

    private Usuario usuario(Long id, String rol) {
        Rol nombreRol = new Rol();
        nombreRol.setNombre(rol);
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(nombreRol);
        return usuario;
    }
}
