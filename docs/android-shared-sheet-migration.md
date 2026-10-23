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
- La navegación de la app (`workout`, `nutrition`, `analytics`, `gym`, `profile`)
  y `cancel` ya resuelven desde el catálogo compartido en todos los locales.
- En `feature:workout` hay 39 aliases de compatibilidad: el código puede seguir
  usando sus `R.string.workout_*`, pero el texto final procede del Sheet.

## Criterio de migración

La fuente canónica de cada texto es la key literal usada por iOS. Los aliases de
Android preservan nombres históricos de `R.string` mientras resuelven a esa misma
key exportada desde el Sheet. La migración restante debe seguir esta referencia de
iOS, no comparar traducciones españolas de Android con el valor inglés del Sheet.

## Uso

Desde `wildforce-back`:

```powershell
.\scripts\export-android-translations.ps1 `
  -AndroidResPath F:\Git\doubleTriangleAndroid\core\designsystem\src\main\res
```

Después de añadir una clave al Sheet, usa el nombre Android normalizado que informa
el exportador (o un alias de compatibilidad si el código aún conserva un nombre
histórico).
