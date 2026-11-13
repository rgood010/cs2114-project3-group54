# cs2114-project3-group54 — Hungry Hokies (GrubHub)

Hungry Hokies is a dining hall pickup-line ordering system. Students order from a walk-in **kiosk**
or the **mobile** app. Each hall's kitchen starts orders using a fairness rule
(up to 3 mobile orders in a row, then 1 walk-in), makes them, and moves them to
pickup. A **status board** shows the line and the ready orders. Order history is
saved to a file so a student can reorder their last meal.

**Deliverable 3 — First Prototype.** The core logic sits in a queue-backed model
that runs without any view. A thin terminal view and a controller sit on top of
it. There is no GUI yet.

---

## How to run

All source is in `src/`. JUnit 4 tests are in `test/`. In Eclipse, `.classpath`
already includes `test/` and the JUnit 4 library.

### 1. The model by itself (driver, no view)
`ModelDriver` creates a `DiningHallModel` and calls it directly. It uses no
view, no controller and no keyboard input. It places mobile and walk-in orders,
runs the kitchen, prints each order's status, position and wait, and shows
bad input being rejected.

- **Eclipse:** right-click `src/ModelDriver.java` → *Run As → Java Application*.
- **Terminal** (from the repo folder):
  ```bash
  javac -d bin src/*.java && java -cp bin ModelDriver
  ```

### 2. The model through tests (no view)
- **Eclipse:** right-click the `test` folder → *Run As → JUnit Test*.
- `DiningHallModelTest`, `LinkedQueueTest`, `OrderTest`, `MenuTest` and
  `OrderHistoryTest` test the model alone. `OrderControllerTest` and `ViewTest`
  test the layers above it. There are 77 tests and all of them pass.

### 3. The thin terminal view
`GrubHubApp` is the full program. View → controller → model.

- **Eclipse:** right-click `src/GrubHubApp.java` → *Run As → Java Application*,
  then type in the Console.
- **Terminal:**
  ```bash
  javac -d bin src/*.java && java -cp bin GrubHubApp
  ```
- **MVP path to try:** enter a student ID → `k` (kiosk) → `1` and type
  `North Dining Hall` → `2` show menu → `3` order `Pizza`, qty `2` → `7`
  start the order → `8` finish it → `5` look up the order number → `0`
  save and quit. Run it again and choose `4` to reorder your last order.

### Layout (MVC)
| Role | Classes |
|---|---|
| Model | `DiningHallModel`, `LinkedQueue<T>`, `Order`, `OrderStatus`, `Menu`, `OrderHistory`, `OrderListener` |
| View | `KioskView`, `MobileView` (both extend `OrderFormView`), `StatusBoardView` |
| Controller | `OrderController` |
| Entry points | `GrubHubApp` (builds the halls and runs the terminal menu), `ModelDriver` (model-only demo) |

### Rules as built
- **Fairness:** the kitchen starts up to 3 mobile orders in a row, then 1 walk-in
  if one is waiting. If one line is empty, it serves the other line.
- **Wait time:** the kitchen finishes orders one at a time, taking
  `minutesPerOrder` each.
  - An order being made at kitchen index *i* (0-based) waits `(i + 1) × minutesPerOrder`.
  - A waiting order at predicted position *p* waits `(ordersBeingMade + p) × minutesPerOrder`.
  - A ready order waits 0.
- **Quantity:** 1–20. Sold-out and unknown items are rejected.
- **History** is loaded from `order_history.txt` at startup and saved there
  on quit. New order numbers always exceed the largest restored number.

---

## Reflection

### What changed from our specification

