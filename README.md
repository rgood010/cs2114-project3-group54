# cs2114-project3-group54 — Hungry Hokies (GrubHub)

Hungry Hokies is a dining hall pickup-line ordering system. Students order from
a walk-in **kiosk** or the **mobile** app. Each hall's kitchen starts orders
using a fairness rule (up to 3 mobile orders in a row, then 1 walk-in), makes
them, and moves them to pickup. A **status board** shows the line and the ready
orders. Order history is saved to a file so a student can reorder their last
meal.

**Deliverable 4 — Second Prototype + Tests.** The Swing GUI is connected to the
model through the controller. Our change feature, **cancel a waiting order**,
runs from the queue up to both the GUI and the terminal view. 137 JUnit tests
pass.

---

## How to run

All source is in `src/` and all JUnit 4 tests are in `test/`. Everything is
in the default package. In Eclipse, `.classpath` already includes `test/` and
the JUnit 4 library.

### 1. The GUI (main program)
`GrubHubGUI` opens one window: the order form (kiosk or mobile) and the
kitchen controls on the left, and the color-coded status board on the right.

- **Eclipse:** right-click `src/GrubHubGUI.java` → *Run As → Java Application*.
- **Terminal** (from the repo folder):
  ```bash
  javac -d bin src/*.java && java -cp bin GrubHubGUI
  ```
- **Try it:**
  1. Enter a student ID and pick *Kiosk* or *Mobile*.
  2. Order `Pizza` with quantity `2`, then `Fries` and `Salad`.
  3. Type an order number in **Order #** and click **Cancel order**. That
     order leaves the line and everyone behind it moves up.
  4. Use **Start next order** and **Finish order**. The board and the
     "Your order" line update by themselves.

  History is saved to `order_history.txt` when you close the window.

### 2. The tests
- **Eclipse:** right-click the `test` folder → *Run As → JUnit Test*.
- **Terminal** (the JUnit and Hamcrest jars ship with Eclipse; set `J` and `H`
  to their paths):
  ```bash
  javac -d bin -cp "$J" src/*.java test/*.java && java -cp "bin:$J:$H" org.junit.runner.JUnitCore LinkedQueueTest OrderTest MenuTest OrderHistoryTest DiningHallModelTest OrderControllerTest ViewTest GuiTest
  ```
- There are **137 tests and all of them pass.** The model tests
  (`LinkedQueueTest`, `OrderTest`, `MenuTest`, `OrderHistoryTest`,
  `DiningHallModelTest`) use no view or controller. `GuiTest` also passes with
  `-Djava.awt.headless=true`.

### 3. The terminal version and the model-only driver
- `GrubHubApp` is the same program in the terminal (option `11` cancels an
  order).
  ```bash
  javac -d bin src/*.java && java -cp bin GrubHubApp
  ```
- `ModelDriver` runs the model with no view or controller at all.
  ```bash
  javac -d bin src/*.java && java -cp bin ModelDriver
  ```

### Layout (MVC)
| Role | Classes |
|---|---|
| Model | `DiningHallModel`, `LinkedQueue<T>`, `Order`, `OrderStatus`, `Menu`, `OrderHistory`, `OrderListener` |
| Controller | `OrderController` |
| View (Swing) | `KioskPanel`, `MobilePanel` (both extend `OrderFormPanel`), `StatusBoardPanel`, `KitchenPanel`, `MenuComboModel` (dropdown helper) |
| View (terminal) | `KioskView`, `MobileView` (both extend `OrderFormView`), `StatusBoardView` |
| Entry points | `GrubHubGUI` (Swing window), `GrubHubApp` (terminal menu; also builds the halls for both apps with `createHalls`), `ModelDriver` (model-only demo) |

### How the view is connected to the model
- **Actions go view → controller → model.** Each button calls one
  `OrderController` method (`placeOrder`, `reorderLast`, `cancelOrder`,
  `lookupStatus`, `startNextOrder`, `finishOrder`, `markSoldOut`). The
  controller checks the input, calls the selected `DiningHallModel`, and
  records a message the view shows.
- **The model tells the views when it changes.** Every panel is an
  `OrderListener`. After any change, the model calls `modelChanged()` and each
  panel redraws on the Swing thread.
