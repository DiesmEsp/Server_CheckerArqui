# Server_Checker

Aplicación Java de sincronización que monitorea una carpeta en busca de archivos binarios (`.dat` / `.idx`) con registros de turnos de caja (`TurnoCaja`) y los inserta en una base de datos MySQL.

---

## Requisitos previos

- **Java 21 JDK** (o superior)
- **MySQL Server** corriendo en un equipo accesible
- **Gradle** (se incluye el wrapper `gradlew.bat`, no necesita instalación)

---

## Configuración

Toda la configuración se encuentra en un solo archivo:

```
app/src/main/java/server_checker/config/AppConfig.java
```

### 1. Ruta de la carpeta a monitorear

```java
public static final String RUTA_CARPETA = "C:\\ruta\\completa\\a\\tu\\carpeta";
```

Cambia esta ruta por la carpeta donde el sistema externo deposita los archivos `.dat` y `.idx`.

> **Importante:** En Windows las barras invertidas deben escaparse con doble backslash (`\\`).
> En Linux/macOS usar barras normales: `/home/usuario/datos`

### 2. Conexión a la base de datos

```java
public static final String DB_URL     = "jdbc:mysql://localhost:3306/arquiproy";
public static final String DB_USUARIO = "root";
public static final String DB_CLAVE   = "tu_contraseña";
```

- `localhost` → cámbialo por la IP o nombre del servidor MySQL si no está en el mismo equipo.
- `3306` → puerto por defecto de MySQL; cámbialo si usas otro.
- `arquiproy` → nombre de la base de datos; puedes usar cualquier nombre.

### 3. Intervalo de sincronización

```java
public static final long INTERVALO_MS = 10_000L;  // 10 segundos
```

Valor en milisegundos entre cada ciclo de revisión de archivos.

### 4. Nombres de archivos esperados (opcional)

```java
public static final String NOMBRE_DAT = "turno_caja.dat";
public static final String NOMBRE_IDX = "turno_caja.idx";
```

Solo si el sistema externo genera archivos con otros nombres.

---

## Base de datos

### 1. Crear la base de datos

```sql
CREATE DATABASE arquiproy;
```

(O el nombre que hayas puesto en `DB_URL`)

### 2. Crear la tabla

Ejecutar en la base de datos creada:

```sql
CREATE TABLE TurnoCaja (
    id              INT PRIMARY KEY,
    nombre_cajero   VARCHAR(100),
    monto_apertura  DOUBLE,
    monto_cierre    DOUBLE,
    fecha           DATETIME,
    estado          TINYINT(1)
);
```

---

## Compilar y ejecutar

### En Windows

```batch
gradlew clean build jar
java -jar app\build\libs\app.jar
```

### En Linux / macOS

```bash
./gradlew clean build jar
java -jar app/build/libs/app.jar
```

El JAR generado incluye todas las dependencias (JDBC MySQL, Guava), no necesita configuración adicional.

---

## Estructura del proyecto

```
Server_Checker/
├── app/
│   ├── build.gradle              ← Dependencias y plugin de aplicación
│   └── src/
│       └── main/java/server_checker/
│           ├── Main.java                        ← Punto de entrada
│           ├── TurnoCaja.java                   ← (clase antigua, no usar)
│           ├── EscritorTurnoCaja.java           ← Utilidad para generar archivos de prueba
│           ├── config/
│           │   └── AppConfig.java               ← ⚙ ÚNICO archivo de configuración
│           ├── model/
│           │   └── TurnoCaja.java               ← Modelo de datos
│           ├── repository/
│           │   ├── ArchivoRepository.java       ← Lectura de .dat/.idx
│           │   └── TurnoCajaRepository.java     ← Persistencia en MySQL
│           ├── service/
│           │   └── SincronizacionService.java   ← Lógica de sincronización
│           ├── scheduler/
│           │   └── SincronizacionScheduler.java ← Ejecutor periódico
│           └── util/
│               └── Logger.java                 ← Log en consola
├── gradlew / gradlew.bat        ← Gradle Wrapper
├── settings.gradle
└── gradle/libs.versions.toml    ← Versiones de dependencias
```

---

## Funcionamiento

1. La aplicación revisa cada `INTERVALO_MS` milisegundos si existen los archivos `turno_caja.dat` y `turno_caja.idx` en `RUTA_CARPETA`.
2. Lee el `.idx` para obtener las posiciones de los registros dentro del `.dat`.
3. Deserializa cada registro (formato binario de 229 bytes fijos).
4. Elimina los archivos `.dat` e `.idx` para no reprocesarlos.
5. Inserta en MySQL los registros nuevos (omite duplicados por ID).
6. Repite el ciclo hasta que se detenga la aplicación (Ctrl+C).

---

## Notas importantes

- **Los archivos `.dat` e `.idx` deben tener un formato binario específico** (ver `ArchivoRepository.java`). Deben ser generados por el sistema `com.brenis.TurnoRepository`.
- Si la inserción en MySQL falla, los archivos ya se habrán eliminado. Revisar los logs en consola.
- La clase `App.java` (en `server_checker.App`) es un stub vacío generado por Gradle y **no se usa**. El punto de entrada real es `server_checker.Main`.
