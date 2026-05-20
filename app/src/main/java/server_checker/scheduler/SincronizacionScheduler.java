package server_checker.scheduler;

import server_checker.config.AppConfig;
import server_checker.service.SincronizacionService;
import server_checker.util.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SincronizacionScheduler
 * -----------------------
 * Gestiona la ejecución periódica del servicio de sincronización.
 *
 * Usa {@link ScheduledExecutorService} (estándar Java, robusto y thread-safe)
 * en lugar de un bucle con {@code Thread.sleep}, lo que ofrece:
 *   - Ejecución en un hilo dedicado.
 *   - Manejo automático de excepciones sin detener el scheduler.
 *   - Posibilidad de apagado limpio (shutdown hook).
 */
public class SincronizacionScheduler {

    private final SincronizacionService servicio;
    private final ScheduledExecutorService executor;

    public SincronizacionScheduler(SincronizacionService servicio) {
        this.servicio = servicio;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread hilo = new Thread(r, "SincronizacionThread");
            hilo.setDaemon(false); // hilo no-daemon: la JVM espera a que termine
            return hilo;
        });
    }

    // ── Inicio ────────────────────────────────────────────────────────────────

    /**
     * Inicia el scheduler: ejecuta el primer ciclo inmediatamente y luego
     * repite cada {@link AppConfig#INTERVALO_MS} milisegundos.
     * Registra un shutdown hook para apagado limpio con Ctrl+C.
     */
    public void iniciar() {
        long intervaloSegundos = AppConfig.INTERVALO_MS / 1000;
        Logger.info("Scheduler iniciado. Ciclo cada " + intervaloSegundos + " segundos. (Ctrl+C para detener)");

        executor.scheduleAtFixedRate(
            this::ejecutarCicloProtegido,
            0,                       // delay inicial: ejecutar de inmediato
            AppConfig.INTERVALO_MS,  // intervalo entre ejecuciones
            TimeUnit.MILLISECONDS
        );

        registrarShutdownHook();
    }

    // ── Privado ───────────────────────────────────────────────────────────────

    /**
     * Envuelve la ejecución del ciclo en un try-catch para que una excepción
     * inesperada no cancele las ejecuciones futuras del scheduler.
     */
    private void ejecutarCicloProtegido() {
        try {
            servicio.ejecutarCiclo();
        } catch (Exception e) {
            Logger.error("Excepción no controlada en el ciclo de sincronización", e);
        }
    }

    /**
     * Registra un shutdown hook para que el scheduler se detenga limpiamente
     * cuando el proceso recibe la señal de terminación (Ctrl+C, SIGTERM, etc.).
     */
    private void registrarShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Señal de apagado recibida. Deteniendo scheduler...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Logger.info("Scheduler detenido correctamente.");
        }, "ShutdownHook"));
    }
}
