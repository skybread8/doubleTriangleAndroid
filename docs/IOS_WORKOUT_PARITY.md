# iOS Workout Parity Specification

This document is the persistent source of truth for bringing the Android workout experience to functional and visual parity with iOS. Every change to the Android workout detail, active session, timers, exercise completion, rest, or completion celebrations must be checked against this document.

## Reference implementation

The canonical behavior lives in the sibling iOS repository:

- `../doubleTriangle/Wildforce/Views/App/Training/Workout/WorkoutDayView.swift`
- `../doubleTriangle/Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutView.swift`
- `../doubleTriangle/Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutSceneView.swift`
- `../doubleTriangle/Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutStore.swift`
- `../doubleTriangle/Wildforce/Views/App/Training/Workout/Completion/WorkoutFinishedCelebration.swift`

Android implementation:

- `feature/workout/src/main/java/io/codepassion/doubletriangle/feature/workout/WorkoutHubScreen.kt`
- `feature/workout/src/main/java/io/codepassion/doubletriangle/feature/workout/WorkoutSessionStore.kt`
- `feature/workout/src/main/java/io/codepassion/doubletriangle/feature/workout/ExerciseWorkTimer.kt`
- `feature/workout/src/main/java/io/codepassion/doubletriangle/feature/workout/WorkoutCompletionFlowScreen.kt`

## Definition of 100% parity

Parity means equivalent product behavior and information hierarchy while using native platform controls where appropriate. Platform-only integrations such as Apple Watch, HealthKit, Live Activities, Android foreground notifications, and TalkBack/VoiceOver do not need identical APIs, but must provide equivalent user value where the platform supports it.

The following must all be true:

- A completed workout cannot be started again from its detail screen.
- Planned and resumed sessions display the correct action and preserve all progress.
- Repetitions, weight, duration, distance, and per-set targets are typed data and never inferred from presentation strings.
- Timed exercises start automatically, remain accurate through lifecycle changes, and follow iOS completion transitions at zero.
- A configured zero-second rest advances immediately and never creates a synthetic one-second rest.
- Rest distinguishes between sets, between superset rounds, and before the next block.
- Supersets execute in exercise/round order, persist one logical exercise result, and request feedback only after the final round.
- Per-set targets and completed values remain independently editable.
- Feedback starts at `Just right`, notes are optional, and completion is immediately available.
- Workout completion includes duration warning, records, summary, score, streak, workout XP/level, plan completion, mesocycle completion, plan/mesocycle XP and level where applicable, notification education, rating prompt, and next-plan generation.
- Typography uses Anton for display headings and Exo 2 for UI/body text with the same hierarchy as iOS.
- Color, corner-radius, spacing, button, timer, sheet, and progress tokens match the iOS source values.
- All user-facing strings come from Android resources and are translatable.
- Every interactive control has TalkBack semantics, a minimum 48 dp touch target, and appropriate haptic feedback.
- Light/dark mode, compact phones, large font scales, process recreation, and background timer restoration are covered by tests.

## Baseline audit (2026-08-27)

Initial estimate: functional 70%, visual 55%, accessibility/localization/integration 40%, global approximately 60%.

### Critical blockers

- [x] Android workout models express typed duration, distance, rep ranges, tracking mode, per-side loading, and per-set targets/results. `ExerciseTrackingMode` and `RepRange` are persisted in plan and custom-workout JSON; legacy display strings are only read for compatibility, never used to decide a new session's timer behavior.
- [x] Timed work is stored as duration, never as repetitions.
- [x] Timed exercises auto-start and transition automatically at zero.
- [x] Zero-second rest advances immediately.
- [x] Completed workout detail shows results instead of a start button.

### Session behavior

- [x] Exit/resume semantics match iOS without accidental data loss.
- [x] Rest contexts and labels match iOS.
- [x] Superset transitions and aggregation match iOS.
- [x] Feedback defaults and persistence match iOS.
- [ ] Set-style scope and detail chips match iOS.
- [ ] Workout path, exercise history, guide, and exercise actions retain progress.

### Visual system

