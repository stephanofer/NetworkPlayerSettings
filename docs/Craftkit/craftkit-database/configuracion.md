# Configuración — `craftkit-database`

La configuración final es inmutable y se construye mediante builders. Si una configuración inválida llega a `build()`, se lanza `DatabaseException`.

## `DatabaseConfig`

Config raíz para crear una conexión MySQL.

```java
MigrationConfig migration = MigrationConfig.builder()
    .existingSchemaStrategy(ExistingSchemaStrategy.BASELINE_AT_ZERO)
    .classLoader(getClass().getClassLoader())
    .build();

DatabaseConfig config = DatabaseConfig.builder()
    .host("127.0.0.1")
    .port(3306)
    .database("hera_network")
    .username("survival")
    .password("secret")
    .tablePrefix("survival_")
    .pool(PoolConfig.builder().maximumPoolSize(8).build())
    .executor(ExecutorConfig.builder().threadNamePrefix("survival-db"))
    .health(DatabaseHealthConfig.builder()
        .checkIntervalMillis(5_000)
        .recoverySuccessThreshold(2)
        .build())
    .migration(migration)
    .putJdbcProperty("socketTimeout", "4000")
    .build();
```

### Campos

| Campo | Default | Validación / comportamiento |
| --- | --- | --- |
| `host` | requerido | No puede estar vacío. |
| `port` | `3306` | Debe estar entre `1` y `65535`. |
| `database` | requerido | No puede estar vacío. |
| `username` | requerido | No puede estar vacío. |
| `password` | `""` | No puede ser `null`. |
| `tablePrefix` | `""` | Solo letras, números y `_`; se valida también contra la tabla de historial Flyway. |
| `pool` | `PoolConfig.builder().build()` | Config Hikari. |
| `executor` | derivado de `PoolConfig` | Si no se define, usa `maximumPoolSize` como cantidad de threads. |
| `health` | `DatabaseHealthConfig.builder().build()` | Monitor operativo siempre habilitado. |
| `migration` | `MigrationConfig.builder().build()` | Config Flyway. |
| `driverClassName` | `com.mysql.cj.jdbc.Driver` para MySQL | Override avanzado opcional; si se deja vacío o `null`, CraftKit usa el driver MySQL oficial. |
| `jdbcProperties` | vacío | Keys no vacías; values no `null`. |

`DatabaseConfig.toString()` oculta la contraseña como `password=<hidden>`.

CraftKit configura explícitamente el driver MySQL en Hikari en vez de depender solo de autodiscovery JDBC. Esto evita fallos por classloaders en runtimes como Velocity, donde el driver puede estar dentro del JAR del plugin pero no ser visible para `DriverManager`. `driverClassName(...)` es un override avanzado: el consumidor debe aportar el driver y este debe aceptar la URL `jdbc:mysql:` que construye el módulo. CraftKit no declara ni verifica compatibilidad MariaDB.

> Nota: `MigrationConfig.sharedDatabaseDefaults()` devuelve una configuración final. Si el plugin consumidor necesita migraciones `classpath:` desde su propio JAR, construya el `MigrationConfig` con `classLoader(getClass().getClassLoader())` y aplique también `existingSchemaStrategy(ExistingSchemaStrategy.BASELINE_AT_ZERO)` cuando use base compartida.

## `PoolConfig`

Configura HikariCP.

| Campo | Default | Validación |
| --- | --- | --- |
| `poolName` | `craftkit-mysql` | No puede estar vacío. |
| `maximumPoolSize` | `10` | Debe ser `>= 1`. |
| `minimumIdle` | `null` | Si existe, debe estar entre `0` y `maximumPoolSize`. |
| `connectionTimeoutMillis` | `10_000` | Debe ser `>= 250`, mínimo aceptado por HikariCP. |
| `validationTimeoutMillis` | `5_000` | Debe ser `>= 250`, mínimo aceptado por HikariCP. |
| `idleTimeoutMillis` | `600_000` | Debe ser `>= 0`. |
| `maxLifetimeMillis` | `1_800_000` | Debe ser `>= 0`. |
| `autoCommit` | `true` | Booleano. |
| `leakDetectionThresholdMillis` | `0` | Debe ser `>= 0`. |

`validationTimeoutMillis` también define el timeout pasado a `Connection.isValid(int)`. Como JDBC usa segundos, CraftKit redondea los milisegundos hacia arriba, con mínimo de `1` segundo. El peor caso de un probe es aproximadamente el tiempo de adquisición más el de validación, sujeto al comportamiento del driver.

## `ExecutorConfig`

Configura el executor interno cuando se usa `Databases.mysql(config)`.

| Campo | Default | Validación |
| --- | --- | --- |
| `threadCount` | `PoolConfig.maximumPoolSize` cuando se construye desde `DatabaseConfig` | Debe ser `>= 1`. |
| `threadNamePrefix` | `craftkit-database` | No puede estar vacío. |
| `daemon` | `true` | Booleano. |
| `shutdownTimeoutMillis` | `10_000` | Debe ser `> 0`. |

El executor interno es un `ThreadPoolExecutor` fijo con cola `ArrayBlockingQueue`. La capacidad de cola interna es `max(64, threadCount * 128)`.

Este executor se usa para SQL y migraciones. El monitor, el scheduler de backoff transaccional y los callbacks de estado usan recursos daemon internos, incluso cuando se configura un executor SQL externo.

## `DatabaseHealthConfig`

Configura el monitor operativo, que siempre está habilitado.

| Campo | Default | Validación / comportamiento |
| --- | --- | --- |
| `checkIntervalMillis` | `5_000` | Debe ser `> 0`; fixed delay entre probes. |
| `recoverySuccessThreshold` | `2` | Debe ser `>= 1`; éxitos consecutivos requeridos para salir de indisponibilidad. Con `1`, se omite `RECOVERING`. |

El primer probe se ejecuta inmediatamente. Solo hay un probe en curso y usa el mismo pool que las operaciones. Consulte [Estado operativo](./estado-operativo.md).

## Propiedades JDBC por defecto

`HikariDataSources` agrega estas propiedades por defecto:

| Propiedad | Valor |
| --- | --- |
| `cachePrepStmts` | `true` |
| `prepStmtCacheSize` | `250` |
| `prepStmtCacheSqlLimit` | `2048` |
| `useServerPrepStmts` | `true` |
| `rewriteBatchedStatements` | `true` |

El consumidor puede sobreescribirlas con `putJdbcProperty(...)` o `jdbcProperties(...)`.

## Comportamiento de creación del datasource

La URL JDBC construida es:

```text
jdbc:mysql://<host>:<port>/<database>
```

El `HikariConfig` actual usa `initializationFailTimeout = -1`. Esto permite crear el datasource sin exigir una conexión durante su construcción; el monitor inicia un probe inmediato y los errores de conectividad también pueden aparecer de forma independiente en operaciones o migraciones.

Durante la creación del datasource, CraftKit registra un diagnóstico seguro con el pool, destino `host:port/database`, driver configurado/resuelto, clase cargada, classloader y versión disponible. No se registra la contraseña ni la URL completa con query params.
