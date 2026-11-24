package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.SetStyleParameters

private data class SetStyleCopy(
    val meaning: String,
    val operation: String,
    val usefulness: String,
    val benefits: List<String>,
)

/** The same plan-specific legend shown by iOS, not just abbreviated chips. */
internal data class SetStyleDetail(val label: String, val explanation: String)

internal fun setStyleIcon(style: ExerciseSetStyle): ImageVector = when (style) {
    ExerciseSetStyle.Warmup -> Icons.Filled.LocalFireDepartment
    ExerciseSetStyle.Straight -> Icons.Filled.Equalizer
    ExerciseSetStyle.TopSetBackoff -> Icons.Filled.Terrain
    ExerciseSetStyle.AscendingPyramid -> Icons.AutoMirrored.Filled.TrendingUp
    ExerciseSetStyle.DropSet -> Icons.AutoMirrored.Filled.TrendingDown
    ExerciseSetStyle.RestPause -> Icons.Filled.PauseCircleOutline
    ExerciseSetStyle.Intervals -> Icons.Filled.Timer
    ExerciseSetStyle.Tempo -> Icons.Filled.MusicNote
}

/**
 * Keeps the straight-set glyph aligned with iOS, whose SF Symbol is `equal`
 * (two horizontal strokes), rather than Material's three-bar Equalizer icon.
 */
