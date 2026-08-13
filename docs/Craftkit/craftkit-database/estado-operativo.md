# Estado operativo

`craftkit-database` monitorea siempre la capacidad observada del pool Hikari para adquirir y validar una conexión. Esta señal sirve para observabilidad operativa; no es una garantía sobre la próxima operación ni una afirmación de que el servidor físico esté caído.

## Lectura no bloqueante

```java
DatabaseOperationalStatus status = database.operationalStatus();
```

`operationalStatus()` devuelve el snapshot actual sin ejecutar I/O ni esperar un probe. También está permitido después de `Database.close()` y entonces devuelve `CLOSED`.

Campos del snapshot:

| Campo | Significado |
| --- | --- |
| `sequence` | `0` en el snapshot inicial; aumenta por cada probe aceptado y al publicar `CLOSED`. |
| `state` | Estado operativo actual. |
| `stateSince` | Instante desde el que rige el estado actual. |
| `lastCheckAt` | Instante del último probe aceptado, o `null`. |
| `lastSuccessAt` | Instante del último probe exitoso, o `null`. |
| `lastFailureAt` | Instante del último probe fallido, o `null`. |
| `lastFailure` | Última `DatabaseException` del monitor, o `null`. |

`isOperational()` solo devuelve `true` cuando `state()` es `OPERATIONAL`. `lastFailure` se conserva después de una recuperación para diagnóstico histórico. Si el fallo proviene de JDBC, la `SQLException` se preserva como causa cuando existe.

## Estados y transiciones

| Estado | Significado |
| --- | --- |
| `STARTING` | Aún no se aceptó el resultado del primer probe. |
| `OPERATIONAL` | El último proceso de validación alcanzó el umbral de éxito aplicable. |
| `UNAVAILABLE` | El pool no pudo adquirir o validar una conexión en el último probe aceptado. |
| `RECOVERING` | Hubo éxitos desde la indisponibilidad, pero todavía no se alcanzó el umbral configurado. |
| `CLOSED` | Estado terminal publicado al iniciar `Database.close()`. |

Reglas:

- primer éxito desde `STARTING` -> `OPERATIONAL`;
- fallo desde `STARTING` u `OPERATIONAL` -> `UNAVAILABLE`;
- éxitos desde indisponibilidad por debajo de `recoverySuccessThreshold` -> `RECOVERING`;
- éxito que alcanza el umbral -> `OPERATIONAL`;
- fallo durante `RECOVERING` -> `UNAVAILABLE`;
- inicio de `close()` -> `CLOSED` terminal.

Con `recoverySuccessThreshold = 1`, la recuperación pasa directamente de `UNAVAILABLE` a `OPERATIONAL` y omite `RECOVERING`.

## Cómo funciona el monitor

- Está siempre habilitado y realiza el primer probe inmediatamente.
- Ejecuta con fixed delay según `checkIntervalMillis`.
- Nunca ejecuta más de un probe a la vez.
- Usa el mismo pool Hikari que las operaciones SQL.
- Cada probe obtiene una conexión con `getConnection()`, llama `Connection.isValid(...)` y la cierra inmediatamente.
- Usa infraestructura daemon interna propiedad de CraftKit, aunque las operaciones SQL usen un executor externo.

`Connection.isValid` recibe segundos. CraftKit redondea `validationTimeoutMillis` hacia arriba, con mínimo de `1`; el peor caso de un probe es aproximadamente adquisición más validación y depende del driver.

La señal describe al pool, no al servidor físico. Por ejemplo, la saturación del pool puede producir `UNAVAILABLE` aunque MySQL siga activo.

## Observers

```java
DatabaseStatusRegistration registration = database.observeOperationalStatus(status -> {
    logger.info("Database state: {}", status.state());
});
```

Cada observer recibe:

- un snapshot inicial;
- snapshots posteriores solo cuando cambia `state`.

Como `sequence` aumenta también en probes que no cambian el estado, los observers pueden ver saltos de secuencia. Las notificaciones se entregan ordenadas y serializadas en un thread daemon interno, no en Paper ni en el executor SQL.

El listener debe ser rápido y no bloqueante. Una `RuntimeException` de un listener se aísla y no detiene el monitor ni otros observers.

```java
registration.close();
```

El cierre de la registration es idempotente y cancela callbacks que aún no comenzaron. Un callback ya iniciado puede terminar. Registrar un observer después de `Database.close()` falla con `DatabaseException`.

Al cerrar `Database`, `CLOSED` se refleja sincrónicamente en `operationalStatus()` y su notificación se encola de forma asíncrona.

## Qué no controla

El estado no bloquea, reintenta ni modifica `query`, `update`, `execute`, `transaction` o `migrate`. Cada future conserva sus propios errores y no hay circuit breaker.

Tampoco confirma migraciones ni readiness del schema. `migrate()` sigue siendo una operación separada. El retry transaccional es independiente de `RECOVERING`.

## Cierre y resultados tardíos

`CLOSED` es terminal. Al iniciar `close()`, CraftKit cancela el monitor y descarta resultados de probes que lleguen tarde. Las tareas SQL aún no iniciadas completan sus futures excepcionalmente, incluso si estaban en un executor externo; las operaciones ya en vuelo pueden terminar y sus efectos pueden ser ambiguos.

## Acceso directo al pool

`dataSource()` permite generar actividad fuera de las operaciones async, pero el monitor sigue observando ese mismo pool. Cerrar o corromper directamente el datasource no equivale a `Database.close()` y terminará apareciendo como indisponibilidad.

## Verificación de integración

La suite Testcontainers cubre outage y recuperación contra MySQL real. Docker debe estar disponible en CI para ejecutar esos tests de integración.
