# iOS to Android parity matrix

## Delivery order

Work proceeds strictly from top to bottom. A block only moves to `Verified` when
it builds, passes lint and its applicable automated and device checks. Do not
start a lower-priority block before all higher-priority blocks are verified.

| Priority | Block | Completion gate | Status |
|---|---|---|---|
| P0 | Technical foundation | Consistent design system, lint without errors, durable local data boundary and an accurate parity board | In progress |
| P1 | Mobile workout vertical | Reference workout day, information and active-workout flows are behaviourally and visually verified | Ready for review — automated verification deferred |
| P2 | Analytics and profile | Equivalent metrics, charts and editable profile flows are verified against shared fixtures | Ready for review — automated verification deferred |
| P3 | Nutrition | Planner, calendar and every supported logging path are verified end to end | Foundation — deferred by product decision |
| P4 | Platform and scale | Health Connect write sync, live surface, widgets, Wear OS, cloud sync and localization are verified | In progress |

Baseline: enabled functionality on iOS `main`. Dormant feature flags and roadmap items do not block initial Android parity.

Status values: `Not started`, `Foundation`, `In progress`, `Ready for review`, `Verified`.

| Area | iOS reference | Android target | Status | Acceptance check |
|---|---|---|---|---|
| App shell | `ContentView` | Four root destinations and persistent bottom navigation | Ready for review | All four destinations are reachable |
| Design system | Asset colors, Anton, Exo 2, button modifiers | Compose theme, typography, colors, reusable components | Foundation | Light/dark screenshots match brand tokens |
| Workout hub | `WorkoutHubView` | Header, streak, weekly calendar, mode selector, plan cards | Ready for review | Layout and interactions compared side by side |
| Onboarding | `OnboardingView` and 18 steps | Full flow through local plan preview is persisted; Health Connect imports latest height and weight | In progress | Complete the first slice and enter workout hub |
| Workout plan | Plan, mesocycle, day and exercise models | Room entities plus domain mapping | In progress | Same fixture produces the same plan structure |
| Active workout | Active workout store and views | Foreground-safe workout session | Ready for review | Start, log, pause, resume and finish |
| Completion | Score, XP, streak and celebrations | Equivalent rules and Compose animations | Ready for review | Ported unit fixtures have identical results |
| Custom workouts | Custom workout views | Create, edit, run and delete | Ready for review | CRUD plus active session verified |
| Analytics | Analytics and charts | Compose analytics overview, volume trend and per-exercise history | In progress | Same dataset yields equivalent values |
| Profile | Profile, settings, body metrics/photos | Compose profile, editable settings, avatar and basic weight history | In progress | Fields and validation match |
| Nutrition | Planner, calendar and seven log methods | Manual meal logging, daily macro targets and seven-day summary | Foundation | Each method has an end-to-end test |
| Health | HealthKit | Health Connect | In progress | Import height/weight and export user-confirmed weight and completed strength sessions |
| Live workout surface | Live Activity / Dynamic Island | Ongoing workout and rest notification | In progress | Current workout remains accessible |
| Widgets | Workout and nutrition widgets | Home-screen workout quick-access widget | In progress | Widget opens the app and refreshes plan/session state |
| Watch | watchOS app | Wear OS app and Health Services | Not started | Phone/watch sync and offline workout verified |
| Localization | 1,665 keys across 10 locales | Android string resources | Not started | Missing-key check and UI smoke tests |

## First review checklist

- Run on one compact and one large phone.
- Review light and dark themes.
- Select/deselect every weekday.
- Switch between plan and custom workout modes.
- Open every root destination.
- Record visual differences before extending the data layer.

## Platform rules

- Preserve branding and business behavior.
- Use native Android interaction patterns where iOS APIs have no direct equivalent.
- Keep remote services behind interfaces so local fixtures can run without credentials.
- Do not add credentials to source control.

## Visual parity direction

Applies to training, analytics and profile. Nutrition is explicitly deferred.

- Use a pale lavender/ivory background, clean white cards and charcoal primary actions.
- Reserve orange for progress, chart series and small performance accents; do not use it as a
  general surface color.
- Keep the floating bottom navigation icon-only, with a subtle rounded selected state.
- Workout detail uses a photographic hero, circular back control, charcoal start action and
  `EJERCICIOS` / `INFORMACIÓN` segmented control.
- Analytics begins with the period selector and last-workout context, followed by high-value
  session and volume cards.
- Profile groups its navigation rows into one white rounded list, below avatar, streak and level.
