# Contrato de disponibilidad para el módulo de citas

## Consulta

```http
GET /mentenova/api/disponibilidad?psicologoId=1&fecha=2026-11-02&modalidad=PRESENCIAL
```

Requiere sesión de administrador, recepción o psicólogo. Un psicólogo solo puede consultar su propio identificador. `psicologoId=1` es ilustrativo: reemplázalo por un ID existente.

| Parámetro | Requerido | Valores |
|---|---|---|
| psicologoId | Sí | ID del profesional |
| fecha | Sí | Fecha ISO: yyyy-MM-dd |
| modalidad | No | PRESENCIAL o VIRTUAL; por defecto PRESENCIAL |
| especialidadId | No | ID de una especialidad activa asignada al profesional |

Ejemplo de respuesta, verificado sobre datos de prueba:

```json
{
  "psicologoId": 1,
  "fecha": "2026-11-02",
  "modalidad": "PRESENCIAL",
  "intervalos": [
    {"inicio": "09:00:00", "fin": "09:30:00"},
    {"inicio": "11:30:00", "fin": "13:00:00"},
    {"inicio": "14:00:00", "fin": "15:00:00"}
  ]
}
```

Los intervalos representan rangos máximos libres. La consulta no fija una duración de sesión ni crea una cita. Los límites se tratan como `[inicio, fin)`: una cita que acaba a las 10:00 permite otra que empieza a las 10:00.

Una cuenta inactiva, una especialidad no asignada o la ausencia de intervalos válidos devuelve `intervalos: []`. Los parámetros inválidos devuelven 400, un ID inexistente devuelve 404 y los accesos sin permiso devuelven 403. Una sesión ausente se maneja mediante el login existente de la aplicación.

## Revalidación al guardar una cita

El servicio incluye este método:

```java
disponibilidadService.validarParaReserva(
    psicologoId, fecha, horaInicio, horaFin, modalidad
);
```

Tiene propagación `MANDATORY`: debe invocarse desde la transacción del servicio de citas. Bloquea la fila del psicólogo, recalcula la disponibilidad y rechaza un intervalo que ya no esté libre.

El flujo del servicio de citas debe ser:

1. Abrir su operación `@Transactional`.
2. Invocar `validarParaReserva` para el intervalo solicitado.
3. Guardar o reprogramar la cita dentro de esa misma transacción.
4. Confirmar la transacción completa.

Todas las rutas de escritura del módulo de citas que compitan por un profesional deben seguir ese mismo bloqueo y validación. No se debe confirmar la disponibilidad en una transacción separada y guardar la cita después: se liberaría el bloqueo antes de la escritura. El módulo de citas también debe validar paciente, fecha, duración y transiciones de estado según sus propias reglas.

Para reprogramar, el módulo de citas debe tratar la cita original dentro de su transacción para que su intervalo anterior no se descuente a sí mismo al validar una nueva franja superpuesta. Este paquete no altera el flujo de reprogramación del integrante responsable de citas.

## Reglas del cálculo

- Solo se utilizan horarios semanales activos de la modalidad consultada.
- Las habilitaciones agregan rangos para ambas modalidades porque la tabla actual de excepciones no guarda modalidad.
- Los bloqueos se restan después de unir horarios y habilitaciones; siempre tienen prioridad.
- Las citas PENDIENTE y CONFIRMADA se descuentan en cualquier modalidad.
- Las citas CANCELADA y los demás estados no se consideran ocupación activa en este cálculo.
- El resultado se ordena por hora y une los rangos contiguos.

La consulta y la agenda leen las tablas de citas existentes; no incluyen CRUD de citas, pagos, pacientes ni atención administrativa.