- **The views keep no copy of model state.**
  - The item dropdowns use `MenuComboModel`, and the board lists use
    `BoardListModel`. Both ask the model for their rows every time Swing
    draws, instead of holding their own lists.
  - The "Your order" line and the kitchen counts are rebuilt from the model
    on each change.
  - The terminal `StatusBoardView` builds its rows when it prints them; it
    used to cache them in two lists.
  - The only thing a view remembers is view state: which item is selected,
    what was typed, and the number of the order it placed, so it knows which
    order to show.

### Rules as built
- **Fairness:** the kitchen starts up to 3 mobile orders in a row, then 1
  walk-in if one is waiting. If one line is empty, it serves the other line.
- **Wait time:** the kitchen finishes orders one at a time, taking
  `minutesPerOrder` each.
  - An order being made at kitchen index *i* (0-based) waits `(i + 1) × minutesPerOrder`.
  - A waiting order at predicted position *p* waits `(ordersBeingMade + p) × minutesPerOrder`.
  - A ready or cancelled order waits 0.
- **Cancel (new in D4):**
  - Only the student who placed an order can cancel it, and only while it is
    `WAITING`.
  - It is unlinked from its line (`LinkedQueue.remove`), and the order behind
    it moves up.
  - Its status becomes `CANCELLED`, and that is saved in history.
  - The fairness counter doesn't change, because the order was never started.
- **Quantity:** 1–20. Sold-out and unknown items are rejected.
- **History** is loaded from `order_history.txt` at startup and saved on quit.
  New order numbers always exceed the largest restored number.

### Bad input, and the test that covers each case
Every case is rejected with a message, and the program keeps running. The
controller checks first and gives a friendly message. The model checks again
and throws, so it is safe even without the controller.

| Bad input | What the user sees | Tests |
|---|---|---|
| Sold-out item *(scope)* | "Not available: Salad is sold out. Please pick another item." | `DiningHallModelTest.placeOrderSoldOutThrowsAndDoesNotNotify`, `OrderControllerTest.placeOrderSoldOutIsReported`, `GuiTest.soldOutItemIsRejected` |
| Quantity over 20 *(scope)* | "Invalid input: Re-enter input (quantity must be between 1 and 20)." | `DiningHallModelTest.placeOrderQuantityOver20Throws`, `OrderControllerTest.placeOrderTooManyIsReported`, `ViewTest.mobileReadOrderRepromptsAfterRejection`, `GuiTest.quantityOverTwentyIsRejected` |
| Quantity 0 | same message | `DiningHallModelTest.placeOrderQuantityZeroThrows` |
| Quantity not a number ("abc") | "Invalid input: Re-enter input ("abc" is not a whole number)." | `ViewTest.kioskReadOrderRepromptsBadQuantity`, `GuiTest.nonNumberQuantityIsRejected` |
| Unknown item | "… is not on the … menu" | `DiningHallModelTest.placeOrderUnknownItemThrows`, `OrderControllerTest.reorderLastItemNotAtThisHallIsReported` |
| Blank student ID | rejected | `DiningHallModelTest.placeOrderBlankIdThrows` |
| Unknown hall | previous hall stays selected | `OrderControllerTest.selectUnknownHallKeepsPrevious` |
| Unknown or non-numeric order number in a lookup | "Order not found." / "Order numbers are whole numbers." | `DiningHallModelTest.getStatusUnknownThrows`, `GuiTest.lookUpHandlesBadAndUnknownNumbers` |
| Reorder with no history, or for another student | message | `OrderControllerTest.reorderLastNoHistoryIsReported`, `OrderControllerTest.reorderLastWrongStudentIsReported` |
| Kitchen full, or nothing to start or finish | message | `DiningHallModelTest.startNextOrderKitchenFullThrows`, `DiningHallModelTest.finishOrderEmptyThrows`, `OrderControllerTest.kitchenCommandsReportResults`, `GuiTest.kitchenReportsFullKitchenAndCounts` |
| Malformed or missing history file | old history kept, message shown | `OrderHistoryTest.loadMalformedLeavesHistoryUnchanged`, `OrderHistoryTest.loadMissingFileThrows` |
| **Cancel: order number not a number** *(new)* | "Invalid input: Re-enter input …" and re-prompt | `ViewTest.cancelRepromptsNonNumberThenCancels`, `GuiTest.cancelButtonRejectsBadNumbers` |
| **Cancel: unknown order** *(new)* | "Order not found." | `DiningHallModelTest.cancelUnknownOrderThrows`, `OrderControllerTest.cancelUnknownOrderIsReported` |
| **Cancel: someone else's order** *(new)* | "Order #n was placed by another student." | `DiningHallModelTest.cancelOtherStudentsOrderThrowsAndKeepsIt`, `OrderControllerTest.cancelOtherStudentsOrderIsReported` |
| **Cancel: already being made, ready or cancelled** *(new)* | "Order #n is BEING_MADE and can no longer be cancelled." | `DiningHallModelTest.cancelBeingMadeOrderThrows`, `DiningHallModelTest.cancelTwiceThrows`, `OrderControllerTest.cancelOrderBeingMadeIsReported`, `OrderTest.readyOrderCannotBeCancelled` |
| **Cancel: blank student ID, or no hall selected** *(new)* | rejected / "Select a dining hall first." | `DiningHallModelTest.cancelBlankStudentThrows`, `OrderControllerTest.cancelWithNoHallIsReported` |

