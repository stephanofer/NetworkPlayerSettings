# Módulo de configuración de NetworkPlatform

## Propósito

El módulo de configuración de `network-platform-paper` resuelve la carga, registro, guardado, recarga y actualización de archivos YAML para plugins consumidores de la network usando **BoostedYAML** como motor real.

La idea central del módulo es:

- dar una DX simple para el caso común;
- conservar acceso total al `YamlDocument` real;
- evitar boilerplate repetido;
- proteger el `dataFolder` del plugin contra rutas inválidas;
- ofrecer operaciones síncronas y asíncronas;
- coordinar lifecycle y concurrencia sin esconder el poder de BoostedYAML.

---

## Ubicación en el proyecto

Código principal:

```text
network-platform-paper/src/main/java/com/stephanofer/networkplatform/paper/config/
├─ ConfigService.java
├─ ConfigFileSpec.java
├─ LoadedConfig.java
└─ ConfigException.java

network-platform-paper/src/main/java/com/stephanofer/networkplatform/paper/config/internal/
├─ PaperConfigService.java
├─ ConfigPathResolver.java
└─ RegisteredConfig.java
```

Integración con la entrada principal:

```text
network-platform-paper/src/main/java/com/stephanofer/networkplatform/paper/NetworkPlatform.java
```

Tests:

```text
network-platform-paper/src/test/java/com/stephanofer/networkplatform/paper/config/ConfigFileSpecTest.java
network-platform-paper/src/test/java/com/stephanofer/networkplatform/paper/config/internal/ConfigPathResolverTest.java
network-platform-paper/src/test/java/com/stephanofer/networkplatform/paper/config/internal/PaperConfigServiceTest.java
```

---

## Arquitectura general

El módulo se divide en cuatro piezas principales:

1. **API pública**
   - `ConfigService`
   - `ConfigFileSpec`
   - `LoadedConfig`
   - `ConfigException`

2. **Implementación interna**
   - `PaperConfigService`

3. **Seguridad de rutas**
   - `ConfigPathResolver`

4. **Registro y lock por archivo**
   - `RegisteredConfig`

Flujo de alto nivel:

```text
Plugin consumidor
   -> NetworkPlatform.create(this)
   -> platform.configs()
   -> PaperConfigService
      -> valida path
      -> resuelve archivo dentro del dataFolder
      -> decide si usa defaults del JAR o no
      -> crea YamlDocument con BoostedYAML
      -> registra LoadedConfig en memoria
      -> expone siempre la misma instancia mientras siga cargado
```

---

## Integración con `NetworkPlatform`

`NetworkPlatform` expone el servicio a través de `configs()`.

Comportamiento actual:

- inicialización lazy;
- una sola instancia por `NetworkPlatform`;
- registro automático del shutdown en `lifecycle`.

Uso:

```java
NetworkPlatform platform = NetworkPlatform.create(this);

var configs = platform.configs();
```

Internamente, cuando el servicio se crea:

- se llama `ensureActive()`;
- se instancia `PaperConfigService` con `PlatformContext`;
- se registra `configService::shutdown` en `PlatformLifecycle`.

Esto garantiza que el executor del módulo y su registry se cierren cuando el plugin consumidor llame:

```java
platform.shutdown();
```

---

## Filosofía de uso con BoostedYAML

Este módulo **no reemplaza** BoostedYAML.

El consumidor sigue trabajando con:

```java
YamlDocument
```

Por lo tanto, el módulo no agrega wrappers para:

- getters;
- setters;
- secciones;
- update logic;
- serializers;
- relocations;
- ignored routes;
- value mappers.

Todo eso sigue siendo responsabilidad y poder del `YamlDocument` real.

### Qué agrega NetworkPlatform

- registro centralizado;
- path safety;
- metadata de configs cargadas;
- lifecycle;
- async I/O;
- errores más claros;
- API de carga consistente.

