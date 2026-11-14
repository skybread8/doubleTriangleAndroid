# Repository instructions

## iOS behavior

Use the sibling `../doubleTriangle` iOS app as the product reference for Android changes. Check the relevant iOS flow before implementing, preserve platform conventions where necessary, and report any remaining differences rather than claiming unverified parity.

## Workout parity

For every task that reads or changes the workout detail screen, active workout session, repetitions or weight entry, timers, rest, supersets, exercise completion, or workout celebrations, read `docs/IOS_WORKOUT_PARITY.md` first and treat the sibling iOS implementation identified there as the canonical product behavior.

Do not claim 100% iOS workout parity until every item in that document's definition and checklist is verified. Update the checklist and its tests whenever parity work is completed.

Preserve unrelated user changes in the working tree.