### Model and queue tests: one normal and one bad-input case per method
| Class.method | Normal case | Bad-input case |
|---|---|---|
| `LinkedQueue.enqueue` | `enqueueAddsToRear` | `enqueueNullThrows` |
| `LinkedQueue.dequeue` | `dequeueReturnsFrontAndRemovesIt` | `dequeueEmptyThrows` |
| `LinkedQueue.peek` | `peekDoesNotRemove` | `peekEmptyThrows` |
| `LinkedQueue.isEmpty/size` | `isEmptyAndSize` | (no input; the empty case is in the same test) |
| `LinkedQueue.iterator` | `iteratesFrontToRear` | `iteratorPastEndThrows` |
| `LinkedQueue.remove` *(new)* | `removeFront`, `removeMiddleLinksNeighbours`, `removeRearMovesRearBack`, `removeOnlyItemClearsBothEnds` | `removeNullThrows`, `removeMissingReturnsFalse` |
| *Interleaved enqueue/dequeue* | `interleavedEnqueueDequeueKeepsFifoOrder`, `removeBetweenInterleavedOperations` | |
| `Order` constructor | `gettersReturnFields` | `constructorRejectsBlankStudentId` (+ null and empty) |
| `Order.setStatus` | `statusMovesForwardOneStage`, `waitingOrderCanBeCancelled` | `skippingStageThrowsAndKeepsStatus`, `nullStatusThrowsAndKeepsStatus`, `beingMadeOrderCannotBeCancelled`, `cancelledOrderCannotMoveOn` |
| `Menu` constructor / `find` | `findReturnsCanonicalName` | `blankMenuItemThrows`, `findUnknownOrBlankReturnsNull` |
| `Menu.inStock` / `markSoldOut` | `inStock` | `markUnknownSoldOutThrows`, `markNullSoldOutThrows` |
| `OrderHistory.add` / `getLast` | `addAndGetLastReturnsDetachedCopy` | `addNullThrows`, `getLastNoHistoryThrows`, `getLastBlankIdThrows` |
| `OrderHistory.save` / `load` | `saveThenLoadRoundTrips`, `cancelledStatusSurvivesSaveAndLoad` | `saveBlankPathThrows`, `loadMalformedLeavesHistoryUnchanged`, `loadMissingFileThrows` |
| `OrderHistory.size` / `getLargestOrderNum` | `sizeCountsEveryOrder`, `newOrderNumbersExceedRestoredNumbers` | |
| `DiningHallModel` constructor | (`setUp`) | `constructorRejectsBadArguments` |
| `DiningHallModel.placeOrder` | `placeOrderQueuesRecordsAndNotifies` | `placeOrderQuantityOver20Throws`, `placeOrderSoldOutThrowsAndDoesNotNotify`, … |
| `DiningHallModel.startNextOrder` | `startNextOrderMovesToBeingMade`, `fairnessThreeMobileThenOneWalkIn` | `startNextOrderKitchenFullThrows`, `startNextOrderNothingWaitingThrows` |
| `DiningHallModel.finishOrder` | `finishOrderMovesFrontToReady` | `finishOrderEmptyThrows` |
| `DiningHallModel.cancelOrder` *(new)* | `cancelRemovesOrderAndMovesLineUp`, `cancelledOrderIsNeverStarted`, `cancelKeepsFairnessCounter` | `cancelUnknownOrderThrows`, `cancelBlankStudentThrows`, `cancelOtherStudentsOrderThrowsAndKeepsIt`, `cancelBeingMadeOrderThrows`, `cancelTwiceThrows` |
| `DiningHallModel.markSoldOut` | `markSoldOutNotifies` | `markSoldOutUnknownThrows` |
| `DiningHallModel.getPosition` / `getWaitTime` / `getStatus` | `fairnessThreeMobileThenOneWalkIn`, `waitTimes` | `getPositionUnknownThrows`, `getWaitTimeUnknownThrows`, `getStatusUnknownThrows`, `orderAtOtherHallIsUnknownHere` |
| `DiningHallModel.addListener` / `removeListener` | `addListenerTwiceNotifiesOnce`, `removeListenerStopsNotifications` | `addNullListenerThrows` |
| `DiningHallModel` list getters | `listGettersReturnCopies` | |