- [x] Workout colors are mapped by their actual iOS usage: neutral accent, system-orange warm emphasis, red superset connector, and warmup/cooldown section colors. `AccentColor2` is excluded because it belongs to Nutrition, not Workout.
- [x] Workout detail hero type sizes match iOS (Day 38, title 24).
- [x] Active exercise title matches iOS (Exo 2, 26).
- [x] Active sheet uses the iOS 54-point continuous corner treatment.
- [x] Primary/secondary button geometry matches iOS.
- [x] Work timer is 132; rest timer is 148; controls match their iOS sizes.
- [ ] Progress, pills, shadows, gradients, and transitions match iOS.

### Completion and platform quality

- [x] Completion state machine contains every applicable iOS phase.
- [x] Summary metrics do not count warmup/cooldown or timed seconds as reps.
- [ ] Android equivalents exist for available health/notification data.
- [ ] Strings are resource-backed and localized.
- [ ] TalkBack semantics cover timers, progress, set editors, feedback, and rest controls.
- [ ] Haptics cover set, exercise, feedback, rest adjustment, and final completion.

## Implementation log

### 2026-08-27 — first parity implementation pass

- Added typed duration, distance, per-side load and per-set repetition/weight targets to the Android model and plan JSON contract.
- Added automatic timed-work start/completion, lifecycle-aware timer restoration and zero-rest transitions.
- Added persisted rest context for between-set, between-round and before-next-block transitions.
- Corrected timed result storage, distance capture, feedback default, superset transitions and completed-workout detail behavior.
- Aligned workout score adherence and feedback weighting with the iOS implementation.
- Added separate workout, plan and mesocycle XP/level phases plus notification education, rating and next-plan phases.
- Aligned the verified hero, title, sheet, button and timer dimensions and the principal workout palette tokens.
- Mirrored the verified iOS workout palette as Android design tokens, including device background, primary button foreground/shadow, warmup and cooldown accents. `AccentColor2` is intentionally not used by Workout.
- Added unit coverage for typed timers, timed result metrics, zero-rest behavior and scoring.
- Verified with `./gradlew :feature:workout:test :app:testDebugUnitTest`.

### 2026-08-28 — typed custom-workout prescriptions

- Added explicit `ExerciseTrackingMode` and `RepRange` to the shared Android workout model and the generated-plan/custom-workout persistence contracts.
- The manual custom-workout editor now configures repetitions, timed work, and timed distance work as distinct prescriptions.
- Timers now use the typed tracking mode and typed duration only; legacy presentation text remains a compatibility parser, not runtime workout behavior.
- Added model coverage for repetition, duration, and duration-with-distance prescriptions and verified with `./gradlew :core:model:test :feature:workout:test :app:testDebugUnitTest`.

### 2026-08-28 — accessibility and haptic interaction pass

- Added progress and state semantics to work/rest timers, contextual labels for set-editor increment/decrement controls, and selected-state semantics for exertion feedback.
- Added haptic acknowledgement for completed sets and the final level/plan/mesocycle milestones; timer, rest, feedback and metric controls retain haptic response.
- The accessibility checkbox remains open pending TalkBack traversal and 48 dp target verification on hardware at normal and large font scales.

### 2026-08-28 — minimum touch-target pass

- Kept the compact iOS-equivalent visual controls while expanding active-session navigation, back, set adjustment, custom-editor stepper and menu hit areas to at least 48 dp in Compose.
- Hardware TalkBack and large-font verification remains required before closing the accessibility checklist item.

### 2026-08-29 — custom-workout localization pass

- Moved the custom-workout list, editor, AI request, exercise picker and work-timer user-facing strings into the Workout feature string resources.
- The global localization checklist remains open: the rest of the workout hub, active session and completion flow still require the same migration and locale coverage.

### 2026-08-29 — set-style scope persistence pass

- Added `appliesToFinalSetOnly` to Android set-style parameters, the AI/custom-workout persistence contracts and the manual editor. Drop/rest-pause styles now explicitly preserve whether they apply to every set or only the final set, matching the iOS configuration scope.
- The set-style information screen uses that persisted scope for its caption and per-set instructions. The checklist remains open until the active inline chips and information screen receive screenshot comparison.

The remaining unchecked items are intentionally not implied to be complete. In particular, full resource localization, explicit tracking/rep-range types, complete TalkBack/haptic coverage and screenshot-based pixel comparison still block a 100% claim.

### 2026-08-30 — Samsung SM-S918B custom-workout hardware pass