---

## Empaquetado y relación con BoostedYAML

`network-platform-paper` **vendoriza** BoostedYAML.

En `build.gradle.kts` del módulo se usa Shadow y relocation:

```kotlin
tasks.shadowJar {
    archiveClassifier.set("")
    configurations = listOf(shade)
    relocate("dev.dejvokep.boostedyaml", "com.stephanofer.networkplatform.paper.libs.boostedyaml")
}
```

### Qué significa esto

- el plugin consumidor no necesita declarar BoostedYAML por separado;
- el `shadowJar` final de `network-platform-paper` incluye BoostedYAML;
- las clases terminan relocalizadas bajo:

```text
com/stephanofer/networkplatform/paper/libs/boostedyaml/
```

### Estado actual verificado

Se verificó el artifact generado:

```text
network-platform-paper/build/libs/network-platform-paper-1.0.1-SNAPSHOT.jar
```

y contiene clases como:

```text
com/stephanofer/networkplatform/paper/libs/boostedyaml/YamlDocument.class
com/stephanofer/networkplatform/paper/libs/boostedyaml/settings/loader/LoaderSettings.class
com/stephanofer/networkplatform/paper/libs/boostedyaml/settings/updater/UpdaterSettings.class
```

### Importante sobre el código fuente

En el código fuente Java del módulo, la implementación compila actualmente contra el package original de BoostedYAML (`dev.dejvokep...`), pero el artifact shadeado final lo reloca al package interno de NetworkPlatform.

En términos prácticos, el empaquetado final del módulo ya protege al consumidor contra conflictos de classloader, que es exactamente lo que recomienda la propia librería.

---

## API pública

## `ConfigService`

Interfaz principal del módulo.

```java
public interface ConfigService {
    YamlDocument load(String path);
    YamlDocument load(ConfigFileSpec spec);
    CompletableFuture<YamlDocument> loadAsync(String path);
    CompletableFuture<YamlDocument> loadAsync(ConfigFileSpec spec);
    Optional<YamlDocument> find(String path);
    YamlDocument get(String path);
    boolean isLoaded(String path);
    Collection<LoadedConfig> loaded();
    void reload(String path);
    void reloadAll();
    CompletableFuture<Void> reloadAsync(String path);
    CompletableFuture<Void> reloadAllAsync();
    void save(String path);
    void saveAll();
    CompletableFuture<Void> saveAsync(String path);
    CompletableFuture<Void> saveAllAsync();
    boolean update(String path);
    void updateAll();
    CompletableFuture<Boolean> updateAsync(String path);
    CompletableFuture<Void> updateAllAsync();
    void unload(String path);
    void clear();
    void shutdown();
}
```

### Responsabilidades de cada grupo de métodos

#### Carga

- `load(String path)`
- `load(ConfigFileSpec spec)`

Crean o recuperan un `YamlDocument` registrado. Si la config ya está cargada, devuelven la misma instancia.

#### Carga asíncrona

- `loadAsync(String path)`
- `loadAsync(ConfigFileSpec spec)`

Ejecutan la carga sobre el executor interno single-thread del módulo.

#### Búsqueda y acceso

- `find(String path)` devuelve `Optional<YamlDocument>`;
- `get(String path)` falla si la config no está cargada;
- `isLoaded(String path)` responde si existe en registry;
- `loaded()` devuelve metadata de todas las configs cargadas.

#### Persistencia y refresh

- `reload(...)`
- `save(...)`
- `update(...)`

Todas estas operaciones se hacen sobre el `YamlDocument` ya registrado.

#### Gestión del registry

- `unload(path)` remueve una sola config del registro;
- `clear()` vacía el registro completo;
- `shutdown()` limpia registry y apaga el executor.

---

## `ConfigFileSpec`

`ConfigFileSpec` describe cómo debe cargarse una config.

Campos actuales:

- `path`
- `resource`
- `usePathAsDefaultResource`
- `failIfResourceMissing`
- `replaceExisting`
- `settings`

### Modos de resource soportados

El spec permite tres estrategias explícitas.

#### 1. Resource por defecto usando el mismo path

```java
ConfigFileSpec.builder("config.yml").build();
```

Significa:

- `path = "config.yml"`
- `resource = null`
- `usePathAsDefaultResource = true`
- `effectiveResourcePath() = "config.yml"`

Es el modo por defecto del caso simple.

#### 2. Resource custom

```java
ConfigFileSpec.builder("module/player/config.yml")
    .resource("defaults/player.yml")
    .build();
```

Significa:

- `path = "module/player/config.yml"`
- `resource = "defaults/player.yml"`
- `usePathAsDefaultResource = false`
- `effectiveResourcePath() = "defaults/player.yml"`

#### 3. Sin defaults resource

```java
ConfigFileSpec.builder("runtime/cache.yml")
    .withoutResource()
    .build();
```

Significa:

- `path = "runtime/cache.yml"`
- `resource = null`
- `usePathAsDefaultResource = false`
- `effectiveResourcePath() = null`

En este caso el servicio crea el `YamlDocument` sin intentar abrir nada del JAR.

### Builder disponible

```java
ConfigFileSpec.builder("kits/iron.yml")
    .resource("kits/iron.yml")
    .withoutResource()
    .failIfResourceMissing(false)
    .replaceExisting(false)
    .settings(...)
    .generalSettings(...)
    .loaderSettings(...)
    .dumperSettings(...)
    .updaterSettings(...)
    .autoUpdate(true)
    .versionRoute("config-version")
    .build();
```

### Shortcuts que agrega el spec

#### `autoUpdate(true)`

Si el builder recibe `autoUpdate(true)`, crea un `LoaderSettings` derivado usando:

```java
LoaderSettings.builder(existingOrDefault)
    .setAutoUpdate(true)
    .build();
```

#### `versionRoute("config-version")`

Si el builder recibe `versionRoute(...)`, crea un `UpdaterSettings` derivado usando:

```java
UpdaterSettings.builder(existingOrDefault)
    .setVersioning(new BasicVersioning("config-version"))
    .build();
```

### Cómo se resuelven settings

El spec siempre termina con exactamente 4 settings:

1. `GeneralSettings`
2. `LoaderSettings`
3. `DumperSettings`
4. `UpdaterSettings`

Si no se pasan explícitamente, usa:

```java
GeneralSettings.DEFAULT
LoaderSettings.DEFAULT
DumperSettings.DEFAULT
UpdaterSettings.DEFAULT
```

### Validaciones del spec

`ConfigFileSpec` rechaza:

- `path` blank;
- `resource` blank;
- `versionRoute` blank;
- cualquier settings que no sea uno de los 4 tipos reconocidos;
- `replaceExisting(true)` si no existe `effectiveResourcePath()`;
- `failIfResourceMissing(true)` si no existe `effectiveResourcePath()`.

---

## `LoadedConfig`

`LoadedConfig` expone metadata de cada config registrada.

```java
public record LoadedConfig(
    String path,
    Path file,
    String resourcePath,
    boolean defaultsResourcePresent,
    YamlDocument document
) {}
```

### Significado de cada campo

#### `path`
Ruta normalizada relativa al `dataFolder`.

#### `file`
Ruta absoluta/resuelta dentro del `dataFolder` real del plugin consumidor.

#### `resourcePath`
Resource efectivo que se intentó usar para defaults.

Puede ser:

- el mismo path lógico;
- un resource custom;
- `null` si el spec fue creado con `withoutResource()`.

#### `defaultsResourcePresent`
Indica si el resource realmente existió dentro del JAR en el momento de cargar.

Casos:

- `true`: el defaults resource se encontró y fue usado;
- `false`: no existía o el spec no usaba defaults resource.

