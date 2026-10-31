/* Ayuda a elegir un horario libre. La disponibilidad se vuelve a validar en el servidor al guardar. */
(() => {
    'use strict';
    const DURACION_MINUTOS = 60;

    const formulario = document.getElementById('formCita');
    const panel = document.getElementById('disponibilidad');
    if (!formulario || !panel) return;

    const campo = (nombre) => formulario.elements.namedItem(nombre);
    const psicologo = campo('idPsicologo');
    const fecha = campo('fecha');
    const modalidad = campo('modalidad');
    const inicio = campo('horaInicio');
    const fin = campo('horaFin');

    const hhmm = (hora) => hora.slice(0, 5);
    const sumarMinutos = (hora, minutos) => {
        const [h, m] = hora.split(':').map(Number);
        const total = Math.min(h * 60 + m + minutos, 23 * 60 + 59);
        return String(Math.floor(total / 60)).padStart(2, '0') + ':' + String(total % 60).padStart(2, '0');
    };

    const mensaje = (texto, clase = 'form-text m-0') => {
        const span = document.createElement('span');
        span.className = clase;
        span.textContent = texto;
        panel.replaceChildren(span);
    };

    const mostrar = (intervalos) => {
        if (intervalos.length === 0) {
            mensaje('El psicólogo no tiene horarios libres ese día en esta modalidad.', 'text-danger small');
            return;
        }
        panel.replaceChildren(...intervalos.map(({inicio: desde, fin: hasta}) => {
            const boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'btn btn-sm btn-outline-primary';
            boton.textContent = hhmm(desde) + ' – ' + hhmm(hasta);
            boton.addEventListener('click', () => {
                const propuesta = sumarMinutos(hhmm(desde), DURACION_MINUTOS);
                inicio.value = hhmm(desde);
                fin.value = propuesta < hhmm(hasta) ? propuesta : hhmm(hasta);
            });
            return boton;
        }));
    };

    let consulta = 0;
    const consultar = async () => {
        if (!psicologo.value || !fecha.value) {
            mensaje('Selecciona psicólogo, fecha y modalidad para ver sus horarios libres.');
            return;
        }
        const actual = ++consulta;
        mensaje('Consultando disponibilidad...');
        const url = new URL(formulario.dataset.disponibilidad, window.location.origin);
        url.search = new URLSearchParams({
            psicologoId: psicologo.value, fecha: fecha.value, modalidad: modalidad.value
        }).toString();
        try {
            const respuesta = await fetch(url, {headers: {Accept: 'application/json'}});
            if (!respuesta.ok) throw new Error(String(respuesta.status));
            const datos = await respuesta.json();
            if (actual === consulta) mostrar(datos.intervalos || []);
        } catch (e) {
            if (actual === consulta) mensaje('No se pudo consultar la disponibilidad.', 'text-danger small');
        }
    };

    [psicologo, fecha, modalidad].forEach(control => control.addEventListener('change', consultar));
    consultar();
})();