**Classes**
| Spec said | We built | Why |
|---|---|---|
| `LinkedQueue<T>` with only `enqueue/dequeue/peek/isEmpty/size` | Same, plus it implements `Iterable<T>` (read-only front-to-rear walk) | `getPosition` and `getWaitTime` must look *inside* the lines without removing anything. Dequeuing and re-enqueuing everything just to read the line would be slow and error-prone. |
| KioskView and MobileView as two separate classes | Both extend a shared abstract `OrderFormView` | The two forms were identical except for their label and the `mobile` flag. One base class removed about 100 duplicated lines. |
| Root-level `diningHallModel.java`, `orderHistory,java` | `src/DiningHallModel.java`, `src/OrderHistory.java` | Eclipse only builds `src/`. Class names were fixed to match the spec, and the empty file with a comma in its name was removed. |
| No entry point in the diagram | Added `GrubHubApp` and `ModelDriver` | We needed something that builds the halls and runs the program, plus a driver that runs the model without a view. |

**Method signatures**
| Spec said | We built | Why |
|---|---|---|
| `OrderController.placeOrder(...)` returns `void` and "reports" errors to the view | Kept `void`. The controller records `getLastMessage()`, `lastActionSucceeded()` and `getLastOrderNum()` | A `void` method has no way to hand back a receipt or an error message. These getters let the view print the result and re-prompt without needing exceptions. |
| `Order` had `getOrderNum/getStudentId/getItem/getStatus/setStatus` | Added `getQty()`, `isMobile()`, a copy constructor, and a package-private constructor that sets a status | Reorder needs the quantity and channel. Saving needs every field. `getLast` must return a *detached copy*. Loading has to restore a saved status. |
| `Menu` had `getItems/inStock/markSoldOut` | Added `find(item)` | Makes item lookup case-insensitive and returns the menu's own spelling, so "pizza" matches "Pizza". |
| `OrderHistory` had `add/getLast/save/load` | Added `getLargestOrderNum()` and `size()` | The model needs `getLargestOrderNum()` so new order numbers are larger than any restored number. |
| `DiningHallModel` had the 7 diagram methods | Added `markSoldOut`, `removeListener`, and read-only queries: `getHallName`, `getMenuItems`, `isInStock`, `isOnMenu`, `getWaitingLine`, `getOrdersBeingMade`, `getReadyOrders`, `getKitchenCapacity` | The status board has to *list* orders, but the spec only had per-order lookups. The queries return **copies**, so a view can't change the real orders. |
| Controller had 4 methods | Added `startNextOrder`, `finishOrder`, `markSoldOut`, `addListener/removeListener`, `getSelectedHall`, `getHallNames` | Without these, the user had no way to reach the kitchen actions. Views register through the controller, which adds them to every hall so they keep getting updates after the user switches halls. |
| `reorderLast(studentId)` | Rejects an ID that isn't the session's student | Otherwise one student could reorder on someone else's account. |

**Where validation lives**
- The spec's validation section said the model throws and the controller
  catches. The boundary section said the controller rejects bad input *before*
  calling the model. **We do both.** The controller checks the item, stock and
  quantity first and gives a friendly message. The model checks everything
  again and throws, so it is still safe when called without the controller
  (as in `ModelDriver` and the tests).
- Non-numeric quantity (`"abc"`) is caught in the view (`NumberFormatException`)
  and re-prompted before the controller is called, as the spec planned.
- **The bad input named in our scope**, and how we handle it:
  | Scope said | What happens now |
  |---|---|
  | Sold-out item → "Not available", let the user pick something else | `OrderController` checks `isInStock` first and the model throws `IllegalStateException` as a backup. The message starts with **"Not available:"** and the view asks for the order again. |
  | More than 20 → "Invalid input: Re-enter input", prompt to retype | `OrderController` checks 1–20 first and the model throws `IllegalArgumentException` as a backup. The message starts with **"Invalid input: Re-enter input"** and the view asks again. A quantity that isn't a number gets the same message from the view. |

  We also reject things the scope didn't mention: unknown items, quantity 0 or
  less, a blank student ID, an unknown hall, an unknown order number, starting
  an order when the kitchen is full, and a malformed history file.

**Wait-time rule**
- The spec referred to "the rule in Section 4", but that section was never
  written. We chose the sequential rule above and documented it in
  `DiningHallModel`'s Javadoc so it can be changed in one place.

