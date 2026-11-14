# Migración de traducciones Android al Sheet compartido

## Cambios aplicados

- El catálogo `sheet_translations.xml` se genera en `core:designsystem`, que ya
  es dependencia de la app y de las features. Lee las mismas dos pestañas que
  iOS: `app` y `exercise_catalog`.
- Las claves Android se forman directamente desde la clave del Sheet: minúsculas,
  espacios y caracteres no válidos se convierten en `_`; si empieza por número se
  antepone `key_`. No se añade ningún prefijo propio.
- Las claves duplicadas se conservan por primera aparición y el exportador avisa
  con `Write-Warning`, indicando la clave que se ha omitido.
- Los formatos de iOS se adaptan al formato Android: `%@` pasa a `%1$s`, `%ld` a
  `%1$d`, etc. Los argumentos quedan posicionales para que cada idioma pueda
  reordenarlos con seguridad.
- La navegación de la app todavía tiene recursos propios; queda pendiente
  conectarla al catálogo compartido y comprobarla en todos los locales.
- En `feature:workout` hay 39 aliases de compatibilidad: el código puede seguir
  usando sus `R.string.workout_*`, pero el texto final procede del Sheet.

## Criterio de migración

La fuente canónica de cada texto es la key literal usada por iOS. Los aliases de
Android preservan nombres históricos de `R.string` mientras resuelven a esa misma
key exportada desde el Sheet. La migración restante debe seguir esta referencia de
iOS, no comparar traducciones españolas de Android con el valor inglés del Sheet.

## Uso

La copia incluida se actualizó desde el Sheet el 10/11/2026. La presencia del catálogo
no garantiza que todas las pantallas lo utilicen: aún hay literales en código y
recursos específicos de Android que necesitan una revisión frente a iOS y al
Sheet. Hay 169 fallbacks sin clave compartida verificada: disponen de traducciones
locales para los diez idiomas, mantenidas en `scripts/android-fallback-translations.txt`.
El script las genera y comprueba sus parámetros; deben revisarse y sustituirse por
claves del Sheet cuando exista una equivalencia. Esto no implica que se hayan
localizado todos los textos escritos directamente en las pantallas.

Desde el repositorio Android, en un entorno con acceso de lectura al Sheet:

```powershell
.\scripts\sync-shared-translations.ps1
```

El script llama al exportador del backoffice sin modificar ese repositorio ni
la hoja. Descarta celdas vacías y errores de fórmula, corrige artefactos
conocidos del exportador y usa traducciones del catálogo local de iOS solo si
la celda correspondiente del Sheet está vacía y la clave y los parámetros
coinciden. El Sheet siempre tiene prioridad. El script también conserva un
valor base para la única celda inglesa vacía de la exportación del 23 de
octubre. Para comprobar una copia ya
descargada sin acceder a la red, ejecuta `sync-shared-translations.ps1
-SkipDownload`.

Después de añadir una clave al Sheet, usa el nombre Android normalizado que informa
el exportador (o un alias de compatibilidad si el código aún conserva un nombre
histórico).

## Auditoría de la copia del 14/11/2026

La exportación de solo lectura del Sheet confirmó que japonés, neerlandés y
chino simplificado contienen numerosas celdas vacías en claves utilizadas por
Android. Android recurre al recurso base cuando falta una celda; esto no demuestra
que la interfaz esté completamente traducida. La verificación de los 169 textos
específicos de Android tampoco mide la cobertura de todas las claves del Sheet.

Diez textos de onboarding proceden de claves de iOS que no existen en el Sheet.
Sus traducciones locales se mantienen en `scripts/onboarding-local-fallbacks.json`
y se generan mediante `sync-onboarding-sheet-map.ps1`; cuando el Sheet contiene
una clave equivalente, se utiliza el recurso compartido.
