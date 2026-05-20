package server_checker;

import server_checker.model.TurnoCaja;
import server_checker.util.Logger;

import java.io.*;
import java.util.Date;
import java.util.Vector;

/**
 * EscritorTurnoCaja
 * -----------------
 * Utilidad para generar los archivos .dat e .idx en la carpeta configurada.
 *
 * Úsala desde el sistema que origina los datos (ej: el sistema de caja)
 * para persistir un Vector<TurnoCaja> en disco con el formato
 * que ArchivoRepository espera leer.
 *
 * Formato .idx → N+1 longs: offset[0..N-1] + centinela (tamaño total del .dat)
 * Formato .dat → por registro: int | UTF | double | double | long | boolean
 */
public class EscritorTurnoCaja {

    private static final String CARPETA    = "C:\\Users\\HP\\OneDrive\\Desktop\\Datos";
    private static final String NOMBRE_DAT = "turnos.dat";
    private static final String NOMBRE_IDX = "turnos.idx";

    // ── Ejemplo de uso ────────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {
        Vector<TurnoCaja> turnos = new Vector<>();
        turnos.add(new TurnoCaja(1, "Juan Pérez",   500.00, 1200.50, new Date(), true));
        turnos.add(new TurnoCaja(2, "María López",  300.00,  980.75, new Date(), true));
        turnos.add(new TurnoCaja(3, "Carlos Ruiz",  450.00,    0.00, new Date(), false));

        escribir(CARPETA, turnos);
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Serializa el vector de turnos en los archivos .dat e .idx
     * dentro de la carpeta indicada.
     */
    public static void escribir(String rutaCarpeta, Vector<TurnoCaja> turnos) throws IOException {
        File carpeta = prepararCarpeta(rutaCarpeta);
        File datFile = new File(carpeta, NOMBRE_DAT);
        File idxFile = new File(carpeta, NOMBRE_IDX);

        long[] offsets = escribirDat(datFile, turnos);
        escribirIdx(idxFile, offsets);

        Logger.info("Escritura completada: " + turnos.size() + " registro(s).");
    }

    // ── Privado ───────────────────────────────────────────────────────────────

    private static File prepararCarpeta(String ruta) {
        File carpeta = new File(ruta);
        if (!carpeta.exists()) carpeta.mkdirs();
        return carpeta;
    }

    /**
     * Escribe el .dat y retorna el arreglo de offsets (incluye centinela al final).
     */
    private static long[] escribirDat(File datFile, Vector<TurnoCaja> turnos) throws IOException {
        long[] offsets = new long[turnos.size() + 1];
        long posicion = 0;

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(datFile)))) {

            for (int i = 0; i < turnos.size(); i++) {
                offsets[i] = posicion;
                byte[] bytes = serializarTurno(turnos.get(i));
                dos.write(bytes);
                posicion += bytes.length;
            }
            offsets[turnos.size()] = posicion; // centinela
        }

        Logger.info("DAT escrito: " + datFile.getAbsolutePath());
        return offsets;
    }

    private static void escribirIdx(File idxFile, long[] offsets) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(idxFile)))) {

            for (long offset : offsets) {
                dos.writeLong(offset);
            }
        }
        Logger.info("IDX escrito: " + idxFile.getAbsolutePath());
    }

    /**
     * Serializa un TurnoCaja a bytes usando el mismo orden que ArchivoRepository.
     * int | UTF | double | double | long | boolean
     */
    private static byte[] serializarTurno(TurnoCaja t) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt    (t.getId());
        dos.writeUTF    (t.getNombreCajero());
        dos.writeDouble (t.getMontoApertura());
        dos.writeDouble (t.getMontoCierre());
        dos.writeLong   (t.getFecha().getTime());
        dos.writeBoolean(t.isEstado());
        dos.flush();

        return baos.toByteArray();
    }
}