#### `document`
Instancia real de `YamlDocument` registrada.

---

## `ConfigException`

Excepción runtime del módulo.

Se usa para:

- errores de path;
- config no cargada;
- fallo al leer resource;
- fallo al cargar YAML;
- fallo al guardar;
- fallo al actualizar;
- combinaciones inválidas relacionadas con replace/defaults.

Ejemplos de mensajes reales del código:

```text
Invalid config path '../../server.properties': path traversal is not allowed
Config 'config.yml' is not loaded
Missing config resource 'config.yml' for 'config.yml'
Failed to load config 'kits/iron.yml' at '...'
Failed to save config 'config.yml' at '...'
```

---

## Implementación interna

## `PaperConfigService`

Es la implementación real de `ConfigService`.

### Estado interno

Mantiene:

- `PlatformContext context`
- `JavaPlugin plugin`
- `ConcurrentHashMap<String, RegisteredConfig> configs`
- `ExecutorService executor`
- `AtomicBoolean shutdown`

### Registry interno

El registry usa como key el path normalizado:

```text
config.yml
kits/iron.yml
module/player/config.yml
```

Cada entrada es un `RegisteredConfig` con:

- `LoadedConfig`
- `ConfigFileSpec`
- `ReentrantLock`

### Garantía de idempotencia

Si una config ya fue cargada:

```java
service.load("config.yml")
service.load(ConfigFileSpec.builder("config.yml").build())
```

devuelven la misma instancia de `YamlDocument`.

Esto evita romper referencias guardadas por el plugin consumidor.

---

## Seguridad de rutas

`ConfigPathResolver` es el componente responsable de validar y resolver rutas.

### Qué valida `normalizeConfigPath(...)`

Rechaza:

- `null`
- string vacío o solo espacios
- paths terminados en `/`
- paths absolutos
- segmentos `..`
- extensiones que no sean `.yml` o `.yaml`

Además normaliza:

- `\` -> `/`
- segmentos redundantes del path

### Qué valida `resolveDataFile(...)`

Resuelve el archivo relativo contra:

```java
platform.context().dataDirectory()
```

y rechaza cualquier resultado que escape del `dataFolder`.

---

## Flujo real de carga

## `load(String path)`

1. crea `ConfigFileSpec.builder(path).build()`;
2. valida que el servicio siga activo;
3. normaliza el path;
4. busca si ya existe en el registry;
5. si ya existe, devuelve el mismo `YamlDocument`;
6. si no existe, crea un `RegisteredConfig` nuevo;
7. guarda esa entrada en `ConcurrentHashMap`.

## Creación interna de una config nueva

`createRegisteredConfig(...)` hace:

1. resuelve el archivo real en disco;
2. resuelve el modelo de resource;
3. crea el `YamlDocument`;
4. arma `LoadedConfig` con metadata;
5. arma `RegisteredConfig` con lock por archivo.

---

## Resolución de defaults resource

`loadResource(...)` sigue esta lógica.

### Caso A — no hay resource efectivo

Si `effectiveResourcePath() == null`:

- no se abre ningún resource del JAR;
- se devuelve `ResourcePayload.notConfigured()`;
- luego se usa `YamlDocument.create(file, settings...)`.

### Caso B — hay resource efectivo y existe

Si el JAR del plugin contiene el resource:

- se lee completo a bytes;
- se marca `defaultsResourcePresent = true`;
- luego se crea el documento con:

```java
YamlDocument.create(file.toFile(), defaults, settings)
```

### Caso C — hay resource efectivo pero no existe

Si el resource no está en el JAR:

- si `failIfResourceMissing == true`, lanza `ConfigException`;
- si no, sigue sin defaults.

En ese caso:

- `resourcePath` queda registrado;
- `defaultsResourcePresent = false`.

---

## Creación del `YamlDocument`

`createDocument(...)` soporta dos rutas.

### Con defaults

```java
YamlDocument.create(file.toFile(), defaults, settings)
```

### Sin defaults

```java
YamlDocument.create(file.toFile(), settings)
```

### `replaceExisting(true)`

Si `replaceExisting == true` y hay resource presente:

1. crea carpetas padre si hacen falta;
2. copia el resource al archivo destino usando `Files.copy(..., REPLACE_EXISTING)`;
3. recién después crea el `YamlDocument`.

Si `replaceExisting == true` pero no hay resource real presente:

- falla con `ConfigException`.

---

## Reload, save y update

Todas estas operaciones trabajan sobre la instancia ya registrada.

## `reload(path)`

- busca la config en registry;
- toma el lock de esa config;
- llama `document.reload()`;
- si BoostedYAML tiene defaults y auto-update habilitado, el comportamiento queda delegado a BoostedYAML.

### Garantía importante

`reload()` conserva la misma instancia de `YamlDocument`.

Eso permite que un plugin consumidor haga esto con seguridad:

```java
YamlDocument config = platform.configs().load("config.yml");
platform.configs().reload("config.yml");
// config sigue siendo la misma referencia
```

## `save(path)`

- toma el lock;
- llama `document.save()`;
- si falla, envuelve el error en `ConfigException`.

## `update(path)`

- toma el lock;
- llama `document.update()`;
- devuelve `boolean` exactamente como lo hace BoostedYAML.

### Semántica importante

Si no hay defaults asociados:

```java
document.update() == false
```

Eso **no se trata como error**.

---

## Concurrencia

Cada config registrada tiene su propio:

```java
ReentrantLock
```

Esto se usa en:

- `reload`
- `save`
- `update`

### Qué evita

- `save` mientras `reload` del mismo archivo;
- `update` mientras `save` del mismo archivo;
- dos `reload` simultáneos sobre la misma config;
- corrupción por I/O concurrente del mismo `YamlDocument`.

No se usan locks globales, por lo que el registry sigue siendo liviano.

---

## Async I/O

El módulo usa:

```java
Executors.newSingleThreadExecutor(...)
```

con un thread daemon llamado:

```text
NetworkPlatform-Config-<PluginName>
```

### Por qué single-thread

Es la decisión actual del código para:

- mantener orden predecible;
- evitar varias operaciones de I/O compitiendo entre sí;
- simplificar el modelo mental;
- reducir riesgo de carreras en runtime.

### Métodos asíncronos disponibles

- `loadAsync`
- `reloadAsync`
- `reloadAllAsync`
- `saveAsync`
- `saveAllAsync`
- `updateAsync`
- `updateAllAsync`

Todos estos delegan trabajo al executor interno.

### Relación con Paper

Este diseño existe porque la doc local de Paper recomienda evitar I/O bloqueante en el main thread. Por eso el módulo ofrece APIs sync y async separadas.

- sync: útil en `onEnable()`;
- async: útil en runtime, comandos o tareas administrativas.

---

## Lifecycle y shutdown

Cuando el servicio es creado desde `NetworkPlatform`:

```java
this.lifecycle.onShutdown(configService::shutdown)
```

### `shutdown()` del servicio

Hace:

1. marca el servicio como apagado con `AtomicBoolean`;
2. limpia el registry;
3. apaga el executor.

Después de eso, nuevas operaciones de carga fallan con:

```java
IllegalStateException("ConfigService is already shut down")
```

---

## Ejemplos de uso real

## Caso simple

```java
NetworkPlatform platform = NetworkPlatform.create(this);