**Test cases**
- The spec planned one normal and one bad case per method. We kept those and
  added tests for:
  - the 3-mobile/1-walk-in ordering
  - wait times at each stage
  - save → load round trip
  - load of a malformed file leaving history unchanged
  - order numbers above restored ones
  - listener registered twice notified once
  - views re-prompting on bad quantity
- The test plan's example "order 5 is third in line with a 10-minute wait"
  can't be true under our formula with an empty kitchen (third in line = 15
  min). The tests use values that follow the formula we built.

### Did the model really stand alone?
Yes. No model class imports or refers to a view or controller class. The model
only knows `OrderListener`, which is an interface in the model layer. It never
reads the keyboard or prints, and `ModelDriver` and the model tests run it with
no view at all.

Two compromises we're aware of:
- **Views read through the real model object.** Views call
  `controller.getSelectedHall()` and use the model's read-only methods. The spec
  allows this ("views can ask the model..."), but nothing in Java *stops* a view
  from calling a mutating method. Only our convention prevents it. A read-only
  interface would enforce it.
- **The menu loop lives in `GrubHubApp`.** It handles both building the objects
  and printing the menu. It should probably move into its own small view class
  when the GUI is added.

Nothing had to move across the model/view line to make the program run.

### What happened to our unknowns
Our scope said we didn't yet know *"the model view controller stuff and probably
the testing relating to it,"* and that we hadn't built our own queue inside a
project before.

| Unknown from the scope | Status |
|---|---|
| **Model-view-controller** | **Figured out.** The model (`DiningHallModel` and its helpers) holds all state and logic. The views only display and read input. `OrderController` connects them. The observer pattern (`OrderListener`) keeps every screen up to date without the model knowing about any view class. See "Did the model really stand alone?" above. |
| **Testing with MVC** | **Mostly figured out.** The model is tested with no view (`DiningHallModelTest` and the other model tests, plus `ModelDriver`). The controller is tested against real models. Views take a `Scanner`/`PrintStream`, so `ViewTest` can type input and read output. **Still open:** the menu loop in `GrubHubApp` has no tests, and we don't yet know how we'll test a GUI. |
| **Building our own queue in a project** | **Figured out.** `LinkedQueue<T>` is the singly linked FIFO queue the scope described, with constant-time add at the back and remove at the front. We added `Iterable` so the model can read the line without changing it. |

Questions that came up later while writing the spec:

| Unknown | Status |
|---|---|
| How to compute a position when two lines feed one kitchen | **Figured out.** The model copies both lines and simulates the fairness rule to get the predicted order (`predictedLine()`). |
| How to estimate wait time across both lines | **Figured out (our own rule).** The sequential formula above. It's still a simplification, because a real kitchen makes `kitchenCapacity` orders in parallel. |
| How to keep order history between runs | **Figured out.** A tab-separated text file. The whole file is validated before the history is replaced. |

### What doesn't work yet
- **No pickup.** Ready orders stay in `readyQueue` forever. The spec notes there
  is no pickup/removal command.
- **No staff/student separation.** Kitchen actions (start, finish, sold out) are
  in the same terminal menu as student ordering.
- **History saves only on a clean quit** (`0`). If the program crashes or the
  window is closed, that run's orders are lost.
- **Order lookup only searches the selected hall.** An order placed at another
  hall shows "Order not found." until you switch to that hall.
- **Status board redraws after every change.** With several views active, the
  terminal gets noisy.
- **Wait time ignores parallel cooking**, as described above.
- **No tests for `GrubHubApp`'s menu loop.** It is only checked by running it.
- **Scope stretch goals not built yet:** most-ordered items, total spending,
  color-coded status and the GUI (see Next Steps).
- All 77 existing tests pass. No known crash on the MVP path or on the bad-input
  cases from our scope (sold-out item, quantity over 20). The same goes for the
  extra cases we added (non-numeric quantity, unknown item, hall or order
  number).

