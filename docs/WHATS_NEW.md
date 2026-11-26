# Novedades en la app

La selección se mantiene localmente en `WhatsNew.kt` y en los recursos `whats_new_strings.xml`. Para una nueva entrega, sustituir los textos y la ilustración por contenido confirmado en esa versión y aumentar `WHATS_NEW_REVISION`. La ventana se muestra al abrir la app hasta que se cierra; después no reaparece para esa revisión. Las revisiones son independientes de `versionCode`: una actualización sin novedades nuevas no vuelve a mostrar la ventana.

La ilustración actual es una animación conceptual de Compose que representa un entrenamiento y su paso al calendario, no un GIF ni una captura de pantalla. Puede sustituirse por una demostración adecuada a la funcionalidad destacada. Revisar las traducciones y comprobar la ventana en dispositivos y tamaños de fuente diferentes antes de publicar. Los idiomas sin traducción propia usan los recursos ingleses. La preferencia puede restaurarse mediante la copia de seguridad de Android.
