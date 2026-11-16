package com.fintech.backend.model;

import java.util.Objects;

/** Beneficio disponible en el catálogo de recompensas. */
public class Beneficio {

    private final Long idBeneficio;
    private String nombre;
    private String descripcion;
    private int costoPuntos;

    public Beneficio(String nombre, String descripcion, int costoPuntos) {
        idBeneficio = GeneradorIds.siguienteId();
        setNombre(nombre);
        setDescripcion(descripcion);
        setCostoPuntos(costoPuntos);
    }

    public Long getIdBeneficio() { return idBeneficio; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre del beneficio no puede ser null").trim();
        if (this.nombre.isEmpty()) throw new IllegalArgumentException("El nombre del beneficio es obligatorio");
    }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) {
        this.descripcion = Objects.requireNonNull(descripcion, "La descripción no puede ser null").trim();
        if (this.descripcion.isEmpty()) throw new IllegalArgumentException("La descripción es obligatoria");
    }
    public int getCostoPuntos() { return costoPuntos; }
    public void setCostoPuntos(int costoPuntos) {
        if (costoPuntos <= 0) throw new IllegalArgumentException("El costo debe ser mayor que cero");
        this.costoPuntos = costoPuntos;
    }
}