- Deployed the available debug APK to the connected Samsung SM-S918B (1440×3088, dark mode) and exercised the custom-workout entry point without saving a disposable draft.
- Confirmed that the custom list, new-workout chooser, AI request form, manual editor, exercise picker, typed prescription chips, steppers and set-style selector render and accept navigation input on real hardware.
- The AI form exposes goal, multiple focuses, muscle groups, duration, equipment, warmup and cooldown as intended. The manual editor exposes repetitions, time and time-plus-distance tracking modes.
- Hardware inspection found that the app-level navigation rail overlaid the manual editor's persistent action and lower controls, and that the editor title was truncated at the S23 Ultra width. The source now reserves 92 dp above the rail and uses a compact header title. This correction is pending a fresh APK build and redeploy before the visual/accessibility checkbox can close.

### 2026-08-30 — verification follow-up

- Built and deployed the corrected debug APK with the project-required JetBrains JDK 25. The manual editor's ready action now remains above the app navigation rail on the Samsung; the header title was shortened to `EDITAR RUTINA` to avoid ellipsis at that width.
- Passed `:core:model:test :feature:workout:test :app:testDebugUnitTest`.

### 2026-09-23 — custom-workout list hierarchy pass

- Aligned the Android custom-workout list with `CustomWorkoutsView.swift`: the populated state now starts with a discrete new-workout action rather than a second screen title, and each saved workout is a 180 dp image-led card with the title and metadata over the lower gradient plus a floating actions menu.
- The empty state now follows the iOS content-unavailable hierarchy (icon, short title, explanatory copy, primary creation action). Functional create, open, edit, duplicate and adapt flows are unchanged.
- Verified compilation with `:feature:workout:compileDebugKotlin`; screenshot comparison at a shared iOS/Android viewport remains pending.

### 2026-09-23 — workout hub and mesocycle hierarchy pass

- Reorganized the Android workout hub to match `WorkoutHubView.swift`: the secondary-surface header now contains only profile/streak/actions and the seven-day calendar. The mode selector and plan content start below it.
- Replaced the Android inline plan-context banner with separate iOS-equivalent `MesocycleHeaderView` and `PlanHeaderView` counterparts: a tappable mesocycle card (week/phase pills and roadmap) precedes the weekly plan card.
- Added the Android counterpart of `WorkoutMesocycleDetailView`: a 24 dp summary card, current-cycle timeline, phase/status metadata, and collapsible prior-cycle history.
- Verified compilation with `:feature:workout:compileDebugKotlin`. This is source-level hierarchy parity only; same-state iOS/Android image comparison is still required before the visual checklist can be closed.

### 2026-09-23 — workout detail prescription hierarchy pass

- Reworked the Android screen shown before starting a workout to follow `WorkoutDayView.swift`'s continuous hierarchy: hero metadata, start/resume action, divider, segmented display selector, then the selected content. The former floating rounded sheet and its artificial hero gap were removed.
- Split the day metadata into independently readable focus, day-type and duration pills, matching iOS's horizontally scrolling header metadata rather than truncating a single combined string.
- Replaced the Android exercise-row summary with the typed iOS-equivalent prescription hierarchy. Rows now show sets/rounds, reps or time, target load, distance and the set-style scope; timed and distance exercises are no longer labelled as repetitions. The expanded objective also uses the typed tracking mode.
- Verified compilation with `:feature:workout:compileDebugKotlin`. Screenshot comparison at an identical iOS/Android viewport remains required; therefore the visual-system checklist remains open.

### 2026-09-23 — pre-workout detail navigation and information pass

- Made a planned-exercise row open the full Android exercise guide, matching the iOS `NavigationLink` to `ExerciseDetailsView`; the former inline partial guide was removed.
- Raised the content surface below the workout header, aligned exercise-title wrapping and hierarchy with `PlannedExerciceView`, and added the iOS-equivalent symbols to the workout overflow actions.
- Reworked the Information tab to retain every target muscle and required-equipment item in horizontal lists, matching `WorkoutDayView.infoView`. The exercise guide now also presents primary/secondary muscles and required equipment before its instructional content.
- Source-level parity is complete for these changes; screenshot comparison at a matched viewport remains required before closing visual-system items.

### 2026-09-23 — active-workout header and numeric-entry pass

