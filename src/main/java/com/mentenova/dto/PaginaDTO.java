package com.mentenova.dto;

import java.util.List;
import java.util.stream.IntStream;

public record PaginaDTO<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <T> PaginaDTO<T> de(List<T> contenido, int pagina, int tamano, long totalElementos) {
        int totalPaginas = (int) Math.max(1, (totalElementos + tamano - 1) / tamano);
        return new PaginaDTO<>(contenido, pagina, tamano, totalElementos, totalPaginas);
    }

    public long desde() {
        return totalElementos == 0 ? 0 : (long) (pagina - 1) * tamano + 1;
    }

    public long hasta() {
        return Math.min((long) pagina * tamano, totalElementos);
    }

    public boolean tieneAnterior() {
        return pagina > 1;
    }

    public boolean tieneSiguiente() {
        return pagina < totalPaginas;
    }

    public List<Integer> paginas() {
        int inicio = Math.max(1, Math.min(pagina - 2, totalPaginas - 4));
        int fin = Math.min(totalPaginas, inicio + 4);
        return IntStream.rangeClosed(inicio, fin).boxed().toList();
    }
}
