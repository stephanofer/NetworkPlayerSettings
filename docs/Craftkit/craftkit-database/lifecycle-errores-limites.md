# Lifecycle, errores y límites

## Ownership de recursos

### `Databases.mysql(config)`

CraftKit crea y posee:

- `HikariDataSource`;
- executor SQL interno;
- monitor de estado operativo;
- scheduler daemon de backoff transaccional;
- executor daemon de callbacks de estado.

`database.close()` cierra todos estos recursos.

### `Databases.mysql(config, executor)`

CraftKit crea y posee:

- `HikariDataSource`;
- monitor de estado operativo;
- scheduler daemon de backoff transaccional;
- executor daemon de callbacks de estado.

El consumidor posee:

- executor SQL externo.

`database.close()` no cierra el executor SQL externo.

## Cierre

```java
database.close();
```

`close()`:

1. Publica el estado terminal `CLOSED`.
2. Cancela el monitor y los retries todavía en backoff; cualquier resultado tardío de un probe se descarta.
3. Cierra el datasource.
4. Espera el monitor de forma acotada por sus timeouts de adquisición y validación.
5. Cierra los recursos internos, incluido el executor SQL cuando CraftKit lo posee.
6. Si el executor SQL interno no termina dentro de `shutdownTimeoutMillis`, usa `shutdownNow()` y vuelve a esperar el mismo límite.
7. Si el cierre falla, lanza `DatabaseException`.

El cierre es idempotente. `operationalStatus()` refleja `CLOSED` sincrónicamente; la transición se entrega a observers de forma asíncrona y ordenada.

Las tareas todavía en cola completan sus futures excepcionalmente, tanto con executor interno como externo. CraftKit no vacía ni cierra una cola externa, pero invalida sus tareas aún no iniciadas. Las operaciones que ya estaban en vuelo pueden terminar durante el cierre; ante pérdida de conexión o respuesta, sus efectos pueden ser ambiguos y deben resolverse según la semántica de la operación.

## Operaciones después de `close()`

Después de cerrar:

- `migrate()`, incluso con migraciones deshabilitadas, `query(...)`, `update(...)`, `execute(...)` y `transaction(...)` devuelven `CompletableFuture` fallido.
- `dataSource()` y `table(...)` lanzan `DatabaseException`.
- `observeOperationalStatus(...)` lanza `DatabaseException`.
- `operationalStatus()` sigue permitido y devuelve `CLOSED`.
- `isClosed()` devuelve `true`.

## Errores

El módulo usa `DatabaseException` como excepción runtime pública.

Ejemplos de errores envueltos:

- fallo creando datasource;
- fallo en query;
- fallo en update;
- fallo en operación;
- fallo en transacción;
- agotamiento de retry transaccional;
- fallo en Flyway;
- uso después de cerrar;
- configuración inválida;
- prefijo o nombre de tabla inválido.

Cuando un probe falla por `SQLException`, `lastFailure()` expone una `DatabaseException` que conserva esa excepción como causa. El último fallo se mantiene después de la recuperación para diagnóstico histórico.

## Acceso directo al datasource

`dataSource()` permite actividad fuera de las operaciones async de `Database`. Esa actividad sigue usando el pool observado por el monitor, pero el consumidor debe gestionar threads y recursos JDBC.

Cerrar o corromper directamente el datasource no equivale a `Database.close()`: `isClosed()` no cambia por ello, aunque el monitor terminará publicando indisponibilidad.

## Secretos

`DatabaseConfig.toString()` no expone la contraseña. La renderiza como:

```text
password=<hidden>
```

El mensaje al fallar la creación del datasource usa host, puerto y database, pero no incluye password.

## Límites actuales

`craftkit-database` no implementa:

- Paper scheduler;
- retorno automático al main thread;
- ORM;
- query builder;
- repositorios genéricos;
- transaction manager avanzado;
- nested transactions;
- helpers de savepoints;
- métricas;
- circuit breaker;
- retry automático global;
- retry de queries/updates fuera de transacción.

Estos límites son intencionales para mantener el módulo ligero y enfocado en infraestructura crítica.

## Verificación existente

La suite de tests cubre:

- defaults y validación de configs;
- ocultamiento de password;
- construcción de `HikariConfig`;
- validación de prefijos/tablas;
- executor interno y ownership;
- cierre idempotente;
- operaciones después de close;
- monitor, snapshots, transiciones, observers y cancelación;
- migraciones disabled/enabled;
- configuración Flyway con placeholders/history table;
- estrategias `FAIL`, `BASELINE_AT_ZERO`, `BASELINE_AT_VERSION`;
- transacciones con commit/rollback/restauración de estado;
- executor de transacciones;
- retry transaccional unitario para deadlock/lock timeout;
- integración con MySQL real mediante Testcontainers para deadlock, lock wait timeout, outage y recuperación del estado operativo.

Los tests de integración requieren Docker disponible en CI.