All of these are in the model test classes and call the model directly, not
the UI. `OrderControllerTest`, `ViewTest` and `GuiTest` test the layers above.

---

## Reflection

### How did our design absorb the change?
The change feature is the one we named in our Deliverable 3 Next Steps:
**cancel a waiting order.** We did not switch features.

**Classes the feature actually touched, compared with our D3 prediction**
| Class | Predicted (D3) | What actually happened |
|---|---|---|
| `LinkedQueue<T>` | New `boolean remove(T item)` | **Matched.** `remove` unlinks a node anywhere and fixes `front`, `rear` and `count`. We also decided `remove(null)` throws, like `enqueue(null)`. |
| `OrderStatus` | New `CANCELLED` | **Matched.** |
| `Order` | `setStatus` allows `WAITING → CANCELLED` only | **Matched, with a surprise.** The old rule was "the new status must be the next ordinal". `CANCELLED` comes right after `READY`, so `READY → CANCELLED` would have passed that check. We had to make cancel its own rule, and also block any move *out* of `CANCELLED`. |
| `DiningHallModel` | New `cancelOrder(orderNum, studentId)`; `getPosition`/`getWaitTime` handle `CANCELLED` | **Matched, but less work than expected.** `getPosition` already returned 0 for anything not `WAITING`, so it needed no change. Only `getWaitTime` needed one new `case`. The fairness counter also needed no change (tested in `cancelKeepsFairnessCounter`). |
| `OrderController` | New `cancelOrder(orderNum)`; `lookupStatus` shows cancelled orders | **Matched.** |
| `OrderFormView` (terminal) | New `cancelOrder()` | **Matched.** |
| `GrubHubApp` | One new menu option | **Matched** (option `11`). |
| `OrderFormPanel` (GUI) | Not predicted: the GUI didn't exist in D3 | **New in D4.** Added a **Cancel order** button next to **Look up**. The "Your order" line shows `CANCELLED` without position and wait. |

**Predicted *not* to change for the feature**
- `KioskView`, `MobileView`, `Menu`, `OrderHistory` and `OrderListener` did
  not change.
- The GUI classes `KioskPanel` and `MobilePanel` did not change either. The
  shared base class absorbed the feature for both channels, which is the
  payoff we hoped for when we merged the two forms.
- The status board dropped cancelled orders with **no code for cancelling**.
  It redraws from the model on `modelChanged()`, and a cancelled order is no
  longer in either line. This is tested in
  `ViewTest.statusBoardDropsCancelledOrder` and
  `GuiTest.cancelButtonCancelsAndBoardDropsOrder`.
- `OrderHistory` saved the `CANCELLED` status with no edits. It holds the same
  `Order` object, and `load` already used `OrderStatus.valueOf`. This is
  tested in `cancelledStatusSurvivesSaveAndLoad`.

