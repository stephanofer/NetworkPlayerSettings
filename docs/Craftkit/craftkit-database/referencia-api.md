# Referencia de API pública

Esta referencia lista la API pública actual del paquete `com.hera.craftkit.database`.

## `Databases`

```java
public static Database mysql(DatabaseConfig config);
public static Database mysql(DatabaseConfig config, Executor executor);
```

- `mysql(config)`: crea datasource y executor SQL interno. CraftKit cierra ambos, el monitor, el scheduler de retry y los callbacks de estado.
- `mysql(config, executor)`: crea datasource y usa executor SQL externo. CraftKit no cierra ese executor, pero sí el datasource, el monitor, el scheduler de retry y los callbacks de estado.

## `Database`

```java
CompletableFuture<Void> migrate();
DatabaseOperationalStatus operationalStatus();
DatabaseStatusRegistration observeOperationalStatus(DatabaseOperationalStatusListener listener);
<T> CompletableFuture<T> query(SqlQuery<T> query);
CompletableFuture<Integer> update(SqlUpdate update);
CompletableFuture<Void> execute(SqlOperation operation);
<T> CompletableFuture<T> transaction(SqlTransaction<T> transaction);
<T> CompletableFuture<T> transaction(TransactionOptions options, SqlTransaction<T> transaction);
DataSource dataSource();
String tablePrefix();
String table(String name);
boolean isClosed();
void close();
```

`operationalStatus()` es no bloqueante y sigue permitido después de `close()`. `observeOperationalStatus(...)` lanza `DatabaseException` si la instancia está cerrada.

## Functional interfaces

```java
public interface SqlQuery<T> {
    T execute(Connection connection) throws SQLException;
}
```

```java
public interface SqlUpdate {
    int execute(Connection connection) throws SQLException;
}
```

```java
public interface SqlOperation {
    void execute(Connection connection) throws SQLException;
}
```

```java
public interface SqlTransaction<T> {
    T execute(Connection connection) throws SQLException;
}
```

## `DatabaseConfig`

Builder methods:

```java
host(String)
port(int)
database(String)
username(String)
password(String)
tablePrefix(String)
pool(PoolConfig)
executor(ExecutorConfig)
executor(ExecutorConfig.Builder)
migration(MigrationConfig)
health(DatabaseHealthConfig)
driverClassName(String)
jdbcProperties(Map<String, String>)
putJdbcProperty(String, String)
build()
```

Getters:

```java
host(); port(); database(); username(); password(); tablePrefix();
pool(); executor(); migration(); health(); driverClassName(); jdbcProperties();
```

## `DatabaseHealthConfig`

Factory y builder:

```java
builder()
checkIntervalMillis(long)
recoverySuccessThreshold(int)
build()
```

Getters:

```java
checkIntervalMillis()
recoverySuccessThreshold()
```

Constantes:

```java
DEFAULT_CHECK_INTERVAL_MILLIS = 5_000L
DEFAULT_RECOVERY_SUCCESS_THRESHOLD = 2
```

`checkIntervalMillis` debe ser `> 0` y `recoverySuccessThreshold` debe ser `>= 1`.

## Estado operativo

### `DatabaseOperationalState`

```java
STARTING
OPERATIONAL
UNAVAILABLE
RECOVERING
CLOSED
```

### `DatabaseOperationalStatus`

```java
long sequence()
DatabaseOperationalState state()
Instant stateSince()
Instant lastCheckAt()
Instant lastSuccessAt()
Instant lastFailureAt()
DatabaseException lastFailure()
boolean isOperational()
```

Los timestamps de checks, éxitos y fallos pueden ser `null` si todavía no ocurrieron. `isOperational()` solo devuelve `true` para `OPERATIONAL`.

### `DatabaseOperationalStatusListener`

```java
void onStatusChanged(DatabaseOperationalStatus status)
```

### `DatabaseStatusRegistration`

```java
boolean isClosed()
void close()
```

El cierre de la registration es idempotente. La semántica de snapshots y entrega se documenta en [Estado operativo](./estado-operativo.md).

## `PoolConfig`

Builder methods:

```java
poolName(String)
maximumPoolSize(int)
minimumIdle(Integer)
connectionTimeoutMillis(long)
validationTimeoutMillis(long)
idleTimeoutMillis(long)
maxLifetimeMillis(long)
autoCommit(boolean)
leakDetectionThresholdMillis(long)
build()
```

