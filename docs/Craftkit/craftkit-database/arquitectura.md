# Arquitectura — `craftkit-database`

`craftkit-database` expone una API pequeña y deja la implementación concreta en clases internas.

## Mapa de componentes

| Componente | Tipo | Responsabilidad |
| --- | --- | --- |
| `Databases` | Público | Entry point para crear `Database`. |
| `Database` | Público | Contrato principal: estado operativo, migraciones, operaciones async, transacciones, `DataSource`, tablas y cierre. |
| `DatabaseConfig` | Público | Configuración raíz de MySQL, pool, executor SQL, health, migraciones y propiedades JDBC. |
| `PoolConfig` | Público | Parámetros HikariCP. |
| `ExecutorConfig` | Público | Parámetros del executor interno de DB. |
| `DatabaseHealthConfig` | Público | Intervalo del monitor y umbral de éxitos para recuperación. |
| `DatabaseOperationalStatus`, `DatabaseOperationalState` | Público | Snapshot estructuralmente inmutable y estados del monitor. |
| `DatabaseOperationalStatusListener`, `DatabaseStatusRegistration` | Público | Observación y cancelación de notificaciones de estado. |
| `MigrationConfig` | Público | Parámetros de Flyway y estrategia para schemas existentes. |
| `TransactionOptions` | Público | Opciones por transacción: isolation, read-only y política de retry. |
| `TransactionIsolation` | Público | Mapeo a niveles JDBC de isolation. |
| `TransactionRetryPolicy` | Público | Política opt-in de retry transaccional: intentos, backoff, jitter, classifier y listener. |
| `SqlRetryClassifier` | Público | Clasificador de `SQLException` reintentables. Incluye classifier MySQL para deadlock/lock timeout. |
| `TransactionRetryListener`, `TransactionRetryEvent` | Público | Observabilidad de reintentos sin acoplar el módulo a logging/métricas. |
| `ExistingSchemaStrategy` | Público | Estrategia de Flyway para bases no vacías/schemas existentes. |
| `SqlQuery`, `SqlUpdate`, `SqlOperation`, `SqlTransaction` | Público | Functional interfaces usadas por el consumidor. |
| `DatabaseException` | Público | Runtime exception del módulo. |
| `HikariDatabase` | Interno | Implementación de `Database`. |
| `HikariDataSources` | Interno | Construcción de `HikariConfig` y `HikariDataSource`. |
| `DatabaseExecutors` | Interno | Creación y cierre del executor interno. |
| `FlywayMigrator` | Interno | Configuración y ejecución de Flyway. |
| `DatabaseMigrator` | Interno | Abstracción interna para migraciones. |
| `DatabaseHealthMonitor` | Interno | Ejecuta probes seriales sobre el pool con fixed delay. |
| `DatabaseOperationalTracker` | Interno | Mantiene snapshots, transiciones y observers. |
| `TablePrefixes` | Interno | Validación y composición de prefijos/nombres de tabla. |

## Flujo de creación

```java
Database database = Databases.mysql(config);
```

Internamente:

1. Valida `DatabaseConfig`.
2. Crea un `ExecutorService` con `DatabaseExecutors.createExecutor(config.executor())`, salvo que se haya provisto uno externo.
3. Crea un `HikariDataSource` con `HikariDataSources.create(config)`.
4. Crea `FlywayMigrator` usando el datasource, `MigrationConfig`, `tablePrefix` y el `ClassLoader` configurado para resolver migraciones `classpath:`.
5. Crea el tracker, el monitor y el scheduler de backoff; el executor de callbacks se crea al registrar el primer observer.
6. Inicia el monitor con un primer probe inmediato y devuelve `HikariDatabase`.

Si falla la creación del datasource o de la instancia, el código intenta cerrar los recursos ya creados y agrega fallos de cierre como `suppressed`.

## Variante con executor externo

```java
Database database = Databases.mysql(config, customExecutor);
```

En esta variante CraftKit crea y cierra el `HikariDataSource`, pero **no cierra** el executor SQL externo. El consumidor es dueño de ese executor. El monitor, el scheduler de backoff y el executor daemon de callbacks siempre pertenecen a CraftKit y se cierran con `Database`.

## Modelo async

Todas las operaciones principales se envían a un executor explícito:

- `migrate()`
- `query(...)`
- `update(...)`
- `execute(...)`
- `transaction(...)`

`HikariDatabase` usa `executor.execute(...)` y completa manualmente un `CompletableFuture`. El código actual no usa `ForkJoinPool.commonPool()` ni `CompletableFuture.supplyAsync(...)` para ejecutar JDBC.

El monitor no comparte ese executor. Usa infraestructura interna daemon, ejecuta un solo probe por vez con fixed delay y consulta el mismo pool mediante `getConnection()` e `isValid(...)`; cada conexión de prueba se cierra inmediatamente.

Cuando una transacción tiene retry opt-in, cada intento vuelve a entrar al executor DB. El backoff se programa en un scheduler daemon propiedad de `Database` para no ocupar workers SQL; el JDBC del siguiente intento vuelve siempre al executor configurado. Cerrar `Database` cancela los backoffs pendientes y completa sus futures excepcionalmente.

## Retry transaccional

El retry vive en `craftkit-database` porque deadlocks y lock timeouts son una preocupación transversal de los plugins HERA con escrituras concurrentes.

Principios de diseño:

- Es opt-in por transacción mediante `TransactionOptions.retryPolicy(...)`.
- Reintenta la transacción completa, no una sentencia aislada.
- Usa una conexión nueva por intento.
- Cierra la conexión antes de aplicar backoff.
- Solo reintenta fallos clasificados durante el callback del consumidor.
- No reintenta fallos de `commit`, `rollback`, restauración de estado, cierre de conexión, setup, runtime exceptions ni errores no transitorios.
- No completa el `CompletableFuture` hasta que el callback, el `commit` y el cierre del intento exitoso hayan terminado correctamente.

El retry transaccional y el estado `RECOVERING` son mecanismos independientes. El monitor no modifica la política ni la ejecución de una transacción.

## Estado operativo

El estado describe la capacidad observada del pool para adquirir y validar una conexión. No es un circuit breaker, no confirma migraciones ni garantiza que una operación futura tendrá éxito. Las operaciones SQL conservan su ejecución y sus errores propios, independientemente del estado publicado.

Los observers reciben su snapshot inicial y luego solo cambios de estado, ordenados y serializados en un thread daemon interno. No se ejecutan en Paper ni en el executor SQL. Consulte [Estado operativo](./estado-operativo.md) para las transiciones, timestamps y garantías de cierre.

## Relación con Paper

El módulo no importa ni usa Paper/Bukkit. Esto mantiene `craftkit-database` como infraestructura Java/JDBC reusable.

Consecuencia: cualquier callback de operaciones SQL u observer que toque jugadores, mundos, inventarios, scoreboards o cualquier API Paper debe volver al main thread desde el plugin consumidor.
