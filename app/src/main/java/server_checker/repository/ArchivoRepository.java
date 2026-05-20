package server_checker.repository;

import server_checker.model.TurnoCaja;
import server_checker.util.Logger;

import java.io.*;
import java.util.Date;
import java.util.Vector;

/**
 * ArchivoRepository
 * -----------------
 * Capa de acceso a datos para los archivos binarios .dat e .idx
 * generados por TurnoRepository (com.brenis).
 *
 * Formato .idx → entradas de 12 bytes fijos:
 *     [int  id          → 4 bytes]
 *     [long posicionDat → 8 bytes]
 *
 * Formato .dat → registros de 229 bytes fijos:
 *     [int     id            →   4 bytes]
 *     [char×100 nombreCajero → 200 bytes  (100 chars × 2 bytes/char, padding '\0')]
 *     [double  montoApertura →   8 bytes]
 *     [double  montoCierre   →   8 bytes]
 *     [long    fecha (millis)→   8 bytes]
 *     [boolean estado        →   1 byte ]
 *                               ─────────
 *                               229 bytes
 */
public class ArchivoRepository {

    // ── Constantes del formato binario (deben coincidir con TurnoRepository) ──

    private static final int NOMBRE_MAX_CHARS = 100;       // chars escritos con writeChar()
    private static final int INDEX_ENTRY_SIZE = 12;        // 4 (int) + 8 (long)

    // ── Localización de archivos ──────────────────────────────────────────────

    /**
     * Busca un archivo por nombre exacto dentro de la carpeta.
     *
     * @return El archivo si existe, {@code null} si no.
     */
    public File buscarArchivo(File carpeta, String nombreArchivo) {
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            Logger.error("La carpeta no existe o no es válida: " + carpeta.getAbsolutePath());
            return null;
        }
        File archivo = new File(carpeta, nombreArchivo);
        return archivo.exists() ? archivo : null;
    }

    // ── Lectura del .idx ──────────────────────────────────────────────────────

    /**
     * Lee todas las posiciones físicas del archivo .idx.
     *
     * Cada entrada ocupa 12 bytes: int (id) + long (posición en .dat).
     * Solo nos interesa la posición (long); el id del índice lo descartamos
     * porque el id real está dentro del registro del .dat.
     *
     * @return Arreglo de posiciones físicas en el .dat, o {@code null} si hay error.
     */
    public long[] leerOffsets(File archivoIdx) {
        int totalEntradas = (int) (archivoIdx.length() / INDEX_ENTRY_SIZE);

        try (RandomAccessFile raf = new RandomAccessFile(archivoIdx, "r")) {

            long[] offsets = new long[totalEntradas];
            for (int i = 0; i < totalEntradas; i++) {
                raf.readInt();              // leer y descartar el id del índice (4 bytes)
                offsets[i] = raf.readLong(); // posición física en el .dat  (8 bytes)
            }
            return offsets;

        } catch (IOException e) {
            Logger.error("Error leyendo el archivo .idx", e);
            return null;
        }
    }

    // ── Lectura del .dat ──────────────────────────────────────────────────────

    /**
     * Deserializa todos los objetos TurnoCaja del archivo .dat
     * saltando a cada posición indicada por el arreglo de offsets.
     *
     * @return Vector con los turnos leídos, o {@code null} si hay error de I/O.
     */
    public Vector<TurnoCaja> leerTurnos(File archivoDat, long[] offsets) {
        Vector<TurnoCaja> turnos = new Vector<>();

        try (RandomAccessFile raf = new RandomAccessFile(archivoDat, "r")) {

            for (long offset : offsets) {
                raf.seek(offset);
                TurnoCaja turno = deserializarTurno(raf);
                turnos.add(turno);
                Logger.info("  Leído → " + turno);
            }

        } catch (IOException e) {
            Logger.error("Error leyendo el archivo .dat", e);
            return null;
        }

        return turnos;
    }

    // ── Eliminación de archivos ───────────────────────────────────────────────

    /**
     * Elimina todos los archivos recibidos.
     * Advierte si alguno no se puede borrar, pero no lanza excepción.
     */
    public void eliminarArchivos(File... archivos) {
        for (File archivo : archivos) {
            if (archivo.delete()) {
                Logger.info("Archivo eliminado: " + archivo.getName());
            } else {
                Logger.warn("No se pudo eliminar: " + archivo.getName());
            }
        }
    }

    // ── Privado: deserialización de un registro de 229 bytes ─────────────────

    /**
     * Lee un único registro desde la posición actual del RandomAccessFile.
     *
     * Orden y tamaños deben coincidir EXACTAMENTE con escribirRegistro()
     * de TurnoRepository:
     *
     *   int         id            →   4 bytes  (readInt)
     *   char×100    nombreCajero  → 200 bytes  (readChar × 100, ignorar '\0')
     *   double      montoApertura →   8 bytes  (readDouble)
     *   double      montoCierre   →   8 bytes  (readDouble)
     *   long        fecha         →   8 bytes  (readLong)
     *   boolean     estado        →   1 byte   (readBoolean)
     */
    private TurnoCaja deserializarTurno(RandomAccessFile raf) throws IOException {

        // Campo 1: id → 4 bytes
        int id = raf.readInt();

        // Campo 2: nombreCajero → 200 bytes (100 chars × 2 bytes/char)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NOMBRE_MAX_CHARS; i++) {
            char c = raf.readChar();  // readChar lee 2 bytes
            if (c != '\0') sb.append(c);
        }
        String nombreCajero = sb.toString();

        // Campo 3: montoApertura → 8 bytes
        double montoApertura = raf.readDouble();

        // Campo 4: montoCierre → 8 bytes
        double montoCierre = raf.readDouble();

        // Campo 5: fecha → 8 bytes (millisegundos desde epoch)
        Date fecha = new Date(raf.readLong());

        // Campo 6: estado → 1 byte
        boolean estado = raf.readBoolean();

        return new TurnoCaja(id, nombreCajero, montoApertura, montoCierre, fecha, estado);
    }
}
