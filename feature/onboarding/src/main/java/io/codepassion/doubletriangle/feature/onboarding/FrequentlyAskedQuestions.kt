package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens

@Composable
internal fun FrequentlyAskedQuestions() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Todo lo que necesitas para entrenar, comer mejor y seguir avanzando con Wildforce.",
            style = MaterialTheme.typography.body2,
            color = WildforceThemeTokens.textSecondary,
        )
        FaqCategory.entries.forEach { category ->
            Text(
                category.title.uppercase(),
                fontFamily = Exo2FontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WildforceThemeTokens.accent,
                modifier = Modifier.padding(top = 8.dp),
            )
            category.questions.forEach { entry -> FaqRow(entry) }
        }
        Text(
            "¿Necesitas más ayuda? Envíanos desde Ayuda el modelo de móvil, versión de Android y de Wildforce, junto con una breve descripción.",
            style = MaterialTheme.typography.body2,
            color = WildforceThemeTokens.textSecondary,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun FaqRow(entry: FaqEntry) {
    var expanded by remember(entry.question) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WildforceThemeTokens.backgroundSecondary)
            .clickable { expanded = !expanded }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.question,
                modifier = Modifier.weight(1f),
                fontFamily = Exo2FontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = WildforceThemeTokens.textPrimary,
            )
            Text(if (expanded) "−" else "+", color = WildforceThemeTokens.accent, fontSize = 24.sp)
        }
        if (expanded) {
            Text(entry.answer, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
    }
}

private data class FaqEntry(val question: String, val answer: String)

private enum class FaqCategory(val title: String, val questions: List<FaqEntry>) {
    GettingStarted(
        "Primeros pasos",
        listOf(
            FaqEntry("¿Qué es Wildforce?", "Wildforce es tu plataforma de entrenamiento y nutrición para iPhone y Android. Crea planes adaptados a tu objetivo, registra cada sesión, controla tus macros y convierte la constancia en progreso real."),
            FaqEntry("¿Cómo se crea mi plan de entrenamiento?", "Durante el inicio eliges objetivo, experiencia, disponibilidad, equipo, preferencias y limitaciones. Wildforce usa esa información para crear un plan progresivo que puedes seguir y ajustar."),
            FaqEntry("¿Qué necesito para usar Wildforce en Android?", "Necesitas Android 9 o posterior y la versión más reciente de Wildforce disponible para tu región."),
            FaqEntry("¿Puedo entrenar sin conexión?", "Puedes consultar y registrar contenido disponible localmente. Generar planes o analizar información con IA requiere conexión. Los cambios pendientes se sincronizan al recuperar internet cuando la sincronización en la nube está activa."),
        ),
    ),
    Training(
        "Entrenamiento y progreso",
        listOf(
            FaqEntry("¿Puedo cambiar un ejercicio durante un entreno?", "Sí. Puedes buscar una alternativa, sustituir el ejercicio o retirarlo de la sesión activa para adaptarte al equipo que tengas disponible."),
            FaqEntry("¿Puedo crear mis propios entrenamientos?", "Sí. Puedes crear, editar, ejecutar y eliminar entrenamientos personalizados cuando quieras salir de tu plan o necesites una sesión a medida."),
            FaqEntry("¿Cómo funcionan los descansos?", "Al terminar una serie, Wildforce controla el descanso. El entreno activo puede seguir accesible mediante una notificación si has permitido las notificaciones."),
            FaqEntry("¿Dónde veo mi evolución?", "En Progreso encontrarás el historial por ejercicio, volumen, estadísticas, rachas y evolución corporal para comprobar qué funciona."),
            FaqEntry("¿Qué ocurre al terminar un entreno?", "La sesión queda en tu historial y actualiza estadísticas, progreso y racha. Podrás compararla con sesiones anteriores cuando haya datos suficientes."),
        ),
    ),
    Nutrition(
        "Nutrición",
        listOf(
            FaqEntry("¿Cómo registro lo que como?", "Puedes usar foto, código de barras, etiqueta nutricional, texto, búsqueda de alimentos, recientes o registro manual. Elige la opción más rápida para ti."),
            FaqEntry("¿Puede proponer comidas con lo que tengo en casa?", "Sí. Añade los ingredientes disponibles y usa ideas de comida para recibir propuestas alineadas con tus objetivos nutricionales."),
            FaqEntry("¿Qué pasa si no llego a mis macros?", "Wildforce puede sugerir alimentos o comidas para cubrir los macros que te quedan. El objetivo no es la perfección, sino tomar mejores decisiones de forma constante."),
            FaqEntry("¿Puedo ver mi planificación por días?", "Sí. La planificación incluye calendario semanal, objetivos de energía y macros, y días de déficit, mantenimiento o superávit según tu meta."),
        ),
    ),
    Devices(
        "Android y dispositivos",
        listOf(
            FaqEntry("¿Qué integración de salud usa Android?", "Wildforce se conecta con Health Connect. Tú eliges los permisos para altura, peso, calorías, nutrición y entrenamientos, y puedes revocarlos desde Ajustes."),
            FaqEntry("¿Para qué sirven las notificaciones?", "Mantienen visible el entreno y el descanso activos y permiten recibir recordatorios. Si no ves la sesión activa, comprueba el permiso de notificaciones de Wildforce."),
            FaqEntry("¿Hay widget de Wildforce para Android?", "Hay un acceso rápido al entreno desde la pantalla de inicio. Mantén pulsado un espacio vacío, abre el selector de widgets y busca Wildforce cuando esté habilitado en tu versión."),
            FaqEntry("¿Funciona con Wear OS?", "La integración con Wear OS está planificada, pero todavía no está disponible. Puedes registrar y seguir los entrenos desde tu móvil Android."),
            FaqEntry("¿Puedo usar Wildforce en iPhone?", "Sí. En iPhone, Wildforce se integra con Apple Health y Apple Watch para entrenos activos, descansos e información de repeticiones en ejercicios compatibles."),
        ),
    ),
    Account(
        "Cuenta, datos y ajustes",
        listOf(
            FaqEntry("¿Puedo usar unidades imperiales?", "Sí. Elige sistema métrico o imperial en Ajustes. El peso y las medidas se mostrarán en el formato que prefieras."),
            FaqEntry("¿Mis datos estarán en un dispositivo nuevo?", "La información se guarda localmente y puede sincronizarse con tu cuenta si la sincronización en la nube está activa. Inicia sesión con la misma cuenta para recuperar los datos sincronizados."),
            FaqEntry("¿Puedo cambiar idioma, tema o preferencias?", "Sí. Desde Perfil y Ajustes puedes cambiar idioma, unidades, preferencias de plan, recordatorios y tema visual."),
            FaqEntry("¿Qué hago si Health Connect no sincroniza?", "Revisa los permisos en Health Connect, actualiza Wildforce y vuelve a abrir la app. Si continúa, contacta con soporte e indica plataforma y versión de la app."),
            FaqEntry("¿Qué hago si algo no funciona?", "Actualiza Wildforce, reinicia la app y revisa permisos. Si el problema continúa, contacta con soporte e incluye modelo de móvil, versión de Android, versión de Wildforce y una breve descripción."),
        ),
    ),
}
