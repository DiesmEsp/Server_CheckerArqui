package server_checker.model;

import java.util.Date;

/**
 * TurnoCaja
 * ---------
 * Representa un turno de caja con todos sus atributos operativos.
 */
public class TurnoCaja {

    private int     id;
    private String  nombreCajero;
    private double  montoApertura;
    private double  montoCierre;
    private Date    fecha;
    private boolean estado;

    public TurnoCaja(int id, String nombreCajero, double montoApertura,
                     double montoCierre, Date fecha, boolean estado) {
        this.id            = id;
        this.nombreCajero  = nombreCajero;
        this.montoApertura = montoApertura;
        this.montoCierre   = montoCierre;
        this.fecha         = fecha;
        this.estado        = estado;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getId()                { return id; }
    public String getNombreCajero()   { return nombreCajero; }
    public double getMontoApertura()  { return montoApertura; }
    public double getMontoCierre()    { return montoCierre; }
    public Date getFecha()            { return fecha; }
    public boolean isEstado()         { return estado; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(int id)                      { this.id = id; }
    public void setNombreCajero(String v)          { this.nombreCajero = v; }
    public void setMontoApertura(double v)         { this.montoApertura = v; }
    public void setMontoCierre(double v)           { this.montoCierre = v; }
    public void setFecha(Date v)                   { this.fecha = v; }
    public void setEstado(boolean v)               { this.estado = v; }

    @Override
    public String toString() {
        return String.format(
            "TurnoCaja{id=%d, cajero='%s', apertura=%.2f, cierre=%.2f, fecha=%s, estado=%b}",
            id, nombreCajero, montoApertura, montoCierre, fecha, estado
        );
    }
}