`StatusBoardView` and `StatusBoardPanel` *were* edited in D4, but not because
of cancel. We removed their cached rows so the views keep no copy of model
state (see "How the view is connected to the model").

**What surprised us:** the `Order` ordinal rule above, and how little
`DiningHallModel` needed beyond the new method.

### Did the change cross the model/view line?
**No.** All of the cancel logic lives in the model:
- who may cancel
- which status allows it
- removing the order from the line
- the new positions and waits

The views learned only two things:
- one new controller method, `cancelOrder(int)`
- that `CANCELLED` is a status worth showing differently (the order form hides
  position and wait for it)

No view touches a queue or decides whether a cancel is allowed. The model
still imports no view or controller class, and every model test runs without
one. The change did not ripple anywhere it shouldn't have.

**A design change that would make the next feature cheaper:** move the
transition rules into `OrderStatus` itself (for example
`boolean canMoveTo(OrderStatus next)`, backed by a small table).
- Our next feature, pickup, adds a `PICKED_UP` status. It would then be one
  enum change instead of another special case in `Order.setStatus`. That
  special case is exactly what surprised us this time.
- A second improvement: give views a read-only interface for a hall instead of
  the real `DiningHallModel`. A future view then *can't* call a mutating
  method, even by accident.

### What changed from our D3 plan, and why
| Area | D3 plan | What we did | Why |
|---|---|---|---|
| Features | GUI was a stretch goal "if time allows, after the change feature" | Built the Swing GUI **before** the cancel feature, then added cancel to both GUI and terminal | D4 asks for the view to be connected to the model, and the GUI was the view we wanted to demo. It also tested our MVC claim: the model needed **no** changes for the GUI. |
| Features | MVP step 1 was pickup | Pickup is **not built yet** | We put the change feature and the GUI first, since this deliverable requires them. Pickup is first for the final build. |
| Features | Separate kitchen screen | Partly done: the kitchen is its own `KitchenPanel`, but it is still in the same window | Same window keeps the 3-minute demo simple. A separate window is listed for the final build. |
| Signatures | `LinkedQueue.remove(T)`, `DiningHallModel.cancelOrder(int, String)`, `OrderController.cancelOrder(int)`, `OrderFormView.cancelOrder()` | Same signatures | — |
| Signatures | — | Added `GrubHubApp.createHalls(OrderHistory)` | So the terminal app and the GUI build identical halls. |
| Validation | Unknown → `IllegalArgumentException`; wrong student or not waiting → `IllegalStateException` | Same, plus a blank student ID → `IllegalArgumentException`, and cancelling twice → `IllegalStateException` | Found while writing the tests. |
| Validation | — | A non-numeric order number gets the scope's "Invalid input: Re-enter input" wording | To match the bad-input wording from our scope. |
| Tests | Every test listed in D3's "New tests needed" for cancel | All written, plus: interleaved enqueue/dequeue tests, a test for removing between interleaved operations, the fairness counter after a cancel, the remaining model methods (`markSoldOut`, `removeListener`, constructor, list getters, `Menu.find`), and `GuiTest` (20 tests) | To reach one normal and one bad-input test per model method, and to cover the GUI. |
| Tests | Tests for pickup and save-after-each-order | Not written | Those features aren't built yet. |
| Views | — | Views no longer keep copies of model lists (`MenuComboModel`, `BoardListModel`, `StatusBoardView`) | So the view always shows the model's current state and can't drift from it. |

### What doesn't work yet
- **No pickup.** Ready orders stay in `readyQueue` and on the board forever.
- **History saves only on a clean quit** (closing the GUI window, or `0` in
  the terminal). If the program crashes, that run's orders are lost.
- **Order lookup and cancel only search the selected hall.** An order placed at
  another hall shows "Order not found." until you switch to that hall. The
  GUI's "Your order" line does follow the order.
- **A cancel can't be undone**, and cancelling needs the order number (there's
  no "my orders" list yet).
- **Kitchen and student controls share one window.**
- **Wait time ignores parallel cooking.** A kitchen with capacity 2 still adds
  `minutesPerOrder` per order ahead.
- **Not tested automatically:** the terminal menu loop in `GrubHubApp` and the
  GUI's start-up dialog. They are checked by running them.
