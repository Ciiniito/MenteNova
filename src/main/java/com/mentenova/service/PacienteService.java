package com.mentenova.service;

import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.paciente.CitaPacienteDTO;
import com.mentenova.dto.paciente.CriterioBusquedaPacientes;
import com.mentenova.dto.paciente.DatosContactoDTO;
import com.mentenova.dto.paciente.FormatoPaciente;
import com.mentenova.dto.paciente.PacienteDetalleDTO;
import com.mentenova.dto.paciente.PacienteFilaDTO;
import com.mentenova.dto.paciente.PacienteFormDTO;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.entity.Paciente;
import com.mentenova.entity.Usuario;
import com.mentenova.repository.PacienteConsultaRepository;
import com.mentenova.repository.PacienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class PacienteService {

    public static final int TAMANO_PAGINA = 10;
    public static final String ESTADO_ACTIVOS = "ACTIVOS";
    public static final String ESTADO_INACTIVOS = "INACTIVOS";
    public static final String ESTADO_TODOS = "TODOS";

    private static final List<OpcionDTO> TIPOS_DOCUMENTO = List.of(
            new OpcionDTO("DNI", "DNI"),
            new OpcionDTO("CE", "Carné de extranjería"),
            new OpcionDTO("PASAPORTE", "Pasaporte"));
    private static final Map<String, Pattern> FORMATOS_DOCUMENTO = Map.of(
            "DNI", Pattern.compile("\\d{8}"),
            "CE", Pattern.compile("[A-Z0-9]{6,12}"),
            "PASAPORTE", Pattern.compile("[A-Z0-9]{6,12}"));
    private static final LocalDate FECHA_NACIMIENTO_MINIMA = LocalDate.of(1900, 1, 1);
    private static final int MAXIMO_PALABRAS = 5;
    private static final int CAMBIOS_VISIBLES = 10;

    private final PacienteRepository pacientes;
    private final PacienteConsultaRepository consultas;
    private final Clock reloj;

    public PacienteService(PacienteRepository pacientes, PacienteConsultaRepository consultas, Clock reloj) {
        this.pacientes = pacientes;
        this.consultas = consultas;
        this.reloj = reloj;
    }

    public List<OpcionDTO> tiposDocumento() {
        return TIPOS_DOCUMENTO;
    }

    public static String normalizarEstado(String estado) {
        if (estado == null) {
            return ESTADO_ACTIVOS;
        }
        String valor = estado.trim().toUpperCase(Locale.ROOT);
        return List.of(ESTADO_ACTIVOS, ESTADO_INACTIVOS, ESTADO_TODOS).contains(valor) ? valor : ESTADO_ACTIVOS;
    }

    public PaginaDTO<PacienteFilaDTO> buscar(UsuarioSesionDTO usuario, String texto, String estado, int pagina) {
        Long idPsicologo = alcancePsicologo(usuario);
        CriterioBusquedaPacientes criterio = criterio(texto, normalizarEstado(estado), idPsicologo);
        long total = consultas.contar(criterio);
        int totalPaginas = (int) Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA);
        int paginaActual = Math.min(Math.max(1, pagina), totalPaginas);
        List<PacienteFilaDTO> filas = consultas.listar(criterio, TAMANO_PAGINA, (paginaActual - 1) * TAMANO_PAGINA);
        if (idPsicologo != null) {
            filas = filas.stream().map(PacienteFilaDTO::sinDocumento).toList();
        }
        return PaginaDTO.de(filas, paginaActual, TAMANO_PAGINA, total);
    }

    public PacienteDetalleDTO detalle(UsuarioSesionDTO usuario, Long idPaciente, Integer limiteHistorial) {
        Long idPsicologo = alcancePsicologo(usuario);
        validarAccesoPsicologo(idPsicologo, idPaciente);
        boolean administrativo = idPsicologo == null;
        return construirDetalle(buscarPaciente(idPaciente), idPsicologo, administrativo, administrativo,
                limiteHistorial, usuario.esAdministrador());
    }

    public List<CitaPacienteDTO> historial(UsuarioSesionDTO usuario, Long idPaciente) {
        Long idPsicologo = alcancePsicologo(usuario);
        validarAccesoPsicologo(idPsicologo, idPaciente);
        buscarPaciente(idPaciente);
        List<CitaPacienteDTO> citas = consultas.historial(idPaciente, idPsicologo, null);
        return idPsicologo == null ? citas : citas.stream().map(CitaPacienteDTO::sinPago).toList();
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public PacienteFormDTO formulario(Long idPaciente) {
        Paciente paciente = buscarPaciente(idPaciente);
        PacienteFormDTO form = new PacienteFormDTO();
        form.setNombres(paciente.getNombres());
        form.setApellidos(paciente.getApellidos());
        form.setTipoDocumento(paciente.getTipoDocumento());
        form.setNumeroDocumento(paciente.getNumeroDocumento());
        form.setFechaNacimiento(paciente.getFechaNacimiento());
        form.setTelefono(paciente.getTelefono());
        form.setCorreo(paciente.getCorreo());
        form.setDireccion(paciente.getDireccion());
        form.setCorreoCuenta(paciente.getUsuario() == null ? null : paciente.getUsuario().getCorreo());
        return form;
    }

    public Optional<PacienteDetalleDTO> misDatos(UsuarioSesionDTO usuario) {
        return pacientes.findByUsuario_Id(usuario.idUsuario())
                .map(paciente -> construirDetalle(paciente, null, true, true, null, false));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public Long registrar(UsuarioSesionDTO usuario, PacienteFormDTO form) {
        Paciente paciente = new Paciente();
        aplicarFormulario(form, paciente, null);
        return guardar(usuario, paciente).getId();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public void actualizar(UsuarioSesionDTO usuario, Long idPaciente, PacienteFormDTO form) {
        Paciente paciente = buscarPaciente(idPaciente);
        aplicarFormulario(form, paciente, idPaciente);
        guardar(usuario, paciente);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public void cambiarEstado(UsuarioSesionDTO usuario, Long idPaciente, boolean activo) {
        Paciente paciente = buscarPaciente(idPaciente);
        paciente.setActivo(activo);
        guardar(usuario, paciente);
    }

    @Transactional
    @PreAuthorize("hasRole('PACIENTE')")
    public void actualizarContacto(UsuarioSesionDTO usuario, DatosContactoDTO datos) {
        Paciente paciente = pacientes.findByUsuario_Id(usuario.idUsuario())
                .orElseThrow(() -> new AccessDeniedException("Tu cuenta no está vinculada a una ficha de paciente."));
        String telefono = limpiar(datos.getTelefono());
        if (telefono == null) {
            throw new ValidacionPacienteException(Map.of("telefono", "El teléfono es obligatorio."));
        }
        paciente.setTelefono(telefono);
        paciente.setCorreo(minusculas(limpiar(datos.getCorreo())));
        paciente.setDireccion(limpiar(datos.getDireccion()));
        guardar(usuario, paciente);
    }

    private PacienteDetalleDTO construirDetalle(Paciente paciente, Long idPsicologo, boolean vistaAdministrativa,
                                                boolean muestraPagos, Integer limiteHistorial, boolean conCambios) {
        Long id = paciente.getId();
        List<CitaPacienteDTO> historial = consultas.historial(id, idPsicologo, limiteHistorial);
        CitaPacienteDTO proxima = consultas.proximaCita(id, idPsicologo).orElse(null);
        if (!muestraPagos) {
            historial = historial.stream().map(CitaPacienteDTO::sinPago).toList();
            proxima = proxima == null ? null : proxima.sinPago();
        }
        String psicologoAsignado = proxima != null
                ? proxima.psicologo()
                : consultas.ultimoPsicologo(id, idPsicologo).orElse(null);
        Integer edad = paciente.getFechaNacimiento() == null
                ? null
                : Period.between(paciente.getFechaNacimiento(), LocalDate.now(reloj)).getYears();
        Usuario cuenta = paciente.getUsuario();
        return new PacienteDetalleDTO(
                id,
                FormatoPaciente.nombreCompleto(paciente.getNombres(), paciente.getApellidos()),
                FormatoPaciente.iniciales(paciente.getNombres(), paciente.getApellidos()),
                paciente.getNombres(),
                paciente.getApellidos(),
                vistaAdministrativa ? FormatoPaciente.documento(paciente.getTipoDocumento(), paciente.getNumeroDocumento()) : null,
                vistaAdministrativa ? paciente.getFechaNacimiento() : null,
                edad,
                paciente.getTelefono(),
                paciente.getCorreo(),
                vistaAdministrativa ? paciente.getDireccion() : null,
                Boolean.TRUE.equals(paciente.getActivo()),
                paciente.getFechaCreacion(),
                vistaAdministrativa && cuenta != null ? cuenta.getCorreo() : null,
                consultas.contarCitas(id, idPsicologo),
                proxima,
                psicologoAsignado,
                historial,
                conCambios ? consultas.cambios(id, CAMBIOS_VISIBLES) : List.of(),
                muestraPagos);
    }

    private void aplicarFormulario(PacienteFormDTO form, Paciente paciente, Long idPaciente) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombres = limpiar(form.getNombres());
        String apellidos = limpiar(form.getApellidos());
        String telefono = limpiar(form.getTelefono());
        String tipoDocumento = mayusculas(limpiar(form.getTipoDocumento()));
        String numeroDocumento = mayusculas(sinEspacios(form.getNumeroDocumento()));
        LocalDate fechaNacimiento = form.getFechaNacimiento();
        String correoCuenta = minusculas(limpiar(form.getCorreoCuenta()));

        if (nombres == null) {
            errores.put("nombres", "Los nombres son obligatorios.");
        }
        if (apellidos == null) {
            errores.put("apellidos", "Los apellidos son obligatorios.");
        }
        if (telefono == null) {
            errores.put("telefono", "El teléfono es obligatorio.");
        }
        validarDocumento(tipoDocumento, numeroDocumento, idPaciente, errores);
        if (fechaNacimiento != null && (fechaNacimiento.isBefore(FECHA_NACIMIENTO_MINIMA)
                || !fechaNacimiento.isBefore(LocalDate.now(reloj)))) {
            errores.put("fechaNacimiento", "Ingresa una fecha de nacimiento válida y anterior a hoy.");
        }
        Usuario cuenta = correoCuenta == null ? null : validarCuenta(correoCuenta, idPaciente, errores);
        if (!errores.isEmpty()) {
            throw new ValidacionPacienteException(errores);
        }

        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setTipoDocumento(tipoDocumento);
        paciente.setNumeroDocumento(numeroDocumento);
        paciente.setFechaNacimiento(fechaNacimiento);
        paciente.setTelefono(telefono);
        paciente.setCorreo(minusculas(limpiar(form.getCorreo())));
        paciente.setDireccion(limpiar(form.getDireccion()));
        paciente.setUsuario(cuenta);
    }

    private void validarDocumento(String tipo, String numero, Long idPaciente, Map<String, String> errores) {
        if (tipo == null && numero == null) {
            return;
        }
        if (tipo == null) {
            errores.put("tipoDocumento", "Selecciona el tipo de documento.");
            return;
        }
        if (!FORMATOS_DOCUMENTO.containsKey(tipo)) {
            errores.put("tipoDocumento", "Tipo de documento no válido.");
            return;
        }
        if (numero == null) {
            errores.put("numeroDocumento", "Ingresa el número de documento.");
            return;
        }
        if (!FORMATOS_DOCUMENTO.get(tipo).matcher(numero).matches()) {
            errores.put("numeroDocumento", "DNI".equals(tipo)
                    ? "El DNI debe tener 8 dígitos."
                    : "Usa de 6 a 12 letras o números, sin espacios ni guiones.");
            return;
        }
        boolean duplicado = idPaciente == null
                ? pacientes.existsByTipoDocumentoAndNumeroDocumento(tipo, numero)
                : pacientes.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(tipo, numero, idPaciente);
        if (duplicado) {
            errores.put("numeroDocumento", "Ya existe un paciente registrado con ese documento.");
        }
    }

    private Usuario validarCuenta(String correo, Long idPaciente, Map<String, String> errores) {
        Optional<Usuario> encontrada = pacientes.buscarUsuarioPorCorreo(correo);
        if (encontrada.isEmpty()) {
            errores.put("correoCuenta", "No existe una cuenta de acceso con ese correo.");
            return null;
        }
        Usuario cuenta = encontrada.get();
        if (!UsuarioSesionDTO.PACIENTE.equals(cuenta.getRol().getNombre())) {
            errores.put("correoCuenta", "La cuenta debe tener el rol Paciente.");
            return null;
        }
        boolean vinculadaAOtro = pacientes.findByUsuario_Id(cuenta.getId())
                .map(Paciente::getId)
                .filter(id -> !id.equals(idPaciente))
                .isPresent();
        if (vinculadaAOtro) {
            errores.put("correoCuenta", "Esa cuenta ya está vinculada a otro paciente.");
            return null;
        }
        return cuenta;
    }

    private Paciente guardar(UsuarioSesionDTO usuario, Paciente paciente) {
        consultas.registrarUsuarioAuditoria(usuario.idUsuario());
        try {
            return pacientes.saveAndFlush(paciente);
        } catch (DataIntegrityViolationException e) {
            String detalle = String.valueOf(e.getMostSpecificCause().getMessage());
            if (detalle.contains("uq_paciente_documento")) {
                throw new ValidacionPacienteException(
                        Map.of("numeroDocumento", "Ya existe un paciente registrado con ese documento."));
            }
            if (detalle.contains("id_usuario")) {
                throw new ValidacionPacienteException(
                        Map.of("correoCuenta", "Esa cuenta ya está vinculada a otro paciente."));
            }
            throw new ValidacionPacienteException(Map.of(ValidacionPacienteException.GENERAL,
                    "No se pudo guardar el paciente. Revisa los datos e intenta nuevamente."));
        }
    }

    private Long alcancePsicologo(UsuarioSesionDTO usuario) {
        if (usuario.puedeGestionarPacientes()) {
            return null;
        }
        if (UsuarioSesionDTO.PSICOLOGO.equals(usuario.rol())) {
            return consultas.idPsicologoDeUsuario(usuario.idUsuario())
                    .orElseThrow(() -> new AccessDeniedException("Tu usuario no tiene ficha de psicólogo."));
        }
        throw new AccessDeniedException("No tienes acceso a la gestión de pacientes.");
    }

    private void validarAccesoPsicologo(Long idPsicologo, Long idPaciente) {
        if (idPsicologo != null && !consultas.tieneCitasCon(idPaciente, idPsicologo)) {
            throw new AccessDeniedException("Solo puedes consultar pacientes que tienen citas contigo.");
        }
    }

    private Paciente buscarPaciente(Long idPaciente) {
        return pacientes.findById(idPaciente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El paciente no existe."));
    }

    private CriterioBusquedaPacientes criterio(String texto, String estado, Long idPsicologo) {
        Boolean activo = switch (estado) {
            case ESTADO_INACTIVOS -> false;
            case ESTADO_TODOS -> null;
            default -> true;
        };
        String normalizado = normalizarTexto(texto);
        if (normalizado.isEmpty()) {
            return new CriterioBusquedaPacientes(List.of(), null, null, activo, idPsicologo);
        }
        List<String> palabras = Arrays.stream(normalizado.split("\\s+"))
                .filter(palabra -> !palabra.isBlank())
                .limit(MAXIMO_PALABRAS)
                .toList();
        String compacto = normalizado.replaceAll("\\s+", "");
        String documento = compacto.matches("[a-z0-9]{3,}") && compacto.matches(".*\\d.*") ? compacto : null;
        String digitos = normalizado.replaceAll("\\D", "");
        String telefono = digitos.length() >= 4 ? digitos : null;
        return new CriterioBusquedaPacientes(palabras, documento, telefono, activo, idPsicologo);
    }

    static String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }

    private static String sinEspacios(String texto) {
        if (texto == null) {
            return null;
        }
        String compacto = texto.replaceAll("\\s+", "");
        return compacto.isEmpty() ? null : compacto;
    }

    private static String mayusculas(String texto) {
        return texto == null ? null : texto.toUpperCase(Locale.ROOT);
    }

    private static String minusculas(String texto) {
        return texto == null ? null : texto.toLowerCase(Locale.ROOT);
    }
}