YamlDocument config = platform.configs().load("config.yml");
YamlDocument kits = platform.configs().load("kits/iron.yml");
YamlDocument player = platform.configs().load("module/player/config.yml");
```

Comportamiento:

- busca defaults con el mismo path dentro del JAR;
- si existen, los usa;
- si no existen, crea un archivo vacío con BoostedYAML;
- registra cada config por path normalizado.

## Caso con auto-update y versionado

```java
YamlDocument config = platform.configs().load(
    ConfigFileSpec.builder("config.yml")
        .autoUpdate(true)
        .versionRoute("config-version")
        .build()
);
```

## Caso con settings de BoostedYAML explícitos

```java
YamlDocument config = platform.configs().load(
    ConfigFileSpec.builder("custom.yml")
        .settings(
            GeneralSettings.DEFAULT,
            LoaderSettings.builder()
                .setAutoUpdate(true)
                .build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder()
                .setVersioning(new BasicVersioning("config-version"))
                .build()
        )
        .build()
);
```

## Caso sin defaults resource

```java
YamlDocument runtime = platform.configs().load(
    ConfigFileSpec.builder("runtime/cache.yml")
        .withoutResource()
        .build()
);
```

## Caso de reset destructivo desde defaults

```java
YamlDocument config = platform.configs().load(
    ConfigFileSpec.builder("config.yml")
        .replaceExisting(true)
        .build()
);
```

Esto sobrescribe el archivo del `dataFolder` usando el defaults resource antes de cargar.

---

## Qué hacer y qué no hacer

## Hacer

- usar `load("config.yml")` para el caso simple;
- usar `ConfigFileSpec` cuando necesites customizar defaults o settings;
- usar métodos async para operaciones de runtime sensibles al tick;
- guardar la referencia del `YamlDocument` si el plugin la necesita a largo plazo;
- usar `withoutResource()` cuando el archivo no tenga defaults dentro del JAR.

## No hacer

- pasar rutas con `..`;
- usar rutas absolutas;
- asumir que `update()` lanza error si no hay defaults;
- usar `replaceExisting(true)` si el archivo no tiene defaults resource real;
- asumir que `resourcePath != null` implica que el resource existía;
- crear tu propio manager paralelo de los mismos archivos si ya usás `ConfigService`.

---

## Edge cases cubiertos por la implementación actual

- archivo inexistente;
- carpetas anidadas;
- config ya cargada;
- recarga manteniendo referencia estable;
- config sin defaults;
- resource custom;
- resource faltante tolerado;
- resource faltante obligatorio;
- replace destructive;
- YAML corrupto;
- path traversal;
- rutas absolutas;
- extensiones inválidas;
- operaciones async;
- shutdown del servicio.

---

## Qué revelan los tests

Los tests actuales verifican explícitamente:

- defaults por path;
- `autoUpdate` + `versionRoute`;
- `withoutResource()`;
- validaciones del builder;
- duplicate load devolviendo misma instancia;
- nested directories;
- `reload()` in-place;
- `update()` sin defaults devolviendo `false`;
- `find()`;
- `save()` persistiendo cambios reales en disco;
- metadata de `loaded()`;
- shutdown;
- errores async envueltos correctamente.

Esto convierte a los tests en una fuente confiable para entender el contrato actual del módulo.

---

## Archivos más importantes para extender esta parte

- `NetworkPlatform.java` — punto de entrada del servicio.
- `ConfigService.java` — contrato público.
- `ConfigFileSpec.java` — modelo de carga y settings.
- `PaperConfigService.java` — flujo real de carga/recarga/guardado/update.
- `ConfigPathResolver.java` — seguridad de paths.
- `RegisteredConfig.java` — lock y metadata por config.

---

## Resumen final

Hoy el módulo de config de NetworkPlatform ofrece una capa base profesional para configs YAML de plugins Paper:

- simple en el caso común;
- flexible en casos avanzados;
- segura respecto al `dataFolder`;
- compatible con runtime async;
- alineada con el lifecycle del plugin;
- sin esconder el poder real de BoostedYAML.

Su valor no está en reemplazar BoostedYAML, sino en integrarlo de forma consistente, segura y reutilizable para todos los plugins consumidores de la network.
