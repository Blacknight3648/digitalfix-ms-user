package df.digitalfix_ms_user.model;

import df.digitalfix_ms_user.model.enums.EntityStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMPRESAS")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "RAZON_SOCIAL", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "RUT", nullable = false, length = 12, unique = true)
    private String rut;

    @Column(name = "TIPO_EMPRESA", length = 50)
    private String tipoEmpresa = "FIELD_SERVICE";

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EntityStatus estado = EntityStatus.ACTIVO;

    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getTipoEmpresa() {
        return tipoEmpresa;
    }

    public void setTipoEmpresa(String tipoEmpresa) {
        this.tipoEmpresa = tipoEmpresa;
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

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
