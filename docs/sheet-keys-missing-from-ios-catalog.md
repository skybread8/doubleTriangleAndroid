# Keys que faltan en el Sheet y ya usa iOS

Auditoría actualizada el 23/10/2026 tras exportar las pestañas `app` y `exercise_catalog` del Sheet. El catálogo Android contiene ahora 3.226 recursos: esta actualización incorporó 234 keys respecto a la exportación anterior.

## Resultado

- Literales únicos usados por iOS: 1.718
- Con recurso equivalente exportado desde el Sheet: 1.686
- Pendientes reales de añadir al Sheet: 29
- Fragmentos dinámicos o vacíos que no son keys válidas del Sheet: 3

Cada texto de la tabla siguiente se usa en producción en iOS pero todavía no tiene una key equivalente en el Sheet. Debe añadirse con **ese texto exacto** como key para que Android pueda resolverlo automáticamente con el exportador.

## Textos pendientes

| Literal iOS | Fichero iOS |
| --- | --- |
| ` (Target: %@)` | `Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutSceneView.swift` |
| ` (Target: %d)` | `Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutSceneView.swift` |
| `%@ is charged %@. Your subscription renews automatically unless cancelled at least 24 hours before the end of the current period.` | `Wildforce/Views/App/Profile/SubscriptionPaywallView.swift` |
| `%@ scored noticeably lower than your top-trained muscle groups this period.` | `Wildforce/Services/Workouts/Repositories/ExercisesInsights.swift` |
| `%@ week %d` | `Wildforce/Views/App/Training/Workout/Mesocycle/WorkoutMesocycleDetailView.swift` |
| `%@, %@ intake` | `Wildforce/Views/App/Nutrition/SevenDayNutritionCalendarView.swift` |
| `%@, eaten %@ calories, net %@ calories` | `Wildforce/Views/App/Nutrition/NutritionEnergyBalanceCard.swift` |
| `%@, eaten calories unavailable` | `Wildforce/Views/App/Nutrition/NutritionEnergyBalanceCard.swift` |
| `%@. %d activities` | `Wildforce/Views/Components/ConsistencyHeatmapCard.swift` |
| `%d landmarks · %.0f%% avg confidence` | `Wildforce/Views/App/Training/Workout/PoseLandmarks/PoseLandmarkLiveView.swift` |
| `%d of %d planned` | `Wildforce/Views/App/Training/Analytics/AnalyticsView.swift` |
| `%d of %d planned, %d%%` | `Wildforce/Views/App/Training/Analytics/AnalyticsView.swift` |
| `%d of %d sessions centered on %@ this period.` | `Wildforce/Services/Workouts/Repositories/ExercisesInsights.swift` |
| `%d reps • %@ consistency` | `Wildforce/Views/App/Training/Workout/Completion/WorkoutDayCompletionSummary.swift` |
| `+%@ vs previous best` | `Wildforce/Services/Workouts/Analytics/TrainingAnalytics.swift` |
| `+15s` | `Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutTimerView.swift` |
| `+30s` | `Wildforce/Views/App/Training/Workout/Active/ActiveWorkoutSceneView.swift` |
| `+6mo` | `Wildforce/Views/App/Profile/BodyProgress/BodyProgressCompareView.swift` |
| `Current week %d, %@` | `Wildforce/Views/Components/Training/PlanHeaderView.swift` |
| `Latest logged %@` | `Wildforce/Views/App/Profile/BodyMetricsView.swift` |
| `No exercises match \"%@\"` | `Wildforce/Views/App/Training/Analytics/AnalyticsView.swift` |
| `Over target. ` | `Wildforce/Views/App/Nutrition/NutritionDailyProgressCard.swift` |
| `Standard %@ calories, active %@ calories, burned %@ calories` | `Wildforce/Views/App/Nutrition/NutritionEnergyBalanceCard.swift` |
| `Standard calories unavailable, active %@ calories` | `Wildforce/Views/App/Nutrition/NutritionEnergyBalanceCard.swift` |
| `Standard calories unavailable, active %@ calories, eaten %@ calories` | `Wildforce/Views/App/Nutrition/NutritionEnergyBalanceCard.swift` |
| `Week %d, %@` | `Wildforce/Views/Components/Training/PlanHeaderView.swift` |
| `You completed %d of %d planned sessions in this period.` | `Wildforce/Services/Workouts/Repositories/ExercisesInsights.swift` |
| `You completed %d workouts and logged %@ in this period.` | `Wildforce/Services/Workouts/Repositories/ExercisesInsights.swift` |
| `You logged %@ this period versus %@ in the previous one.` | `Wildforce/Services/Workouts/Repositories/ExercisesInsights.swift` |

## No añadir al Sheet

Estas tres coincidencias proceden de interpolaciones Swift o de una llamada vacía; no son keys estáticas exportables. Deben corregirse en el código iOS si se quiere que esa interfaz use el Sheet de forma estricta.

- Cadena vacía en `Wildforce/Views/Onboarding/Steps/OnboardingAgeStepView.swift`.
- Fragmento de interpolación de `BMR adjusted for your ... lifestyle.` en `Wildforce/Views/App/Profile/UserBodyProfileView.swift`.
- Fragmento de interpolación de `Estimated using the U.S. Navy formula ...` en `Wildforce/Views/App/Profile/UserBodyProfileView.swift`.