- **Stretch goal not built yet:** most-ordered items.
- **No failing tests.** All 137 pass. We know of no crash on the bad-input
  cases above.

---

## Next Steps

### What is left for the final build (in order)
1. **Pickup.** Add a `PICKED_UP` status, `DiningHallModel.pickUpOrder(orderNum)`
   (using `LinkedQueue.remove` on `readyQueue`), a controller method, and a
   **Picked up** button on the board, plus tests.
2. **Save after every accepted order or cancel**, not only on quit.
3. **Look up and cancel at any hall**, not just the selected one.
4. **Separate kitchen window** from the student window.
5. **Most-ordered items** (stretch goal below).
6. **Cleanup:** move transition rules into `OrderStatus.canMoveTo` and give
   views a read-only hall interface (see Reflection).

### Stretch goals
| Scope stretch goal | Status | Decision |
|---|---|---|
| Persistent order history and reorder last meal | Built | Done |
| …view most-ordered items | Not built | **Committing.** One `OrderHistory` method, one controller method and a small GUI list. |
| …see total spending | Not built | **Dropping.** Menu items have no prices. Adding them would change `Menu`, `Order` and the save file. |
| Mobile order scheduling (3 mobile, then 1 walk-in) | Built | Done |
| Order status pipeline + color-coded status | Built (`StatusBoardPanel`) | Done |
| GUI with in-person and mobile screens and a pickup display | Built (`GrubHubGUI`) | Done; the pickup button comes with step 1 |
| Separate kitchen window | Partly built | **Committing** (step 4) |
| Parallel-kitchen wait times / second status board | Not built | **Dropping** |

### Our 3-minute demo (timed)
Delete `order_history.txt` before the demo so order numbers start at 1.

