import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * CONTROLLER. Sits between the views and the dining hall models for one
 * ordering session (one student, one channel). Views hand user input to the
 * controller; the controller validates it, calls the selected model, and
 * records a message the view can show.
 *
 * <p>Commands that can fail because of bad user input (placeOrder,
 * reorderLast, kitchen actions) do not throw for that input; instead they
 * set {@link #lastActionSucceeded()} and {@link #getLastMessage()} so the
 * calling view can tell the student what to fix.
 */
public class OrderController {

    private final Map<String, DiningHallModel> halls;
    private final OrderHistory history;
    private final String activeStudentId;
    private final boolean mobileSession;
    private DiningHallModel selectedHall;

    private String lastMessage = "";
    private boolean lastSucceeded = false;
    private int lastOrderNum = 0;

    /**
     * @param halls         hall directory, keyed by hall name
     * @param history       the history shared by all halls
     * @param studentId     the student using this session
     * @param mobileSession true if this session orders through the mobile
     *                      app, false for the kiosk
     */
    public OrderController(Map<String, DiningHallModel> halls,
        OrderHistory history, String studentId, boolean mobileSession) {
        if (halls == null || halls.isEmpty()) {
            throw new IllegalArgumentException("At least one hall required");
        }
        if (history == null) {
            throw new IllegalArgumentException("History is required");
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        this.halls = Collections.unmodifiableMap(new LinkedHashMap<>(halls));
        this.history = history;
        this.activeStudentId = studentId.trim();
        this.mobileSession = mobileSession;
    }

    // --------------------------------------------------------- session info

    /** @return the hall names, in directory order */
    public List<String> getHallNames() {
        return new ArrayList<>(halls.keySet());
    }

    /**
     * @return the selected hall's model (views use only its read methods),
     *         or null if no hall is selected yet
     */
    public DiningHallModel getSelectedHall() {
        return selectedHall;
    }

    /** @return this session's student ID */
    public String getStudentId() {
        return activeStudentId;
    }

    /** @return true if this is a mobile session */
    public boolean isMobileSession() {
        return mobileSession;
    }

    /** @return true if the last command succeeded */
    public boolean lastActionSucceeded() {
        return lastSucceeded;
    }

    /** @return a user-facing message describing the last command's result */
    public String getLastMessage() {
        return lastMessage;
    }

    /** @return the number of the last order this controller placed, or 0 */
    public int getLastOrderNum() {
        return lastOrderNum;
    }

    /**
     * Registers a view with every hall so it hears about changes no matter
     * which hall is selected later.
     *
     * @param l the listener
     */
    public void addListener(OrderListener l) {
        for (DiningHallModel hall : halls.values()) {
            hall.addListener(l);
        }
    }

    /**
     * Unregisters a view from every hall.
     *
     * @param l the listener
     */
    public void removeListener(OrderListener l) {
        for (DiningHallModel hall : halls.values()) {
            hall.removeListener(l);
        }
    }

    // ------------------------------------------------------------ commands

    /**
     * Selects the named hall (case-insensitive).
     *
     * @param name the hall name
     * @throws IllegalArgumentException if the name is null, blank or
     *                                  unknown; the previous selection stays
     */
    public void selectHall(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Hall name is required");
        }
        for (Map.Entry<String, DiningHallModel> e : halls.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name.trim())) {
                selectedHall = e.getValue();
                return;
            }
        }
        throw new IllegalArgumentException("Unknown dining hall: " + name);
    }

    /**
     * Places an order at the selected hall for this session's student.
     * Bad or sold-out requests are rejected and reported through
     * {@link #getLastMessage()}.
     *
     * @param item   the menu item
     * @param qty    the quantity
     * @param mobile true for the mobile line
     * @throws IllegalStateException if no hall is selected
     */
    public void placeOrder(String item, int qty, boolean mobile) {
        if (selectedHall == null) {
            throw new IllegalStateException("Select a dining hall first.");
        }
        submit(activeStudentId, item, qty, mobile);
    }

    /**
     * @param orderNum an order number
     * @return a display string with the order's status, position and wait
     */
    public String lookupStatus(int orderNum) {
        if (selectedHall == null) {
            return "Select a dining hall first.";
        }
        try {
            OrderStatus status = selectedHall.getStatus(orderNum);
            int position = selectedHall.getPosition(orderNum);
            int wait = selectedHall.getWaitTime(orderNum);
            return "Order #" + orderNum + " at " + selectedHall.getHallName()
                + ": " + status + " | position " + position + " | wait ~"
                + wait + " min";
        }
        catch (IllegalArgumentException e) {
            return "Order not found.";
        }
    }

    /**
     * Places a new order with the same item and quantity as the student's
     * last order, at the selected hall, through this session's channel.
     * Problems are reported through {@link #getLastMessage()}.
     *
     * @param studentId the student whose last order to repeat
     */
    public void reorderLast(String studentId) {
        if (selectedHall == null) {
            fail("Select a dining hall first.");
            return;
        }
        if (studentId == null || studentId.trim().isEmpty()
            || !studentId.trim().equals(activeStudentId)) {
            fail("Invalid student ID for this session.");
            return;
        }
        Order last;
        try {
            last = history.getLast(studentId.trim());
        }
        catch (NoSuchElementException e) {
            fail("No previous orders found for student " + studentId.trim()
                + ".");
            return;
        }
        // 'last' is a detached copy; a brand new order is placed from it.
        submit(activeStudentId, last.getItem(), last.getQty(), mobileSession);
    }

    /** Kitchen: starts the next waiting order at the selected hall. */
    public void startNextOrder() {
        if (selectedHall == null) {
            fail("Select a dining hall first.");
            return;
        }
        try {
            selectedHall.startNextOrder();
            succeed("Started the next order.");
        }
        catch (IllegalStateException e) {
            fail("The kitchen is full. Finish an order first.");
        }
        catch (NoSuchElementException e) {
            fail("No orders are waiting.");
        }
    }

    /** Kitchen: finishes the oldest order being made at the selected hall. */
    public void finishOrder() {
        if (selectedHall == null) {
            fail("Select a dining hall first.");
            return;
        }
        try {
            selectedHall.finishOrder();
            succeed("Finished an order. It is ready for pickup.");
        }
        catch (NoSuchElementException e) {
            fail("No orders are being made.");
        }
    }

    /**
     * Kitchen: marks an item sold out at the selected hall.
     *
     * @param item the item name
     */
    public void markSoldOut(String item) {
        if (selectedHall == null) {
            fail("Select a dining hall first.");
            return;
        }
        try {
            selectedHall.markSoldOut(item);
            succeed(item.trim() + " is now marked sold out.");
        }
        catch (IllegalArgumentException | NullPointerException e) {
            fail("\"" + item + "\" is not on the menu.");
        }
    }

    // ------------------------------------------------------------- helpers

    private void submit(String id, String item, int qty, boolean mobile) {
        // Reject bad input before it reaches the model.
        if (!selectedHall.isOnMenu(item)) {
            fail("\"" + item + "\" is not on the "
                + selectedHall.getHallName() + " menu. Please pick another "
                + "item.");
            return;
        }
        // Messages for the two bad-input cases use our scope doc's wording.
        if (!selectedHall.isInStock(item)) {
            fail("Not available: " + item.trim() + " is sold out. Please "
                + "pick another item.");
            return;
        }
        if (qty < 1 || qty > DiningHallModel.MAX_QTY) {
            fail("Invalid input: Re-enter input (quantity must be between 1 "
                + "and " + DiningHallModel.MAX_QTY + ").");
            return;
        }
        try {
            int num = selectedHall.placeOrder(id, item, qty, mobile);
            lastOrderNum = num;
            succeed("Order #" + num + " placed. " + lookupStatus(num));
        }
        catch (IllegalStateException e) {
            fail("Not available: " + item.trim() + " is sold out. Please "
                + "pick another item.");
        }
        catch (IllegalArgumentException e) {
            fail(e.getMessage() + ". Please try again.");
        }
    }

    private void succeed(String message) {
        lastSucceeded = true;
        lastMessage = message;
    }

    private void fail(String message) {
        lastSucceeded = false;
        lastMessage = message;
    }
}
