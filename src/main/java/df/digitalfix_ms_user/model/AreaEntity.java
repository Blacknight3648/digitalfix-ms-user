package df.digitalfix_ms_user.model;

import df.digitalfix_ms_user.model.enums.EntityStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Área de trabajo del usuario (Operaciones, Terreno, ...). Antes era texto libre en
// USUARIOS.AREA; como catálogo evita variantes del mismo nombre ("operaciones",
// "Operaciones ") y deja la dependencia en ID_AREA (3FN). Ver migración V4.
@Entity
@Table(name = "AREAS")
public class AreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 80, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EntityStatus estado = EntityStatus.ACTIVO;

    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public EntityStatus getEstado() {
        return estado;
    }

    public void setEstado(EntityStatus estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