| Time | What we show |
|---|---|
| 0:00–0:20 | Run `GrubHubGUI`, enter a student ID, pick **Kiosk**. Point out the order form, kitchen and status board. |
| 0:20–0:50 | Order Pizza ×2 (#1), Fries ×1 (#2) and Salad ×1 (#3). The board fills in; "Your order #3" shows position 3, ~15 min. |
| 0:50–1:30 | **New feature.** Cancel #2: it leaves the board, Salad moves up to position 2 and its wait drops to ~10 min. Click **Start next order**, then try to cancel #1 → "can no longer be cancelled". Type `abc` and cancel → "Invalid input". |
| 1:30–2:00 | **Finish order.** #1 turns green under "Ready for pickup", and "Your order" updates without a click (observer pattern). |
| 2:00–2:30 | **Bad input from our scope:** quantity `25` → "Invalid input: Re-enter input". Mark Salad sold out, then order it → "Not available". |
| 2:30–2:50 | In Eclipse, run the `test` folder as JUnit: 137 green. Open `DiningHallModelTest.cancelRemovesOrderAndMovesLineUp`. |
| 2:50–3:00 | Close the window (history saved) and summarize: the model didn't change for the GUI, and cancel stayed inside the model. |

### Who does what
| Remaining item | Owner |
|---|---|
| Pickup: `PICKED_UP`, `pickUpOrder`, model tests | Ryan |
| Pickup and cancel wiring in controller and views, separate kitchen window, GUI tests | Shelby |
| `OrderStatus.canMoveTo` transition table + `OrderTest` updates | Isabella |
| Save after each order, any-hall lookup and cancel, most-ordered items, README for the final | Jonathan |
| Rehearse the 3-minute demo | Everyone |

---

## Attribution

One row per class and test file. "GenAI-assisted" means Claude (Anthropic) in
Claude Code wrote or changed the code from our spec or plan, and the named
person reviewed or ran it.

| File | Who | How |
|---|---|---|
| `src/LinkedQueue.java` | Jonathan | Wrote the first version (linked nodes, enqueue/dequeue/isEmpty/size). Finished with GenAI (peek, null checks, iterator). D4: `remove`, GenAI-assisted. Reviewed. |
| `src/DiningHallModel.java` | Jonathan | Wrote the initial field stub. GenAI-assisted implementation from our spec. D4: `cancelOrder`, GenAI-assisted. Reviewed. |
| `src/Order.java` | Isabella, Jonathan | Isabella: GenAI-assisted from the spec, reviewed. Jonathan: D4 cancel rule in `setStatus`, GenAI-assisted. |
| `src/OrderStatus.java` | Isabella, Jonathan | Isabella: GenAI-assisted from the spec. Jonathan: D4 `CANCELLED`, GenAI-assisted. |
| `src/OrderListener.java` | Isabella | GenAI-assisted from the spec. |
| `src/Menu.java` | Jonathan | GenAI-assisted from the spec. Reviewed. |
| `src/OrderHistory.java` | Jonathan | GenAI-assisted from the spec. Reviewed. |
| `src/OrderController.java` | Ryan, Jonathan | Ryan: generated with GenAI from the spec, reviewed. Jonathan: D4 `cancelOrder` and cancelled lookup text, GenAI-assisted. |
| `src/OrderFormView.java` | Shelby, Jonathan | Shelby: generated with GenAI, reviewed. Jonathan: D4 `cancelOrder()`, GenAI-assisted. |
| `src/KioskView.java` | Shelby | Generated with GenAI. Shelby reviewed it. |
| `src/MobileView.java` | Ryan | Generated with GenAI. Ryan reviewed it. |
| `src/StatusBoardView.java` | Shelby, Jonathan | Shelby: generated with GenAI, reviewed. Jonathan: D4 removed cached rows, GenAI-assisted. |
| `src/GrubHubApp.java` | Ryan, Jonathan | Ryan: generated with GenAI, reviewed, ran the MVP path by hand. Jonathan: `createHalls` and cancel option, GenAI-assisted. |
| `src/ModelDriver.java` | Isabella | GenAI-assisted. Ran it. |
| `src/GrubHubGUI.java` | Shelby | Generated with GenAI (Swing GUI). Ran it. |
| `src/OrderFormPanel.java` | Jonathan | Generated with GenAI. D4 cancel button. Ran it. |
| `src/KioskPanel.java` | Ryan | Generated with GenAI. |
| `src/MobilePanel.java` | Jonathan | Generated with GenAI. |
| `src/StatusBoardPanel.java` | Isabella | Generated with GenAI. Ran it. |
| `src/KitchenPanel.java` | Shelby | Generated with GenAI. Ran it. |
| `src/MenuComboModel.java` | Jonathan | Generated with GenAI. |
| `test/LinkedQueueTest.java` | Jonathan | GenAI-assisted from the spec test plan. D4: remove and interleaving tests. Ran it. |
| `test/OrderTest.java` | Isabella, Jonathan | Jonathan: GenAI-assisted first version. Isabella: expanded, restructured and added the stage-skipping exception tests. Jonathan: D4 cancel tests, GenAI-assisted. |
| `test/MenuTest.java` | Jonathan | GenAI-assisted from the spec test plan. D4: `find` and constructor tests. Ran it. |
| `test/OrderHistoryTest.java` | Isabella, Jonathan | Isabella: GenAI-assisted from the spec test plan, ran it. Jonathan: D4 cancelled-status and size tests, GenAI-assisted. |
| `test/DiningHallModelTest.java` | Isabella, Jonathan | Isabella: GenAI-assisted from the spec test plan, ran it. Jonathan: D4 cancel tests and remaining-method tests, GenAI-assisted. |
| `test/OrderControllerTest.java` | Jonathan | GenAI-assisted from the spec test plan. D4 cancel tests. Ran it. |
| `test/ViewTest.java` | Jonathan | GenAI-assisted. D4 cancel tests. Ran it. |
| `test/GuiTest.java` | Jonathan | Generated with GenAI. Ran it. |

**GenAI note:** We used Claude (Anthropic) in Claude Code.
- For D3 we gave it our Deliverable 2 specification and our partial
  `LinkedQueue`, and it wrote most of the class and test code from that spec.
- For D4 we asked it to build a Swing GUI on the same controller and model,
  and to implement the cancel feature exactly as our D3 Next Steps predicted.
  It also wrote the tests and drafted this README.
- We reviewed the generated code against our spec and plan, ran the tests and
  both programs, and made our own edits (for example `OrderTest` and the
  attribution table).