- Tightened the Android active-workout action capsule to the native iOS toolbar rhythm while preserving 48 dp targets. The guide/document action now has a deliberate inset from the capsule edge, so its square glyph is not visually clipped by the rounding.
- Kept the PR trophy neutral like `GlassHeaderPill` on iOS; the record chip no longer applies an Android-only gold tint.
- Added a small separation between exercise progress and title to mirror `ActiveWorkoutSceneView`'s header spacing.
- Replaced the active reps/weight free-text dialog with an iOS-equivalent in-app numeric keypad: repetitions are integer-only; weight supports one decimal, locale-aware decimal entry, clear/backspace and the relevant `+/-` step (2.5 kg or 5 lb).
- Verified compilation with `:feature:workout:compileDebugKotlin`. Same-state screenshot comparison and interaction testing on hardware remain required before closing the visual/accessibility checklist items.

### 2026-09-23 — set-style information parity pass

- Brought Android's inline set-style instruction and its information destination in line with `ActiveWorkoutSceneView` and `ActiveWorkoutSetStyleInfoView`: the inline card now uses the persisted scope rather than always claiming every set, and it shows the configured plan-detail chips.
- The detail screen now explains each configured drop, backoff, rest, tempo and RIR parameter rather than showing labels alone. Its how-it-works copy is parameter-aware, and the existing set progression row remains the Android counterpart of iOS's mini diagram.
- Verified with `:feature:workout:testDebugUnitTest :feature:workout:compileDebugKotlin`; matched-viewport screenshot comparison remains required for visual checklist closure.

### 2026-09-23 — active-set stepper spacing pass

- Matched `ActiveWorkoutPerSetTableView.stepperControl`'s visible 10-point `HStack` gaps and 48-point value frame for Android's reps/weight `+/-` controls. The Android 48 dp touch target overflows its compact visual slot, so accessibility sizing does not widen the iOS-equivalent visual rhythm.
- Removed the width breakpoint that changed the active metric row into two lines. Target text now ellipsizes while the label, value and controls remain on one iOS-equivalent row across display and font scales.
- Verified with `:feature:workout:compileDebugKotlin :feature:workout:testDebugUnitTest`.

### 2026-09-24 — rest-timer sheet fit pass

- Matched `ActiveWorkoutRestPhaseView`'s content-sized `VStack`: Android's rest-timer content no longer fills and vertically centers within the sheet's maximum height.
- The rest phase now has the same 24 dp title/control/follow-up spacing as iOS, eliminating the blank bands above and below the timer without changing timer state or actions.
- Verified with `:feature:workout:compileDebugKotlin`.

### 2026-09-24 — exercise-feedback picker fit pass

- Matched `ExerciseFeedbackInputView`'s five-option grid: 10 dp inter-item spacing, 14 dp corners, 1 dp selection border, 6 dp icon/label rhythm, 92 dp content-sized controls, and the selected option's 6 dp lift.
- Android now uses iOS's canonical feedback emojis: 😴, 😊, 👍, 🥵 and 💀.

### 2026-09-24 — active-workout toolbar action parity pass

- Aligned the fourth active-workout toolbar action with iOS `PlannedExerciseMenuActions`: Add inserts after the selected logical exercise, Swap targets that same logical exercise (including a superset round), and Move Up/Down is constrained to the current workout section with the unavailable endpoint disabled.
- The Android menu now follows iOS action grouping and destructive safeguards: superset actions expose the same disabled lower bounds, exercise removal requires confirmation, and Cancel Workout explicitly confirms before clearing the persisted session and leaving the screen. Android-only Skip and set-style entries were removed from this toolbar menu.
- Verified source compilation and `:feature:workout:testDebugUnitTest`. Matched-state device screenshots and TalkBack traversal are still required before the broader visual/accessibility checklist can close.

### 2026-09-24 — active-workout background fade pass

- Corrected the Android active-workout photo treatment to match `ActiveWorkoutSceneView.backgroundImage`: the image and gradient field now extend to 700 dp instead of ending at the responsive header height. The top 60% → 50% black treatment clears at mid-image; a separate lower surface fade begins there, keeping the photo visible substantially farther down behind the active sheet.
- Verified compilation with `:feature:workout:compileDebugKotlin`. Matched-viewport image comparison remains required to close the visual-system checklist.

### 2026-09-24 — active-workout record labels and icon parity pass