---

## Next Steps

### Left to finish the MVP (in order)
1. **Pickup:** add a way to remove a ready order when the student collects it,
   so `readyQueue` doesn't grow forever.
2. **Save after every accepted order** instead of only on quit.
3. **Look up an order at any hall**, not just the selected one.
4. **Separate the kitchen screen** (its own small view) from the student menu.
5. **Move the menu loop** out of `GrubHubApp` into a view class, so that class
   only builds objects.

### Change feature for Deliverable 4: **Cancel a waiting order**
A student can cancel their own order while it is still `WAITING`. It leaves the
line, everyone behind it moves up, and wait times drop. An order already
`BEING_MADE` or `READY` can't be cancelled. This changes model state and logic
(removing from the *middle* of a queue, plus a new status), and the user reaches
it through the controller and view, so it counts as non-trivial.

**Predicted to change**
| Class | Layer | Predicted change |
|---|---|---|
| `LinkedQueue<T>` | Model | New `boolean remove(T item)` to unlink a node from anywhere in the list, updating `front`, `rear` and `count`. |
| `OrderStatus` | Model | New value `CANCELLED`. |
| `Order` | Model | `setStatus` allows `WAITING → CANCELLED` only. |
| `DiningHallModel` | Model | New `void cancelOrder(int orderNum, String studentId)`. It removes the order from `mobileQueue` or `walkInQueue`, sets it to `CANCELLED` and notifies listeners. Unknown number → `IllegalArgumentException`, wrong student or not `WAITING` → `IllegalStateException`. `getPosition` and `getWaitTime` handle `CANCELLED`. |
| `OrderController` | Controller | New `void cancelOrder(int orderNum)` using the session's student ID and reporting the result through `getLastMessage()`. `lookupStatus` shows cancelled orders. |
| `OrderFormView` | View | New `cancelOrder()` that reads an order number and calls the controller. |
| `GrubHubApp` | Entry | One new menu option. |

**Predicted *not* to change:** `KioskView`, `MobileView`, `StatusBoardView`,
`Menu`, `OrderHistory`, `OrderListener`. The status board should drop
cancelled orders automatically, because it just redraws from the model after
`modelChanged()`. `OrderHistory` stores the same `Order` object, so the status
change shows up in history without any edits there. If either of those turns
out wrong, we'll record it in Deliverable 4.

### Stretch goals
These are the stretch goals from our scope. The spec moved the first three into
the MVP.

| Scope stretch goal | Built so far | Decision |
|---|---|---|
| **Persistent Order History:** save by student ID, reorder last meal | Built (`OrderHistory.save/load`, `reorderLast`) | Done |
| …view most-ordered items | Not built | **Committing.** Small change: one new `OrderHistory` method plus a controller method and a view method. |
| …see total spending | Not built | **Dropping.** Menu items have no prices, so we'd have to add prices to `Menu`, `Order` and the save file. |
| **Mobile Order Scheduling:** two FIFO lines, 3 mobile then 1 in-person, wait times use both lines | Built | Done |
| **Order Status Pipeline:** waiting → being made → ready, kitchen limit, look up by order number | Built | Done |
| …color-coded status | Not built (terminal only) | **Committing with the GUI.** |
| **GUI:** separate in-person and mobile screens, plus a pickup display | Not built | **Committing if time allows**, after the change feature. Each screen would be a new `OrderListener` view calling the same controller, so the model shouldn't need changes. That makes it the best test of our MVC design. |
| Pickup / clearing ready orders (not in scope; we found it while building) | Not built | **Committing.** It's MVP step 1 above. |
| Second status board / parallel-kitchen wait times (came up in the spec) | Not built | **Dropping** for now. |

### New tests needed
- `LinkedQueueTest`: `remove` at the front, middle and rear; removing the only
  item clears both ends; removing a missing item returns false; `remove(null)`.
