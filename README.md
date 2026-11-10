# cs2114-project3-group54
We build GrubHub: dining hall ordering with kiosk and mobile lines, a kitchen
pipeline, a pickup status board, and saved order history.

## Layout (MVC)
| Role | Classes (in `src/`) |
|---|---|
| Model | `DiningHallModel`, `LinkedQueue<T>`, `Order`, `OrderStatus`, `Menu`, `OrderHistory`, `OrderListener` |
| View | `KioskView`, `MobileView` (both extend `OrderFormView`), `StatusBoardView` |
| Controller | `OrderController` |
| Entry point | `GrubHubApp` (builds the halls and runs the terminal menu) |

JUnit 4 tests for every class are in `test/`.

## Rules implemented
- **Fairness:** the kitchen starts up to 3 mobile orders in a row, then 1
  walk-in if one is waiting. If one line is empty, the other is served.
- **Wait time:** orders finish one at a time, `minutesPerOrder` each. Being
  made at kitchen index i: `(i + 1) * minutesPerOrder`. Waiting at predicted
  position p: `(ordersBeingMade + p) * minutesPerOrder`. Ready: 0.
- **Quantity:** 1 to 20.
- **History** is saved to `order_history.txt` on quit and loaded at startup.

## Run
In Eclipse: run `GrubHubApp` as a Java Application; run `test/` as JUnit tests.