- Corrected the last-record label from Android-only all caps (`ÚLTIMO`) to iOS-equivalent title casing (`Último`) in both the active header and exercise-history highlight.
- Replaced the active-header generic upward arrow with the filled circular upward-arrow glyph, matching iOS `arrow.up.circle.fill`.
- Verified with `:feature:workout:compileDebugKotlin :feature:workout:testDebugUnitTest`. A full visual walkthrough of every localized screen remains required before the global localization/visual checklists can close.

### 2026-09-24 — back-navigation icon sizing pass

- Matched iOS's standard navigation-chevron visual weight by fixing every Android back glyph to 22 dp. The workout-detail and active-workout circular buttons now retain their independent visual and 48 dp touch-target sizes while rendering the same chevron size.
- Applied the same 22 dp glyph to the remaining Android back controls, including workout subviews, onboarding, profile, analytics and nutrition.

### 2026-09-25 — workout-card hierarchy pass

- Aligned the Android plan workout cards with `WorkoutDayHeaderView.swift`: the state badge now sits on the leading content axis between the day label and title, while planned cards preserve its empty vertical rhythm.
- Replaced the combined text-symbol metadata line with individual native-icon focus, day-type and duration items below the title. The iOS-equivalent 12 dp item spacing and capsule badge geometry are retained.
- Verified compilation with `:feature:workout:compileDebugKotlin`. Matched-state screenshot comparison remains required before closing the visual-system checklist.

### 2026-09-25 — completed-workout metrics and local calorie fallback

- Completed cards and the completed workout detail now retain iOS-equivalent completion time, duration, calories and volume. Heart-rate metrics remain absent when no wearable or health source has supplied them.
- Added Android's no-wearable calorie estimate using the same iOS MET formula (`MET × BMR / 24 × hours`) and Mifflin–St Jeor BMR from the local profile. Timed exercises use their recorded duration; the remaining session time is distributed across the other exercises, as on iOS.
- Added `WorkoutMetricCalculatorTest` and verified with `:feature:workout:testDebugUnitTest` plus `:feature:workout:compileDebugKotlin :app:compileDebugKotlin`.

### 2026-09-25 — first-exposure exercise onboarding pass

- Added Android's counterpart to iOS `ActiveWorkoutFeatureOnboardingState`. The first arrival at an exercise now presents the three-step exercise introduction (overview, logging targets, and form cues), using the same local guide, muscle, equipment, prescription and tutorial data as Android's exercise guide.
- The flow also queues iOS-equivalent first-exposure education for a non-standard set style and for supersets. Each item is marked when presented and persists independently, so it is not repeated after relaunch and it never interrupts rest or feedback.
- Verified with `:feature:workout:compileDebugKotlin :feature:workout:testDebugUnitTest`. A matched device screenshot review remains required before closing the visual-system checklist.

### 2026-09-28 — active-superset transition and hierarchy pass

- Corrected the Android completion state machine so a superserie requests feedback only after its final exercise in its final round, matching `ActiveWorkoutStore.isSupersetFeedbackDeferred`.
- Removed the Android-only rest that could be scheduled after the final superserie exercise; configured block rest remains between rounds only.
- Aligned the active superserie context with iOS: compact round progress, an exercise-chip route, contextual completion labels and next-in-block copy. Timed exercises retain the same work timer controls and this context.
- Added unit coverage for the feedback gate. Gradle verification remains pending because the local Gradle wrapper cannot validate the TLS certificate for its distribution download.

### 2026-09-25 — pre-workout muscle-effort pass

- Added the iOS-equivalent target-muscle cards to Android's pre-workout Exercises tab, using the existing muscle illustrations and the 0–18 effective-set scale. Cards are horizontally scrollable and announce the value to TalkBack.
- The calculation mirrors `WorkoutDayStimulus`: primary muscles receive one credit per effective set, secondary muscles receive 0.5, and superset exercises use their round count.
- Replaced the former Information-tab exercise-count label with the same stimulus cards. Added unit coverage for primary/secondary weighting and superset rounds, verified with `:feature:workout:testDebugUnitTest :feature:workout:compileDebugKotlin`.

### 2026-09-25 — pre-workout equipment artwork pass

- Added Android's equivalent of iOS `WorkoutEquipmentScroll` to both pre-workout tabs. It now displays the canonical equipment artwork instead of emoji cards and derives the unique equipment from the workout blocks.
- Included the iOS artwork for bodyweight, dumbbells, adjustable bench, cable machine, Olympic barbell, pull-up bar and leg-press machine. Added coverage for the equipment-to-artwork mapping and verified with `:feature:workout:testDebugUnitTest :feature:workout:compileDebugKotlin`.

