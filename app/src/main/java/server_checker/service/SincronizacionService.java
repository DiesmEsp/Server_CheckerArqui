package server_checker.service;

import server_checker.config.AppConfig;
import server_checker.model.TurnoCaja;
import server_checker.repository.ArchivoRepository;
import server_checker.repository.TurnoCajaRepository;
import server_checker.util.Logger;

import java.io.File;
import java.util.Vector;

/**
 * SincronizacionService
 * ---------------------
 * Orquesta el ciclo completo de sincronización:
 *
 *   1. Detectar archivos .dat e .idx en la carpeta configurada.
 *   2. Leer los offsets del .idx.
 *   3. Deserializar los objetos TurnoCaja desde el .dat.
 *   4. Eliminar los archivos fuente.
 *   5. Persistir los objetos en la base de datos.
 *
 * Esta clase NO conoce detalles de I/O ni de SQL; delega en los repositorios.
 */
public class SincronizacionService {

    private final ArchivoRepository    archivoRepo;
    private final TurnoCajaRepository  bdRepo;
    private final File                 carpeta;

    public SincronizacionService(ArchivoRepository archivoRepo,
                                 TurnoCajaRepository bdRepo) {
        this.archivoRepo = archivoRepo;
        this.bdRepo      = bdRepo;
        this.carpeta     = new File(AppConfig.RUTA_CARPETA);
    }

    // ── Ciclo principal ───────────────────────────────────────────────────────

    /**
     * Ejecuta un ciclo completo de sincronización.
     * Si no hay archivos presentes, termina silenciosamente (estado normal).
     */
    public void ejecutarCiclo() {
        Logger.separador();
        Logger.info("Iniciando ciclo de sincronización...");

        File archivoDat = archivoRepo.buscarArchivo(carpeta, AppConfig.NOMBRE_DAT);
        File archivoIdx = archivoRepo.buscarArchivo(carpeta, AppConfig.NOMBRE_IDX);

        if (!archivosPresentes(archivoDat, archivoIdx)) return;

        long[] offsets = leerOffsets(archivoIdx);
        if (offsets == null) return;

        Vector<TurnoCaja> turnos = leerTurnos(archivoDat, offsets);
        if (turnos == null || turnos.isEmpty()) return;

        Logger.info("Registros leídos del archivo: " + turnos.size());

        // Eliminar ANTES de ir a BD: libera el recurso aunque la BD falle
        archivoRepo.eliminarArchivos(archivoDat, archivoIdx);

        bdRepo.insertar(turnos);

        Logger.info("Ciclo de sincronización completado.");
    }

    // ── Privado: validaciones y pasos intermedios ─────────────────────────────

    private boolean archivosPresentes(File dat, File idx) {
        if (dat == null || idx == null) {
            Logger.info("No se encontraron archivos pendientes. En espera...");
            return false;
        }
        Logger.info("Archivos detectados → DAT: " + dat.getName() + " | IDX: " + idx.getName());
        return true;
    }

    private long[] leerOffsets(File archivoIdx) {
        long[] offsets = archivoRepo.leerOffsets(archivoIdx);
        if (offsets == null || offsets.length == 0) {
            Logger.error("El archivo .idx está vacío o es inválido. Abortando ciclo.");
            return null;
        }
        Logger.info("Offsets leídos: " + offsets.length + " registro(s) encontrado(s).");
        return offsets;
    }

    private Vector<TurnoCaja> leerTurnos(File archivoDat, long[] offsets) {
        Vector<TurnoCaja> turnos = archivoRepo.leerTurnos(archivoDat, offsets);
        if (turnos == null) {
            Logger.error("No se pudieron leer los registros del archivo .dat. Abortando ciclo.");
        }
        return turnos;
    }
}