Constantes:

```java
DEFAULT_POOL_NAME = "craftkit-mysql"
DEFAULT_MAXIMUM_POOL_SIZE = 10
DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10_000L
DEFAULT_VALIDATION_TIMEOUT_MILLIS = 5_000L
DEFAULT_IDLE_TIMEOUT_MILLIS = 600_000L
DEFAULT_MAX_LIFETIME_MILLIS = 1_800_000L
DEFAULT_LEAK_DETECTION_THRESHOLD_MILLIS = 0L
```

## `ExecutorConfig`

Builder methods:

```java
threadCount(int)
threadNamePrefix(String)
daemon(boolean)
shutdownTimeoutMillis(long)
build()
build(int defaultThreadCount)
```

Constantes:

```java
DEFAULT_THREAD_NAME_PREFIX = "craftkit-database"
DEFAULT_DAEMON = true
DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 10_000L
```

## `MigrationConfig`

Factories:

```java
builder()
sharedDatabaseDefaults()
```

Builder methods:

```java
enabled(boolean)
clearLocations()
locations(List<String>)
addLocation(String)
baselineOnMigrate(boolean)
validateOnMigrate(boolean)
cleanDisabled(boolean)
existingSchemaStrategy(ExistingSchemaStrategy)
baselineVersion(String)
baselineDescription(String)
placeholders(Map<String, String>)
putPlaceholder(String, String)
classLoader(ClassLoader)
build()
```

Constante:

```java
DEFAULT_LOCATION = "classpath:db/migration"
```

## `ExistingSchemaStrategy`

```java
FAIL
BASELINE_AT_ZERO
BASELINE_AT_VERSION
```

## `TransactionOptions`

Factories:

```java
defaults()
readUncommitted()
readCommitted()
repeatableRead()
serializable()
readOnly(TransactionIsolation)
builder()
```

Builder methods:

```java
isolation(TransactionIsolation)
readOnly(boolean)
retryPolicy(TransactionRetryPolicy)
build()
```

Getters:

```java
isolation()
readOnly()
retryPolicy()
```

## `TransactionIsolation`

```java
DEFAULT
READ_UNCOMMITTED
READ_COMMITTED
REPEATABLE_READ
SERIALIZABLE
```

Métodos:

```java
jdbcLevel()
shouldApply()
```

`DEFAULT` tiene `jdbcLevel() == null` y `shouldApply() == false`.

## `TransactionRetryPolicy`

Factories:

```java
none()
mysqlTransient()
builder()
```

Builder methods:

```java
maxAttempts(int)
initialDelayMillis(long)
maxDelayMillis(long)
multiplier(double)
jitterFactor(double)
classifier(SqlRetryClassifier)
listener(TransactionRetryListener)
build()
```

Getters:

```java
maxAttempts()
initialDelayMillis()
maxDelayMillis()
multiplier()
jitterFactor()
classifier()
listener()
nextDelayMillis(int failedAttempt)
```

`maxAttempts` incluye el primer intento.

`none()` es el default de `TransactionOptions` y no reintenta.

`mysqlTransient()` usa:

```text
maxAttempts = 3
initialDelayMillis = 25
maxDelayMillis = 250
multiplier = 2.0
jitterFactor = 0.25
classifier = SqlRetryClassifier.mysqlTransient()
listener = TransactionRetryListener.noop()
```

## `SqlRetryClassifier`

```java
boolean isRetryable(SQLException exception)
```

Factories:

```java
never()
mysqlTransient()
```

`mysqlTransient()` cubre:

| Condición | Error code | SQLState habitual |
| --- | ---: | --- |
| Deadlock | `1213` | `40001` |
| Lock wait timeout | `1205` | `HY000` |

También revisa la cadena `SQLException#getNextException()`.

## `TransactionRetryListener`

```java
void onRetry(TransactionRetryEvent event)
```

Factory:

```java
noop()
```

El listener es observacional. Si falla, el retry continúa y el fallo del listener se agrega como `suppressed` al fallo del intento.

## `TransactionRetryEvent`

```java
int failedAttempt()
int maxAttempts()
long nextDelayMillis()
SQLException failure()
```

## `DatabaseException`

```java
DatabaseException(String message)
DatabaseException(String message, Throwable cause)
```