### 2026-09-28 — pre-workout header and exercise-list icon pass

- Replaced Android's text-symbol metadata (`◎`, `◆`, `◷`) with native icons equivalent to the iOS `scope`, day-type and `clock` symbols. The focus and every iOS workout-day type now resolve to a dedicated Android icon.
- Completed and skipped workout statuses now occupy the trailing top-right position in the pre-workout hero, as in `WorkoutDayView`; the edit menu is reserved for planned/resumable workouts.
- Standard exercise blocks no longer render the Android-only `BLOQUE PRINCIPAL` heading. Warmup, cooldown and superset headers retain their iOS-specific hierarchy; standard-block notes remain visible without creating a synthetic block heading.
- Verified with `:feature:workout:compileDebugKotlin :feature:workout:testDebugUnitTest`.

### 2026-09-28 — skipped-workout plan completion pass

- Matched `SkipWorkoutDay.perform`: skipped workouts remain visibly omitted and do not count toward completed-workout history, but do finalize their plan slot.
- When omitting the final pending workout, Android now starts the same next-plan generation flow as iOS. Plan progress also counts completed and skipped sessions as finalized, while preserving the omitted-session count separately.
- Added model coverage for the finalized-plan status rule and verified with `:core:model:test :feature:workout:compileDebugKotlin :app:compileDebugKotlin`.

### 2026-09-28 — replacement-plan session isolation pass

- Generated sessions now receive unique IDs which are persisted in the plan JSON; old paused-session snapshots can no longer match an identically positioned workout in a new week.
- Confirming a replacement plan clears every prior plan-session snapshot plus its foreground timer, notification actions, notification and watch bridge, matching iOS's completed-plan transition rather than offering a stale session to resume.
- Added identity-contract coverage and verified with `:app:testDebugUnitTest :feature:workout:compileDebugKotlin :app:compileDebugKotlin`.

## Visual evidence log

### 2026-08-27 — source-level comparison, active workout

The following values were read directly from the iOS source and mirrored in Android:

| Element | iOS source | Android implementation |
| --- | --- | --- |
| Active hero | 700 pt image with 0.6 → 0 top overlay and secondary-background lower fade | responsive hero with the matching two-stage dark/lower gradient |
| Exercise progress | 6 pt gaps; current segment 10 pt, other segments 8 pt | 6 dp gaps; current segment 10 dp, other segments 8 dp |
| Active sheet | 10 pt horizontal spacing, 54 pt radius, black 10% / 12 pt shadow | 10 dp horizontal spacing, 54 dp radius, black 10% / 12 dp shadow |
| Work timer | 132 pt; 8 pt stroke; 34 pt bold digits | 132 dp; 8 dp stroke; 34 sp bold digits |
| Rest timer | 148 pt; 12 pt stroke; 42 pt bold digits; 64 pt controls | 148 dp; 12 dp stroke; 42 sp bold digits; 64 dp controls |
| Rest title | title2 bold (22 pt) | Exo 2 bold, 22 sp |
| Palette | AccentColor; `Color.orange` for workout emphasis; AccentColor3 superset connector; background/surface/text/button colors | matching `WildforceThemeTokens`; `AccentColor2` is not used in iOS Workout and must not be mapped to Android workout emphasis |
| Planned exercise row | 4 pt contextual stripe, 68 pt image, 16 pt text gap, capsule block label | 4 dp contextual stripe, 68 dp image, 16 dp text gap, capsule block label |
| Rest between superset rounds | compact list of every exercise in the next round | compact list of every exercise in the next round |
| Primary CTA | 8 pt radius, 2 pt resting shadow, no pressed shadow | 8 dp radius, 2 dp resting elevation, no pressed elevation |
| Completed reps/weight editor | Two plain `HStack` rows; title includes the target (`Reps (Target: …)` / `Weight (kg) (Target: …)`), compact `minus.circle.fill` + monospaced value + `plus.circle.fill` | `ActiveSetEditor` now uses two unboxed rows, combined target labels, 28–30 dp controls and regular-weight monospaced values; the former nested rounded card was removed |