@Composable
internal fun SetStyleGlyph(style: ExerciseSetStyle, modifier: Modifier = Modifier, tint: Color) {
    if (style == ExerciseSetStyle.Straight) {
        Canvas(modifier = modifier) {
            val stroke = size.minDimension * 0.1f
            listOf(0.4f, 0.6f).forEach { y ->
                drawLine(tint, Offset(size.width * 0.25f, size.height * y), Offset(size.width * 0.75f, size.height * y), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    } else {
        Icon(setStyleIcon(style), contentDescription = null, modifier = modifier, tint = tint)
    }
}

@Composable
internal fun SetStyleInfoScreen(style: ExerciseSetStyle, totalSets: Int, parameters: SetStyleParameters, onBack: () -> Unit) {
    val copy = style.localizedCopy(parameters)
    val backLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_go_back)
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = backLabel }.clickable(onClick = onBack).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.accent)
                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_go_back).uppercase(), color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
            }
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_set_style).uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SetStyleGlyph(style, Modifier.size(52.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).padding(14.dp), WildforceThemeTokens.textPrimary)
            Column(Modifier.padding(start = 14.dp)) {
                Text(workoutSetStyleLabel(style).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                localizedSetStyleScope(style, totalSets, parameters.appliesToFinalSetOnly)?.let {
                    Text(it, color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                }
            }
        }
        val planDetails = localizedStyleParameterDetails(style, parameters, copy.operation)
        if (planDetails.isNotEmpty()) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.your_plan_details).uppercase(), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                planDetails.forEach { detail ->
                    Column(
                        Modifier.fillMaxWidth()
                            .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(6.dp),
                                tint = WildforceThemeTokens.accent,
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(detail.label, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                                Text(detail.explanation, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InfoSection(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.what_it_means).uppercase(), copy.meaning)
            InfoSection(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.how_it_works).uppercase(), copy.operation)
            SetStyleMiniDiagram(style, totalSets, parameters)
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_22447c9c12d49c89), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                copy.benefits.chunked(2).forEach { rowBenefits ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowBenefits.forEach { benefit ->
                            Row(
                                Modifier.weight(1f).background(WildforceThemeTokens.textPrimary.copy(alpha = 0.10f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WildforceThemeTokens.accent, modifier = Modifier.size(15.dp))
                                Text(benefit, color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        if (rowBenefits.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            InfoSection(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.why_it_s_useful).uppercase(), copy.usefulness)
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun SetStyleMiniDiagram(style: ExerciseSetStyle, totalSets: Int, parameters: SetStyleParameters) {
    val items = when (style) {
        ExerciseSetStyle.Warmup -> listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.light), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.build), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ready))
        ExerciseSetStyle.Straight -> List(totalSets.coerceAtLeast(1).coerceAtMost(6)) { "S${it + 1}" }
        ExerciseSetStyle.TopSetBackoff -> listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.top)) + List(parameters.backoffSetCount.coerceAtLeast(1).coerceAtMost(4)) { stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.backoff) }
        ExerciseSetStyle.AscendingPyramid -> List(totalSets.coerceAtLeast(1).coerceAtMost(6)) { "${it + 1}" }
        ExerciseSetStyle.DropSet -> listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.set), "-${parameters.dropWeightPercent}%", "-${parameters.dropWeightPercent}%")
        ExerciseSetStyle.RestPause -> listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.hard_set), "${parameters.intraSetRestSeconds}s", stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mini), "${parameters.intraSetRestSeconds}s", stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mini))
        ExerciseSetStyle.Intervals -> listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.work), "${parameters.intraSetRestSeconds}s", stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.work), "${parameters.intraSetRestSeconds}s")
        ExerciseSetStyle.Tempo -> parameters.tempo.split('-').filter { it.isNotBlank() }.map { "${it}s" }.ifEmpty { listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.down), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.pause), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.up)) }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, label ->
            val isPause = label.endsWith("s") && style in setOf(ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals, ExerciseSetStyle.Tempo)
            Text(
                label,
                Modifier
                    .background(
                        if (isPause) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                color = if (isPause) WildforceThemeTokens.textSecondary else WildforceThemeTokens.backgroundSecondary,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
            )
            if (index < items.lastIndex && style in setOf(ExerciseSetStyle.DropSet, ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.textSecondary)
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text(body, Modifier.padding(top = 6.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body2)
    }
}

internal fun setStyleScopeLabel(style: ExerciseSetStyle, totalSets: Int, appliesToFinalSetOnly: Boolean): String? = when {
    totalSets <= 1 -> null
    style == ExerciseSetStyle.TopSetBackoff -> "Top set primero"
    appliesToFinalSetOnly -> "Solo en la última serie"
    else -> "Se aplica en todas las series"
}

@Composable
internal fun localizedSetStyleScope(style: ExerciseSetStyle, totalSets: Int, appliesToFinalSetOnly: Boolean): String? {
    if (totalSets <= 1) return null
    val id = when {
        style == ExerciseSetStyle.TopSetBackoff -> io.codepassion.doubletriangle.core.designsystem.R.string.top_set_first
        appliesToFinalSetOnly -> io.codepassion.doubletriangle.core.designsystem.R.string.final_set_only
        else -> io.codepassion.doubletriangle.core.designsystem.R.string.all_sets
    }
    return stringResource(id)
}

internal fun styleParameterDetails(style: ExerciseSetStyle, parameters: SetStyleParameters): List<SetStyleDetail> = buildList {
    when (style) {
        ExerciseSetStyle.TopSetBackoff -> {
            add(SetStyleDetail("${parameters.backoffSetCount} backoff", "Después del top set, realiza ${parameters.backoffSetCount} serie${if (parameters.backoffSetCount == 1) "" else "s"} más ligera${if (parameters.backoffSetCount == 1) "" else "s"} para sumar volumen de calidad."))
            add(SetStyleDetail("-${parameters.backoffWeightPercent}% de carga", "Para las series backoff, reduce aproximadamente un ${parameters.backoffWeightPercent}% el peso respecto al top set."))
        }
        ExerciseSetStyle.DropSet -> {
            add(SetStyleDetail("${parameters.dropCount} drop${if (parameters.dropCount == 1) "" else "s"}", "Tras el esfuerzo principal, baja el peso y continúa ${parameters.dropCount} ${if (parameters.dropCount == 1) "vez" else "veces"} casi sin descanso."))
            add(SetStyleDetail("-${parameters.dropWeightPercent}% por drop", "En cada descenso utiliza aproximadamente un ${parameters.dropWeightPercent}% menos de carga que en el esfuerzo anterior."))
        }
        ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals -> add(SetStyleDetail("${parameters.intraSetRestSeconds}s de pausa", "Descansa unos ${parameters.intraSetRestSeconds} segundos antes del siguiente mini-esfuerzo de esta misma secuencia."))
        ExerciseSetStyle.Tempo -> add(SetStyleDetail("Tempo ${parameters.tempo}", "El tempo ${parameters.tempo} indica cómo controlar la bajada, las pausas y la subida de cada repetición."))
        else -> Unit
    }
    if (style != ExerciseSetStyle.Warmup) {
        val explanation = if (parameters.targetRir == 0) {
            "RIR 0 significa terminar al fallo, sin repeticiones en reserva."
        } else {
            "RIR ${parameters.targetRir} significa acabar sintiendo que aún podrías hacer aproximadamente ${parameters.targetRir} repetición${if (parameters.targetRir == 1) "" else "es"} con buena técnica."
        }
        add(SetStyleDetail("RIR ${parameters.targetRir}", explanation))
    }
}

@Composable
internal fun localizedStyleParameterDetails(style: ExerciseSetStyle, parameters: SetStyleParameters, operation: String): List<SetStyleDetail> {
    val result = mutableListOf<SetStyleDetail>()
    when (style) {
        ExerciseSetStyle.TopSetBackoff -> {
            val count = parameters.backoffSetCount
            result.add(SetStyleDetail("$count ${stringResource(if (count == 1) io.codepassion.doubletriangle.core.designsystem.R.string.backoff_set else io.codepassion.doubletriangle.core.designsystem.R.string.backoff_sets)}", operation))
            result.add(SetStyleDetail(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.__d___backoff_load, parameters.backoffWeightPercent), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.for_the_backoff_work__reduce_the_weight_by_about__d___from_your_top_set_, parameters.backoffWeightPercent)))
        }
        ExerciseSetStyle.DropSet -> {
            result.add(SetStyleDetail("${stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.drop_count)}: ${parameters.dropCount}", operation))
            result.add(SetStyleDetail(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.__d___load, parameters.dropWeightPercent), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.each_drop_should_use_about__d___less_weight_than_the_effort_before_it_, parameters.dropWeightPercent)))
        }
        ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals -> result.add(SetStyleDetail(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string._d_sec_rest, parameters.intraSetRestSeconds), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.pause_for_about__d_seconds_before_starting_the_next_mini_effort_inside_the_same_style_sequence_, parameters.intraSetRestSeconds)))
        ExerciseSetStyle.Tempo -> result.add(SetStyleDetail(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tempo___, parameters.tempo), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tempo____tells_you_how_controlled_each_rep_should_be__usually_covering_the_lowering__pauses__and_lifting_phases_in_order_, parameters.tempo)))
        else -> Unit
    }
    if (style != ExerciseSetStyle.Warmup) {
        val rir = parameters.targetRir
        val explanation = if (rir == 0) stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.rir_0_means_finish_the_set_at_failure__with_no_reps_left_in_reserve_)
        else stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.rir__d_means_finish_the_set_feeling_like_you_could_still_do_about__d_more____with_good_form_, rir, rir, stringResource(if (rir == 1) io.codepassion.doubletriangle.core.designsystem.R.string.rep else io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_reps))
        result.add(SetStyleDetail(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.rir__d, rir), explanation))
    }
    return result
}

