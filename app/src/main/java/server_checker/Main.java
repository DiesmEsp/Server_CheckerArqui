package server_checker;

import server_checker.repository.ArchivoRepository;
import server_checker.repository.TurnoCajaRepository;
import server_checker.scheduler.SincronizacionScheduler;
import server_checker.service.SincronizacionService;
import server_checker.util.Logger;

/**
 * Main
 * ----
 * Punto de entrada de la aplicación.
 *
 * Responsabilidad única: construir el grafo de dependencias (wiring)
 * e iniciar el scheduler. Nada más.
 *
 * Arquitectura de capas:
 *
 *   Main
 *    └── SincronizacionScheduler   (cuándo ejecutar)
 *         └── SincronizacionService (qué ejecutar / orquestación)
 *              ├── ArchivoRepository   (leer y eliminar .dat / .idx)
 *              └── TurnoCajaRepository (persistir en MySQL)
 */
public class Main {

    public static void main(String[] args) {
        Logger.info("=== Sincronizador TurnoCaja arrancando ===");

        // Construcción del grafo de dependencias
        ArchivoRepository   archivoRepo = new ArchivoRepository();
        TurnoCajaRepository bdRepo      = new TurnoCajaRepository();
        SincronizacionService servicio  = new SincronizacionService(archivoRepo, bdRepo);
        SincronizacionScheduler scheduler = new SincronizacionScheduler(servicio);

        // Arranque
        scheduler.iniciar();
    }
}
