# PRD — NetworkPlayerSettings

## 1. Resumen

**NetworkPlayerSettings** será el sistema centralizado de preferencias del jugador dentro de la network de Minecraft.

Inicialmente su función principal será gestionar el **idioma del jugador**, permitiendo mostrar mensajes, menús, scoreboards, tabs y otros elementos en español o inglés.

El sistema debe diseñarse para crecer y soportar futuras preferencias como visibilidad de jugadores, sonidos, partículas, scoreboard, mensajes privados, invitaciones de party y notificaciones. y cualquier settngs que sea utilizado OJO settings que sean a nivel general osea que se apliquen en cada uno de los servidores

---

## 2. Objetivo del producto

Crear un módulo reutilizable que permita a todos los plugins de la network consultar y modificar las preferencias personales de cada jugador desde una única fuente central.

El objetivo es evitar que cada plugin maneje configuraciones por separado.

---

## 3. Problema a resolver

Actualmente, si cada sistema maneja sus propias preferencias, se generan problemas como:

- Datos duplicados.
- Lógica repetida.
- Inconsistencias entre servidores.
- Dificultad para agregar nuevas opciones.
- Mala experiencia para jugadores multilenguaje.

Ejemplo del problema:

```txt
LobbyPlugin guarda idioma
ScoreboardPlugin guarda idioma
TabPlugin guarda idioma
PartyPlugin guarda preferencias
```

Esto no escala.

Cre
---

## 4. Solución propuesta
ar un plugin/módulo central llamado:

```txt
NetworkPlayerSettings
```

Este módulo será responsable de:

- Cargar preferencias del jugador.
- Guardar preferencias en base de datos.
- Mantener preferencias en cache.
- Exponer una API para otros plugins.
- Permitir cambiar preferencias desde menús.
- Notificar cambios a otros sistemas.

---

## 5. Alcance inicial MVP

La primera versión debe incluir:

- Sistema de idioma del jugador.
- Soporte para español e inglés.
- Detección inicial del idioma de Minecraft.
- Comando `/globalsettings`.
- Menú para seleccionar idioma.
- Guardado persistente en base de datos.
- API interna para consultar el idioma y tambien para poder modificar desde otros plugins las preferncias de los jugadores de cualkqueir prefenrecia en este caso del idioma.
- Actualización inmediata del idioma al cambiarlo y eficiente.

---

## 6. Fuera de alcance inicial

Para la primera versión no se implementará:

- Traducción completa de todos los plugins.
- Sincronización avanzada con Redis.
- Plugin Velocity.
- Soporte para más idiomas aparte de español e inglés.

Estas funcionalidades quedan para fases posteriores.

---

## 7. Usuarios principales

### Jugador

Quiere configurar su experiencia personal dentro de la network.

Ejemplo:

```txt
Elegir idioma español o inglés.
```

### Desarrollador

Quiere consultar preferencias sin duplicar lógica.

Ejemplo:

```java
Language language = settingsService.getLanguage(player.getUniqueId());
```

### Administrador de la network

Quiere que la experiencia sea consistente en todos los servidores.

---

## 8. Preferencias soportadas

### MVP

```txt
language = auto / es / en
```

### Futuro

```txt
scoreboard_enabled = true / false
sounds_enabled = true / false
particles_enabled = true / false
private_messages_enabled = true / false
party_invites_enabled = true / false
player_visibility = all / friends / none
notifications_enabled = true / false
chat_filter_enabled = true / false
```

---

## 9. Flujo principal

### Al entrar el jugador

```txt
Jugador entra
   ↓
NetworkPlayerSettings carga datos desde DB
   ↓
Guarda settings en cache
   ↓
Si no tiene idioma definido, detecta idioma de Minecraft
   ↓
Los plugins ya pueden consultar sus preferencias
```

### Al cambiar idioma

```txt
Jugador ejecuta /globalsettings
   ↓
Se abre menú de configuraciones y selecciona idiomas 
   ↓
Jugador selecciona Español o English
   ↓
Se actualiza cache
   ↓
Se guarda en DB
```

---

## 11. Menú de idioma

El menú debe mostrar:

```txt
Selecciona tu idioma / Choose your language

[Español]
[English]
[Automático]
```

Opciones:

```txt
Español    → language = es
English    → language = en
Automático → language = auto
```

---

## 12. Reglas de idioma

Si el jugador tiene idioma manual:

```txt
language = es → usar español
language = en → usar inglés
```

Si el jugador tiene idioma automático:

```txt
Minecraft locale es_* → español
Minecraft locale en_* → inglés
Otro locale → inglés por defecto
```

Ejemplos:

```txt
es_es → es
es_mx → es
es_pe → es
en_us → en
en_gb → en
fr_fr → en
```

---

## 13. Arquitectura técnica

OJO ESTO ES LAGO CONCEPTUAL QUE USAMOS EN EL EQUIPO PARA ATERRIZAR ESTA IDEA A ALGO QUE PODAMSO ENTENDENR MEJOR TU PUEDES RECIDIR LA ARQUITECUTRA TECNICA FINAL SOLAMENTE ESTAMOS HACIENDO UN BASE INICIAL TIENES QUE EVALUARLA

```txt
NetworkPlayerSettings
 ├── PlayerSettingsService
 ├── PlayerSettingsRepository
 ├── PlayerSettingsCache
 ├── LanguageService
 ├── LanguageMenu
 └── SettingsEvents
```

### PlayerSettingsService

Capa principal usada por otros plugins.

Responsabilidades:

- Obtener settings del jugador.
- Cambiar settings.
- Validar valores.
- Disparar eventos internos.

### PlayerSettingsRepository

Responsable de persistencia.

Responsabilidades:

- Cargar desde DB.
- Guardar en DB.

- Crear datos por defecto.
### PlayerSettingsCache

Responsable de rendimiento.

Responsabilidades:

- Mantener settings en memoria.
- Evitar consultas repetidas a DB.
- Limpiar datos al salir el jugador.

### LanguageService

Responsable de resolver el idioma final del jugador.

Responsabilidades:

- Leer idioma manual.
- Detectar locale de Minecraft.
- Aplicar fallback.

---


## 16. Eventos internos

El sistema debe emitir eventos cuando una preferencia cambie.

Ejemplo:

```txt
PlayerSettingChangeEvent
```

Datos mínimos del evento:

```txt
player_uuid
setting_key
old_value
new_value
```

Ejemplo:

```txt
player_uuid = xxx
setting_key = language
old_value = es
new_value = en
```

Esto permite que otros sistemas reaccionen.

Ejemplo:

```txt
ScoreboardPlugin escucha cambio de idioma
   ↓
Actualiza scoreboard del jugador
```

---

## 17. PlaceholderAPI

tambien debemos registrar soporta para poder obtener esto mediante PlaceholderAPI haciendo eficiente
---

## 20. Resultado esperado

Al finalizar, la network tendrá una fuente única para responder:

```txt
¿Qué preferencias tiene este jugador?
```

Y cualquier plugin podrá usar esa información de forma limpia, consistente y escalable.
