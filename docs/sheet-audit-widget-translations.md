# Auditoría de traducciones: widget de entrenamiento

La exportación local del Sheet no contiene claves exactas para los mensajes de estado vacío del widget. Android mantiene traducciones locales en `app/src/main/res/values*/widget_localization.xml` mientras se revisa la hoja en directo:

- `Create your first workout`
- `Recover and come back tomorrow`

El widget usa también `Day`, `No plan`, `Planned Workout`, `Rest Day`, `Completed` y `Skipped` del catálogo compartido. En la exportación local faltan las celdas de japonés, neerlandés y chino simplificado para algunas de estas claves; deben comprobarse antes de actualizar el Sheet.
