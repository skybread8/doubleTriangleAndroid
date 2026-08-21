# iOS to Android parity matrix

Baseline: enabled functionality on iOS `main`. Dormant feature flags and roadmap items do not block initial Android parity.

Status values: `Not started`, `Foundation`, `In progress`, `Ready for review`, `Verified`.

| Area | iOS reference | Android target | Status | Acceptance check |
|---|---|---|---|---|
| App shell | `ContentView` | Four root destinations and persistent bottom navigation | Ready for review | All four destinations are reachable |
| Design system | Asset colors, Anton, Exo 2, button modifiers | Compose theme, typography, colors, reusable components | Foundation | Light/dark screenshots match brand tokens |
| Workout hub | `WorkoutHubView` | Header, streak, weekly calendar, mode selector, plan cards | Ready for review | Layout and interactions compared side by side |
| Onboarding | `OnboardingView` and 18 steps | Full flow through local plan preview is persisted; Health Connect imports latest height and weight | In progress | Complete the first slice and enter workout hub |
| Workout plan | Plan, mesocycle, day and exercise models | Room entities plus domain mapping | In progress | Same fixture produces the same plan structure |
| Active workout | Active workout store and views | Foreground-safe workout session | Not started | Start, log, pause, resume and finish |
| Completion | Score, XP, streak and celebrations | Equivalent rules and Compose animations | Not started | Ported unit fixtures have identical results |
| Custom workouts | Custom workout views | Create, edit, run and delete | Not started | CRUD plus active session verified |
| Analytics | Analytics and charts | Compose chart screens | Not started | Same dataset yields equivalent values |
| Profile | Profile, settings, body metrics/photos | Compose profile flows | Not started | Fields and validation match |
| Nutrition | Planner, calendar and seven log methods | Android nutrition verticals | Not started | Each method has an end-to-end test |
| Health | HealthKit | Health Connect | Not started | Permission, import and export scenarios pass |
| Live workout surface | Live Activity / Dynamic Island | Live Update or ongoing notification | Not started | Current workout remains accessible |
| Widgets | Workout and nutrition widgets | Glance widgets | Not started | Key states verified at supported sizes |
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
