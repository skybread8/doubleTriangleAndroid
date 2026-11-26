# Novedades de Android

Registro de funcionalidades y mejoras visibles para las personas que usan la app. Sirve como fuente para preparar futuros textos de «Novedades» y changelogs; no implica que esas funcionalidades ya se hayan publicado.

## Cómo mantener este registro

- Añadir una entrada por cada funcionalidad nueva o mejora relevante completada, en «Pendientes de publicación», dentro de la misma tarea y commit que el cambio.
- Escribir en español claro, centrado en lo que la persona puede hacer o percibir. Indicar la fecha, el área y cualquier condición relevante (por ejemplo, disponibilidad limitada). Añadir referencias útiles al cambio cuando existan.
- Registrar únicamente cambios comprobados; señalar las limitaciones o verificaciones pendientes. No incluir detalles sensibles ni prometer funciones aún no implementadas.
- Al preparar una versión, agrupar las entradas publicadas bajo su versión y fecha reales, conservando el historial. No asignar versiones ni marcar entradas como publicadas sin confirmación.
- Las correcciones pequeñas y los cambios exclusivamente internos no necesitan una entrada, salvo que sean relevantes para comunicar a usuarios.
- Mantener actualizado el borrador de Google Play en catalán (`ca`), inglés estadounidense (`en-US`) y español de España (`es-ES`), con etiquetas en líneas separadas y un máximo de 500 caracteres Unicode por idioma. Si no cabe todo, priorizar las novedades más relevantes y conservar el detalle en el registro. Confirmar qué cambios incluye la versión antes de publicar. [Formato de Google Play](https://support.google.com/googleplay/android-developer/answer/9859348?hl=es).

## Pendientes de publicación

- **26-11-2026 · Novedades:** una ventana de bienvenida presenta mejoras destacadas con una ilustración animada. Aparece una vez por selección de novedades y puede cerrarse para continuar. La selección inicial está disponible en español, catalán e inglés; los demás idiomas usan inglés. Pendiente de comprobación visual en un dispositivo. Referencia: `WhatsNew.kt`.

- **26-11-2026 · Apariencia:** retocado el efecto de cristal roto del icono alternativo, también en su variante navideña. Pendiente de comprobación visual en un dispositivo. Referencia: `broken_icon_detail.xml`.
- **26-11-2026 · Entrenamiento:** añadidos al selector el curl con barra Z y las rotaciones de cuello, y completadas fichas de ejercicios existentes. Algunas traducciones recurren al inglés cuando no están disponibles en iOS. Referencia: `scripts/sync-ios-exercises.py`.

## Inventario retrospectivo: publicación por confirmar

Revisión realizada el 26-11-2026. La presencia en el código no confirma que un cambio se haya publicado ni en qué versión. Las fechas de los cambios confirmados corresponden al commit citado; las de los cambios sin commit son las de esta revisión. Estas entradas deben contrastarse con las versiones publicadas antes de utilizarlas como novedades de una versión.

### Cambios confirmados en el repositorio

- **14-11-2026 · Entrenamiento:** exportación de entrenamientos pendientes al calendario, con revisión de fechas y hora y opción de abrir o compartir el archivo. Referencia: `c72d255`.
- **26-08-2026 · Entrenamiento:** seguimiento del entrenamiento activo mediante la notificación y Live Updates en dispositivos Android compatibles, con progreso y avisos de finalización del descanso. Referencias: `7e1d709`, `22a864d`.
- **24-08-2026 · Recordatorios:** avisos programados para entrenamientos y nutrición, sujetos a la configuración y los permisos de notificaciones. Referencias: `6f6e6c6`, `8292519`.
- **24-08-2026 · Nutrición:** consulta del historial por día y organización de los registros por tipo de comida. Referencias: `befee56`, `13cc01b`.
- **24-08-2026 · Nutrición:** edición de comidas registradas y personalización de objetivos nutricionales. Referencias: `9631a4a`, `b3571d9`.
- **24-08-2026 · Nutrición:** resumen de adherencia de siete días y mejoras en la navegación por fechas y el resumen de macronutrientes. Referencias: `c051fd9`, `df35673`.

### Cambios presentes en el árbol de trabajo, aún sin commit

Revisados en el código, sin verificar su funcionamiento en un dispositivo durante este inventario. Su contenido puede cambiar antes de integrarse.

- **26-11-2026 · Reservas:** añadir una reserva al calendario desde la confirmación o desde la lista de reservas; requiere una aplicación de calendario compatible. Referencias: `Booking.kt`, `booking_calendar.xml`.
- **26-11-2026 · Entrenamiento:** consultar y realizar entrenamientos del programa del gimnasio cuando haya un programa disponible; se muestra la opción de actualizar si todavía no hay entrenamientos. Referencias: `WorkoutHubScreen.kt`, `GymProgramStore.kt`.
- **26-11-2026 · Entrenamiento:** visualización de las notas asociadas a los bloques de ejercicios. Referencias: `WorkoutHubScreen.kt`, `WorkoutBlockNotes.kt`.
- **26-11-2026 · Apariencia:** decoración navideña y cambio estacional del icono, con una opción en ajustes para activar o desactivar las decoraciones. Referencias: `SeasonalDecorations.kt`, `SeasonalLauncherIcon.kt`, `ProfileScreen.kt`.
- **26-11-2026 · Cuenta:** pantalla de diagnóstico de sincronización que permite consultar información local y copiarla. Referencias: `AccountAndSubscription.kt`, `SyncDiagnosticsDialog.kt`.

## Borrador para Google Play

Preparado a partir del inventario anterior. Incluye cambios aún sin commit y otros cuya publicación se desconoce; revisar su inclusión en la versión antes de copiarlo a Play Console. Las etiquetas deben coincidir con los idiomas configurados en la ficha de la aplicación.

```text
<ca>
• Descobreix les novetats destacades en obrir l’app.
• Nous exercicis i fitxes d’entrenament.
• Exporta entrenaments i reserves al calendari.
• Consulta els entrenaments del teu gimnàs i les notes dels exercicis.
• Segueix l'entrenament i els descansos des de les notificacions.
• Consulta i edita els àpats i personalitza els objectius nutricionals.
• Activa o desactiva la decoració nadalenca i descobreix el nou efecte de vidre trencat de la icona.
• Consulta el diagnòstic de sincronització.
</ca>
<en-US>
• Discover highlighted updates when you open the app.
• More exercises and workout guides.
• Export workouts and bookings to your calendar.
• View your gym workouts and exercise block notes.
• Follow your workout and rest periods from notifications.
• View and edit meals and customize nutrition goals.
• Turn Christmas decorations on or off and see the refreshed shattered-glass icon.
• View sync diagnostics.
</en-US>
<es-ES>
• Descubre las novedades destacadas al abrir la app.
• Nuevos ejercicios y fichas de entrenamiento.
• Exporta entrenamientos y reservas al calendario.
• Consulta los entrenamientos de tu gimnasio y las notas de los bloques.
• Sigue tu entrenamiento y descansos desde las notificaciones.
• Consulta y edita comidas y personaliza tus objetivos nutricionales.
• Activa o desactiva la decoración navideña y descubre el nuevo efecto de cristal roto del icono.
• Consulta el diagnóstico de sincronización.
</es-ES>
```

## Versiones publicadas

Todavía no hay versiones registradas.
