package server_checker.config;

/**
 * AppConfig
 * ---------
 * Centraliza toda la configuración de la aplicación.
 * Modificar aquí y solo aquí para cambiar rutas, credenciales o intervalos.
 */
public final class AppConfig {

    // ── Archivos ──────────────────────────────────────────────────────────────
    public static final String RUTA_CARPETA = "C:\\Users\\HP\\OneDrive\\Desktop\\Datos";
    public static final String NOMBRE_DAT   = "turno_caja.dat";
    public static final String NOMBRE_IDX   = "turno_caja.idx";

    // ── Base de datos ─────────────────────────────────────────────────────────
    public static final String DB_URL     = "jdbc:mysql://localhost:3306/arquiproy";
    public static final String DB_USUARIO = "root";
    public static final String DB_CLAVE   = "Rabito2014";

    // Incluye el ID explícitamente para respetar la identidad del archivo
    public static final String SQL_INSERT =
        "INSERT INTO TurnoCaja (id, nombre_cajero, monto_apertura, monto_cierre, fecha, estado) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    // ── Scheduler ─────────────────────────────────────────────────────────────
    /** Intervalo entre ejecuciones del ciclo de sincronización (milisegundos). */
    public static final long INTERVALO_MS = 10_000L;

    // Clase utilitaria: no instanciar
    private AppConfig() {}
}
