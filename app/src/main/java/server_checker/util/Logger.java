package server_checker.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger
 * ------
 * Logger liviano con timestamp para consola.
 * Centraliza el formato de todos los mensajes de la aplicación.
 */
public final class Logger {

    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Logger() {}

    public static void info(String mensaje) {
        System.out.printf("[%s] [INFO]  %s%n", ahora(), mensaje);
    }

    public static void warn(String mensaje) {
        System.out.printf("[%s] [WARN]  %s%n", ahora(), mensaje);
    }

    public static void error(String mensaje) {
        System.err.printf("[%s] [ERROR] %s%n", ahora(), mensaje);
    }

    public static void error(String mensaje, Exception e) {
        error(mensaje + " → " + e.getMessage());
    }

    public static void separador() {
        System.out.println("─".repeat(70));
    }

    private static String ahora() {
        return LocalDateTime.now().format(FORMATO);
    }
}
