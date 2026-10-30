package com.mentenova.service;
import com.mentenova.dto.TramoHorario;
import java.util.*;

/** Intervalos [inicio, fin): dos franjas que se tocan no se solapan. */
public final class CalculadorDisponibilidad {
    private CalculadorDisponibilidad() {}
    public static List<TramoHorario> unir(List<TramoHorario> tramos) {
        var ordenados=tramos.stream().sorted(Comparator.comparing(TramoHorario::inicio)).toList();
        var resultado=new ArrayList<TramoHorario>();
        for(var tramo:ordenados) {
            if(resultado.isEmpty() || tramo.inicio().isAfter(resultado.get(resultado.size()-1).fin())) resultado.add(tramo);
            else {
                var ultimo=resultado.remove(resultado.size()-1);
                resultado.add(new TramoHorario(ultimo.inicio(),tramo.fin().isAfter(ultimo.fin())?tramo.fin():ultimo.fin()));
            }
        }
        return List.copyOf(resultado);
    }
    public static List<TramoHorario> restar(List<TramoHorario> base,List<TramoHorario> ocupados) {
        List<TramoHorario> libres=unir(base);
        for(var bloqueo:unir(ocupados)) {
            var siguiente=new ArrayList<TramoHorario>();
            for(var libre:libres) {
                if(!bloqueo.inicio().isBefore(libre.fin()) || !bloqueo.fin().isAfter(libre.inicio())) siguiente.add(libre);
                else {
                    if(bloqueo.inicio().isAfter(libre.inicio())) siguiente.add(new TramoHorario(libre.inicio(),bloqueo.inicio()));
                    if(bloqueo.fin().isBefore(libre.fin())) siguiente.add(new TramoHorario(bloqueo.fin(),libre.fin()));
                }
            }
            libres=siguiente;
        }
        return List.copyOf(libres);
    }
}
