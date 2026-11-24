# Auditoría de traducciones: entrenamiento

Revisión de la copia del Sheet incluida en Android, sin consultar la hoja en directo. Las celdas pueden haber cambiado desde la última exportación.

## Celdas que necesitan corrección

| Clave del Sheet | Idioma | Problema | Propuesta |
| --- | --- | --- | --- |
| `Just Right` | es | Contiene `Just Right` en inglés. | `Justo` |
| `Complete Set %i` | es | `Serie %1$d completada` describe una acción ya terminada. | `Completar serie %i` |
| `Complete Set %i` | de | `Satz %1$d abgeschlossen` describe una acción ya terminada. | `Satz %i abschließen` |
| `Complete Set %i` | fr | `Série %1$d terminée` describe una acción ya terminada. | `Terminer la série %i` |
| `Complete Set %i` | it | `Serie %1$d completata` describe una acción ya terminada. | `Completa la serie %i` |
| `Complete Set %i` | pt | `Série %1$d concluída` describe una acción ya terminada. | `Concluir série %i` |

Android usa valores locales para estas acciones; el formato `%1$d` es el generado para Android a partir de `%i`.

## Nombres del catálogo de ejercicios

En la comparación de los 134 nombres ingleses del catálogo local de Android con los recursos exportados no aparecen claves normalizadas para `Lying Leg Curl` ni `Dumbbell Chest Fly`. Esos nombres también figuran como `englishName` en los metadatos de iOS. Confirmar si el Sheet usa otras claves equivalentes antes de añadir filas; Android dispone de nombres locales para estos ejercicios. Esta comparación no verifica la calidad ni la cobertura de todos los idiomas.

Las descripciones accesibles `Workout path`, `Exercise insights` y `Exercise guide` no aparecen como claves exactas en la exportación local. Android las conserva en `workout_toolbar_localization.xml` en los diez idiomas.

## Celdas españolas que contienen inglés

En la copia exportada, `Active`, `Completed`, `Skipped`, `Best weight`, `Best volume` y `Continue` tienen el valor inglés también en la columna española. Android usa los recursos `ui_sheet_*` como alternativa hasta corregir esas celdas en el Sheet.

Las claves `Loading exercises`, `Finding substitutes and exercises for the same muscle group.` y `There are no other exercises for the same muscle group.` no figuran con ese texto exacto en la exportación local; Android mantiene alternativas en `exercise_related_localization.xml` para los diez idiomas.

Las claves de motivos de feedback de iOS `Too expensive`, `Not useful enough`, `Too hard to use`, `Missing features`, `Technical problems / bugs` y `Just trying something else` no figuran con ese texto exacto en la exportación local. Android conserva alternativas en los diez idiomas en `feedback_reason_localization.xml`.