The source comparison is not a replacement for image comparison. On 2026-08-27 a Samsung SM-S918B was attached and the current debug build was inspected in dark mode at 1440×3088. Captures cover the Workout hub, a planned-day preview, and an existing active set. The device exposed an existing saved session, so no set was completed or modified during this inspection. The visual checklist remains open until matching iOS/Android captures at the same viewport, colour mode and logical workout state are attached.

### 2026-08-27 — Samsung dark-mode findings

- Restored Android's existing warm orange (`#F28A29`) after verifying that iOS `AccentColor2` is a Nutrition-only token; it must not tint Workout.
- Removed the incorrect orange stripe from standard and superset block headers. iOS reserves the section stripe for warmup (`.orange`) and cooldown (`.blue`); supersets are identified by their connector instead.
- Verified on-device that the active standard-set screen uses the white dark-mode primary accent, matching iOS `AccentColor` in dark mode.
- Verified on-device: work-set editor, +/− controls, set progress, `+30s` rest, skip rest, next-exercise context, feedback (`MUY FÁCIL`…`MUY DIFÍCIL`), skip confirmation, duration warning, completion summary and score.
- The disposable run was exited with `DESCARTAR`; the custom workout returned to `EMPEZAR ENTRENAMIENTO`, so no progress/history was retained.
- Al comparar `ActiveWorkoutTimerView.swift`, el botón play/pausa del temporizador de ejercicio se ajustó a icono-only en círculo de 52 dp; el texto queda reservado al botón `+15s`, como en iOS.
- Al comparar `ActiveWorkoutStepperInputRow`, se eliminó la tarjeta anidada que hacía demasiado alto y pesado el editor de repeticiones/peso en Android. Los objetivos ahora forman parte del título de cada fila y los controles se redujeron al tamaño compacto de los símbolos SF de iOS.
- En Galaxy S23 Ultra se eliminó el scroll interno para sesiones de hasta cuatro series (el caso habitual); solo sesiones realmente largas mantienen un contenedor acotado. Esto evita que el editor de reps/kg parezca una pantalla anidada.
- La tipografía del editor se alinea con la decisión visual actual de la app: Exo 2 explícita en títulos, etiquetas y valores, manteniendo proporciones consistentes con el resto de la pantalla Android.
- Revisada la transición de `completeCurrentStep`: cada serie guarda reps/peso, incrementa el progreso, reinicia valores objetivo de la siguiente serie y entra en descanso entre series; el feedback solo aparece al terminar el ejercicio (o la ronda final de superserie), igual que iOS.
- La fila activa se integró dentro de `SetTrackingRows` como la fila expandida de iOS. Antes el editor estaba separado y la primera serie quedaba visualmente oculta; ahora, tras completar, la fila expandida pasa a la siguiente serie.
- El sheet activo usa `heightIn` con el viewport del dispositivo y scroll únicamente cuando el contenido excede ese límite; se evita cortar las esquinas y se conserva visible el texto de siguiente ejercicio mientras haya espacio.
- Las cápsulas `PR` y `ÚLTIMO` se movieron al mismo `Row` de la cápsula `STANDARD`, alineando su borde superior con el bloque contextual de iOS.
- Not yet captured on this device: work timer, supersets and the later XP/streak celebration phases. The available disposable workout had no time-based exercise; these remain source/test verified until a safe fixture exposes them.

## Verification protocol

### 2026-09-28 — rest-sheet next-exercise prescription pass

- Replaced Android's fixed `series · reps` next-exercise summary with the compact typed prescription hierarchy used by iOS `PlannedExercicePrescriptionView`: series, repetitions, target load, duration and distance now appear whenever configured.
- Applied the same compact prescription to each exercise in the next superset round, so timed and distance work is not misrepresented as repetitions.
- Added unit coverage for weighted repetitions and duration-with-distance prescriptions. Pending device screenshot comparison remains required for visual checklist closure.

For each parity milestone:

1. Add or update unit tests for the state transition or calculation.
2. Run `./gradlew :feature:workout:test`.
3. Run Android screenshot tests at the agreed phone viewport in light and dark mode.
4. Capture the matching iOS screen at the same logical state.
5. Compare hierarchy, text wrapping, spacing, geometry, and color; attach differences to the checklist.
6. Do not mark an item complete from visual similarity alone when behavior or restored state differs.

The checklist is intentionally kept in version control so future audits and coding sessions can resume from the same source of truth.