/** Kept as a compact representation for the inline active-workout card. */
internal fun styleParameterLabels(style: ExerciseSetStyle, parameters: SetStyleParameters): List<String> =
    styleParameterDetails(style, parameters).map(SetStyleDetail::label)

@Composable
private fun ExerciseSetStyle.localizedCopy(parameters: SetStyleParameters): SetStyleCopy = when (this) {
    ExerciseSetStyle.Warmup -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_the_set_is_here_to_prepare_you_for_the_harder_work_ahead_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.start_with_easier_sets_before_your_hard_work_sets__the_goal_is_to_raise_body_temperature__practice_the_movement__and_gradually_get_closer_to_your_working_weight_without_creating_too_much_fatigue_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.warmup_sets_help_you_feel_more_prepared_before_heavy_work__improve_movement_quality__and_reduce_the_chance_that_your_first_hard_set_feels_abrupt_or_sloppy_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.get_ready), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.practice_form), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.move_better)),
    )
    ExerciseSetStyle.Straight -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_the_exercise_uses_a_steady__repeatable_working_pattern_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.use_the_same_working_weight_and_a_similar_effort_target_across_your_sets__each_set_should_feel_consistent__which_makes_it_easier_to_track_performance_and_notice_when_you_re_getting_stronger_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.straight_sets_are_simple__repeatable__and_easy_to_progress__they_re_great_for_building_consistency_because_you_can_compare_set_to_set_and_week_to_week_without_too_many_moving_parts_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.stay_consistent), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.track_progress), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_it_simple)),
    )
    ExerciseSetStyle.TopSetBackoff -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_you_will_push_one_hardest_set__then_continue_with_lighter_backoff_work_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.build_up_to_one_hardest_set_first__then_reduce_the_load_by_about__d___for__d_____the_top_set_gives_you_the_main_high_effort_stimulus__and_the_lighter_backoff_work_adds_more_quality_volume_after_it_, parameters.backoffWeightPercent, parameters.backoffSetCount, stringResource(if (parameters.backoffSetCount == 1) io.codepassion.doubletriangle.core.designsystem.R.string.backoff_set else io.codepassion.doubletriangle.core.designsystem.R.string.backoff_sets)),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_style_gives_you_both_intensity_and_volume__the_top_set_helps_you_practice_pushing_hard__while_the_backoff_work_lets_you_accumulate_more_productive_reps_without_needing_to_stay_at_your_heaviest_load_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.push_intensity), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.add_volume), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.manage_fatigue)),
    )
    ExerciseSetStyle.AscendingPyramid -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_the_sets_build_up_as_you_go__usually_with_more_load_and_fewer_reps_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.increase_the_weight_from_set_to_set_while_the_reps_usually_come_down__each_set_becomes_more_demanding__so_you_ramp_from_a_manageable_starting_set_into_your_hardest_work_at_the_end_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ascending_pyramids_let_you_ease_into_heavy_work_instead_of_jumping_straight_to_it__they_can_help_with_confidence__technical_rhythm__and_strength_focused_training_where_ramping_up_matters_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.build_momentum), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ramp_load), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.peak_late)),
    )
    ExerciseSetStyle.DropSet -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_the_set_continues_after_a_quick_weight_drop_to_extend_the_effort_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.complete_your_main_effort__then_immediately_reduce_the_load_by_about__d___and_continue__repeat_that__d____with_minimal_rest_so_one_set_turns_into_an_extended_high_fatigue_sequence_, parameters.dropWeightPercent, parameters.dropCount, stringResource(if (parameters.dropCount == 1) io.codepassion.doubletriangle.core.designsystem.R.string.time else io.codepassion.doubletriangle.core.designsystem.R.string.times)),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.drop_sets_are_useful_when_you_want_extra_hypertrophy_stimulus_in_less_time__they_drive_a_lot_of_fatigue_and_effort_quickly__which_makes_them_especially_effective_as_a_finisher_or_last_hard_set_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.extend_effort), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.fast_stimulus), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.finish_strong)),
    )
    ExerciseSetStyle.RestPause -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_one_hard_set_is_broken_into_short_mini_efforts_with_tiny_rests_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.take_one_hard_effort_close_to_failure__rest_for_about__d_seconds__then_continue_with_the_same_weight_for_another_mini_set__you_can_repeat_that_short_rest_and_repeat_pattern_to_squeeze_more_quality_reps_out_of_the_same_load_, parameters.intraSetRestSeconds),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.rest_pause_helps_you_get_more_high_quality_reps_with_a_challenging_weight_than_you_could_in_one_uninterrupted_set__it_s_a_time_efficient_way_to_push_effort_without_adding_a_lot_of_extra_full_sets_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.more_hard_reps), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.short_rests), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.high_effort)),
    )
    ExerciseSetStyle.Intervals -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_the_exercise_follows_a_work_and_recovery_rhythm_instead_of_one_continuous_push_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.alternate_repeated_work_bouts_with_about__d_seconds_of_planned_recovery_between_efforts__the_goal_is_to_keep_each_interval_purposeful_while_giving_you_just_enough_recovery_to_repeat_the_target_intensity_across_the_full_set_sequence_, parameters.intraSetRestSeconds),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.intervals_are_useful_for_conditioning_because_they_let_you_accumulate_hard_efforts_with_a_clear_work_to_recovery_rhythm__they_improve_repeatable_output__pacing__and_cardiovascular_fitness_without_requiring_one_nonstop_maximal_push_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.control_pace), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.repeat_output), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.build_conditioning)),
    )
    ExerciseSetStyle.Tempo -> SetStyleCopy(
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_cue_means_the_speed_and_control_of_each_rep_matter_as_much_as_the_reps_themselves_),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.use_the_planned_rep_speed_of____for_every_rep__that_tempo_tells_you_how_controlled_the_lowering__pause__and_lifting_phases_should_be_so_the_set_is_driven_by_movement_quality__not_just_by_load_, parameters.tempo),
        stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tempo_work_improves_control__keeps_tension_on_the_target_muscle__and_can_make_lighter_loads_more_effective__it_s_especially_useful_when_technique__positioning__or_muscle_awareness_matters_more_than_moving_the_most_weight_possible_),
        listOf(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.control_speed), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.more_tension), stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.better_form)),
    )
}
