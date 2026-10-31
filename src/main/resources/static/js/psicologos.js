/* Mejoras de presentación. Las validaciones y los permisos siguen en el servidor. */
(() => {
    'use strict';
    const normalizar = (texto) => texto.normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();

    const directorio = document.querySelector('[data-directory]');
    if (directorio) {
        const filas = Array.from(directorio.querySelectorAll('[data-professional]'));
        const buscar = directorio.querySelector('[data-search]');
        const botones = Array.from(directorio.querySelectorAll('[data-status]'));
        const vacio = directorio.querySelector('[data-no-results]');
        const resultados = directorio.querySelector('[data-results]');
        const tabla = directorio.querySelector('.psi-table-wrap');
        let estado = 'todos';
        const textos = new Map(filas.map(fila => [fila, normalizar(fila.textContent)]));
        const filtrar = () => {
            const texto = normalizar(buscar.value);
            let cantidad = 0;
            filas.forEach(fila => {
                const mostrar = (estado === 'todos' || fila.dataset.state === estado)
                    && textos.get(fila).includes(texto);
                fila.hidden = !mostrar;
                if (mostrar) cantidad++;
            });
            if (tabla) tabla.hidden = cantidad === 0;
            vacio.hidden = cantidad !== 0 || filas.length === 0;
            resultados.textContent = `Mostrando ${cantidad} de ${filas.length} profesionales`;
        };
        if (filas.length) {
            directorio.querySelector('[data-directory-tools]').hidden = false;
            buscar.addEventListener('input', filtrar);
            botones.forEach(boton => boton.addEventListener('click', () => {
                estado = boton.dataset.status;
                botones.forEach(item => {
                    const seleccionado = item === boton;
                    item.classList.toggle('is-selected', seleccionado);
                    item.setAttribute('aria-pressed', String(seleccionado));
                });
                filtrar();
            }));
            directorio.querySelector('[data-reset]').addEventListener('click', () => {
                buscar.value = '';
                botones[0].click();
                buscar.focus();
            });
            filtrar();
        }
    }

    const formulario = document.querySelector('[data-professional-form]');
    if (formulario) {
        const nombres = formulario.elements.namedItem('nombres');
        const apellidos = formulario.elements.namedItem('apellidos');
        const correo = formulario.elements.namedItem('correo');
        const colegiatura = formulario.elements.namedItem('numeroColegiatura');
        const resumen = () => {
            const nombre = nombres.value.trim();
            const apellido = apellidos.value.trim();
            document.querySelector('[data-preview-name]').textContent =
                [nombre, apellido].filter(Boolean).join(' ') || 'Nuevo profesional';
            document.querySelector('[data-preview-initials]').textContent =
                ((Array.from(nombre)[0] || '') + (Array.from(apellido)[0] || '')).toLocaleUpperCase('es') || 'NP';
            document.querySelector('[data-preview-email]').textContent = correo.value.trim() || 'Su correo aparecerá aquí';
            document.querySelector('[data-preview-license]').textContent = colegiatura.value.trim() || 'Por completar';
        };
        [nombres, apellidos, correo, colegiatura].forEach(campo => campo.addEventListener('input', resumen));
        resumen();
        const estado = formulario.querySelector('[data-account-state]');
        if (estado) {
            const actualizarEstado = () => {
                const activo = estado.value === 'true';
                const etiqueta = document.querySelector('[data-preview-state]');
                etiqueta.classList.toggle('psi-badge--active', activo);
                etiqueta.classList.toggle('psi-badge--inactive', !activo);
                document.querySelector('[data-preview-state-text]').textContent = activo ? 'Activo' : 'Inactivo';
            };
            estado.addEventListener('change', actualizarEstado);
            actualizarEstado();
        }
        const alternar = formulario.querySelector('[data-toggle-password]');
        const password = formulario.elements.namedItem('password');
        alternar.hidden = false;
        alternar.addEventListener('click', () => {
            const visible = password.type === 'password';
            password.type = visible ? 'text' : 'password';
            alternar.setAttribute('aria-pressed', String(visible));
            alternar.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
            alternar.querySelector('span').textContent = visible ? 'Ocultar' : 'Mostrar';
            alternar.querySelector('i').className = visible ? 'bi bi-eye-slash' : 'bi bi-eye';
        });
        formulario.querySelector('[data-error-summary]')?.focus();
    }
})();