- `OrderTest`: `WAITING → CANCELLED` is allowed. `BEING_MADE → CANCELLED` and
  `READY → CANCELLED` throw `IllegalStateException`.
- `DiningHallModelTest`:
  - normal cancel updates the positions of orders behind it and notifies
  - cancel an unknown number → `IllegalArgumentException`
  - cancel someone else's order or a non-waiting order → `IllegalStateException`
  - cancelling keeps the fairness counter correct
- `OrderControllerTest`: cancel reports success or a clear message (no hall,
  not found, already being made).
- `ViewTest`: non-numeric order number re-prompts.
- Tests for the pickup and save-after-each-order changes.

### Who does what
| Item | Owner |
|---|---|
| `LinkedQueue.remove` + tests | Jonathan |
| `OrderStatus`/`Order` cancel rules + `OrderTest` updates | Isabella |
| `DiningHallModel.cancelOrder` + pickup + model tests | Ryan |
| Controller/view/menu wiring for cancel & pickup, kitchen view split, controller/view tests | Shelby |
| Save-after-each-order, any-hall lookup, README Deliverable 4 comparison | Everyone (split at next meeting) |

---

## Attribution

| File | Who | How |
|---|---|---|
| `src/LinkedQueue.java` | Jonathan | Wrote the first version (linked nodes, enqueue/dequeue/isEmpty/size). Finished with GenAI (peek, null checks, iterator). Reviewed. |
| `src/DiningHallModel.java` | Jonathan | Wrote the initial field stub. GenAI-assisted implementation from our spec. Reviewed. |
| `src/Order.java` | Jonathan | GenAI-assisted from the spec. Reviewed. |
| `src/OrderStatus.java` | Jonathan | GenAI-assisted from the spec. |
| `src/OrderListener.java` | Jonathan | GenAI-assisted from the spec. |
| `src/Menu.java` | Jonathan | GenAI-assisted from the spec. Reviewed. |
| `src/OrderHistory.java` | Jonathan | GenAI-assisted from the spec. Reviewed. |
| `src/OrderController.java` | Ryan | Generated with GenAI from the spec. Ryan reviewed it. |
| `src/OrderFormView.java` | Shelby | Generated with GenAI. Shelby reviewed it. |
| `src/KioskView.java` | Shelby | Generated with GenAI. Shelby reviewed it. |
| `src/MobileView.java` | Ryan | Generated with GenAI. Ryan reviewed it. |
| `src/StatusBoardView.java` | Shelby | Generated with GenAI. Shelby reviewed it. |
| `src/GrubHubApp.java` | Ryan | Generated with GenAI. Ryan reviewed it and ran the MVP path by hand. |
| `src/ModelDriver.java` | Jonathan | GenAI-assisted. Ran it. |
| `src/RyanTest.java` | Ryan | Wrote it while setting up the Eclipse Java project (`.project`, `.classpath`). |
| `test/LinkedQueueTest.java` | Jonathan | GenAI-assisted from the spec test plan. Ran it. |
| `test/OrderTest.java` | Isabella, Jonathan | Jonathan: GenAI-assisted first version. Isabella: expanded, restructured and added the stage-skipping exception tests. |
| `test/MenuTest.java` | Jonathan | GenAI-assisted from the spec test plan. Ran it. |
| `test/OrderHistoryTest.java` | Jonathan | GenAI-assisted from the spec test plan. Ran it. |
| `test/DiningHallModelTest.java` | Jonathan | GenAI-assisted from the spec test plan. Ran it. |
| `test/OrderControllerTest.java` | Jonathan | GenAI-assisted from the spec test plan. Ran it. |
| `test/ViewTest.java` | Jonathan | GenAI-assisted. Ran it. |

**GenAI note:** We used Claude (Anthropic) in Claude Code. We gave it our
Deliverable 2 specification and our partial `LinkedQueue`, and it wrote most of
the class and test code from that spec. It also drafted this README. We
reviewed the generated code against the spec, ran the tests and the program,
and made our own edits (for example `OrderTest`).
