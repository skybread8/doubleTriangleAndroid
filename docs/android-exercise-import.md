# Incorporar ejercicios de iOS a Android

Desde la raíz de Android, con Python 3.9 o posterior y el repositorio iOS actualizado en `../doubleTriangle`:

```powershell
python scripts/sync-ios-exercises.py --check
python scripts/sync-ios-exercises.py
```

`--check` informa de las entradas ausentes y devuelve código 1 si hay cambios pendientes. El script lee `Exercise.swift`, `ExerciseCatalog.swift` y `Localizable.xcstrings` de iOS; incorpora las entradas ausentes al catálogo y a los metadatos y guías de Android. No modifica iOS ni accede al backend o a la red. Se puede indicar otra ruta al directorio `WildforceShared/Sources/WildforceShared` con `--ios`.

El script conserva las entradas existentes, incluidos cambios locales: no sincroniza modificaciones de ejercicios ya importados ni elimina ejercicios. Hay que revisar esos cambios por separado. Detecta diferencias de estructura entre el enum y las definiciones antes de escribir. Las traducciones vacías o con caracteres de reemplazo usan inglés; deben revisarse en el origen. No incorpora imágenes ni actualiza los datos de referencia o la clasificación visual de nuevos músculos y equipamientos; comprueba estos elementos manualmente para cada incorporación. Tras ejecutarlo, revisa el diff y verifica la app y las traducciones antes de publicar.
