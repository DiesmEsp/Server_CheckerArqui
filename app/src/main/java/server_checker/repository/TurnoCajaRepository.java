package server_checker.repository;

import server_checker.config.AppConfig;
import server_checker.model.TurnoCaja;
import server_checker.util.Logger;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * TurnoCajaRepository
 * -------------------
 * Capa de acceso a datos para la base de datos MySQL.
 *
 * Responsabilidades:
 *   - Consultar los IDs ya existentes en la BD para evitar duplicados.
 *   - Insertar en lote (batch) solo los registros nuevos.
 *   - Gestionar la transacción: commit o rollback según el resultado.
 */
public class TurnoCajaRepository {

    private static final String SQL_IDS_EXISTENTES =
        "SELECT id FROM TurnoCaja";

    // ── Inserción filtrando duplicados ────────────────────────────────────────

    /**
     * Inserta los turnos cuyo ID no exista aún en la base de datos.
     * Los registros con ID duplicado se omiten con una advertencia en el log.
     *
     * @return Número de registros insertados correctamente.
     */
    public int insertar(Vector<TurnoCaja> turnos) {
        Logger.info("Conectando a la base de datos...");

        try (Connection conexion = abrirConexion()) {

            Set<Integer> idsExistentes = consultarIdsExistentes(conexion);
            Vector<TurnoCaja> turnosNuevos = filtrarDuplicados(turnos, idsExistentes);

            if (turnosNuevos.isEmpty()) {
                Logger.warn("Todos los registros ya existen en la BD. Nada que insertar.");
                return 0;
            }

            return ejecutarBatch(conexion, turnosNuevos);

        } catch (SQLException e) {
            Logger.error("Error al conectar con la base de datos", e);
            return 0;
        }
    }

    // ── Privado: conexión ─────────────────────────────────────────────────────

    private Connection abrirConexion() throws SQLException {
        return DriverManager.getConnection(
            AppConfig.DB_URL,
            AppConfig.DB_USUARIO,
            AppConfig.DB_CLAVE
        );
    }

    // ── Privado: consulta de IDs existentes ───────────────────────────────────

    /**
     * Trae todos los IDs actuales de la tabla TurnoCaja en un Set
     * para hacer la búsqueda de duplicados en O(1).
     */
    private Set<Integer> consultarIdsExistentes(Connection conexion) throws SQLException {
        Set<Integer> ids = new HashSet<>();

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(SQL_IDS_EXISTENTES)) {

            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        }

        Logger.info("IDs ya existentes en BD: " + ids.size());
        return ids;
    }

    // ── Privado: filtrado de duplicados ───────────────────────────────────────

    /**
     * Devuelve un nuevo Vector solo con los turnos cuyo ID
     * no está en el conjunto de IDs existentes.
     */
    private Vector<TurnoCaja> filtrarDuplicados(Vector<TurnoCaja> turnos,
                                                Set<Integer> idsExistentes) {
        Vector<TurnoCaja> nuevos = new Vector<>();

        for (TurnoCaja turno : turnos) {
            if (idsExistentes.contains(turno.getId())) {
                Logger.warn("Omitido por duplicado → ID=" + turno.getId()
                    + " ('" + turno.getNombreCajero() + "') ya existe en la BD.");
            } else {
                nuevos.add(turno);
            }
        }

        Logger.info("Registros nuevos a insertar: " + nuevos.size()
            + " / " + turnos.size() + " leídos.");
        return nuevos;
    }

    // ── Privado: inserción en lote ────────────────────────────────────────────

    /**
     * Inserta los turnos en un único batch transaccional.
     * Hace rollback completo si algo falla.
     */
    private int ejecutarBatch(Connection conexion,
                              Vector<TurnoCaja> turnos) throws SQLException {
        conexion.setAutoCommit(false);

        try (PreparedStatement ps = conexion.prepareStatement(AppConfig.SQL_INSERT)) {

            for (TurnoCaja turno : turnos) {
                mapearParametros(ps, turno);
                ps.addBatch();
            }

            int insertados = contarExitosos(ps.executeBatch());
            conexion.commit();

            Logger.info("Insertados en BD: " + insertados + " / " + turnos.size() + " registros.");
            return insertados;

        } catch (SQLException e) {
            conexion.rollback();
            Logger.error("Error en batch, se hizo rollback", e);
            return 0;
        }
    }

    // ── Privado: mapeo de parámetros ──────────────────────────────────────────

    /**
     * Asigna los campos del TurnoCaja a los parámetros del PreparedStatement.
     * El INSERT usa el ID del archivo para respetar la integridad referencial.
     */
    private void mapearParametros(PreparedStatement ps,
                                  TurnoCaja turno) throws SQLException {
        ps.setInt      (1, turno.getId());
        ps.setString   (2, turno.getNombreCajero());
        ps.setDouble   (3, turno.getMontoApertura());
        ps.setDouble   (4, turno.getMontoCierre());
        ps.setTimestamp(5, new Timestamp(turno.getFecha().getTime()));
        ps.setBoolean  (6, turno.isEstado());
    }

    private int contarExitosos(int[] resultados) {
        int count = 0;
        for (int r : resultados) {
            if (r > 0) count++;
        }
        return count;
    }
}
