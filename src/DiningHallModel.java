import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * MODEL. One dining hall's kitchen: its waiting lines, the orders being
 * made, and the orders ready for pickup. The model never reads input or
 * prints; it tells registered OrderListeners when its state changes.
 *
 * <p>Fairness rule: the kitchen starts up to three mobile orders in a row,
 * then one walk-in order if any walk-in is waiting. If one line is empty,
 * the other line is served.
 *
 * <p>Wait-time rule: the kitchen finishes orders one at a time, in the
 * order they were started, each taking {@code minutesPerOrder} minutes.
 * An order being made at index i (0-based) of the kitchen waits
 * (i + 1) * minutesPerOrder. A waiting order at predicted position p waits
 * (ordersBeingMade + p) * minutesPerOrder. A ready order waits 0.
 */
public class DiningHallModel {

    /** Most mobile orders the kitchen starts in a row while walk-ins wait. */
    public static final int MOBILE_STREAK = 3;
    /** Largest quantity allowed in one order. */
    public static final int MAX_QTY = 20;

    private static int nextOrderNum = 1;

    private final String hallName;
    private final Menu menu;
    private final OrderHistory history;
    private final int kitchenCapacity;
    private final int minutesPerOrder;

    private final LinkedQueue<Order> mobileQueue = new LinkedQueue<>();
    private final LinkedQueue<Order> walkInQueue = new LinkedQueue<>();
    private final LinkedQueue<Order> makingQueue = new LinkedQueue<>();
    private final LinkedQueue<Order> readyQueue = new LinkedQueue<>();
    private final List<OrderListener> listeners = new ArrayList<>();
    /** Every order placed at this hall, by number, for status lookups. */
    private final Map<Integer, Order> orders = new HashMap<>();

    private int consecutiveMobileStarts = 0;

    /**
     * @param hallName        the hall's display name
     * @param menu            this hall's menu
     * @param history         the history shared by all halls
     * @param kitchenCapacity how many orders can be made at once (positive)
     * @param minutesPerOrder minutes to make one order (positive)
     */
    public DiningHallModel(String hallName, Menu menu, OrderHistory history,
        int kitchenCapacity, int minutesPerOrder) {
        if (hallName == null || hallName.trim().isEmpty()) {
            throw new IllegalArgumentException("Hall name is required");
        }
        if (menu == null || history == null) {
            throw new IllegalArgumentException("Menu and history required");
        }
        if (kitchenCapacity < 1 || minutesPerOrder < 1) {
            throw new IllegalArgumentException(
                "Capacity and minutes per order must be positive");
        }
        this.hallName = hallName.trim();
        this.menu = menu;
        this.history = history;
        this.kitchenCapacity = kitchenCapacity;
        this.minutesPerOrder = minutesPerOrder;
    }

    // ------------------------------------------------------------ commands

    /**
     * Places a new WAITING order in the chosen line.
     *
     * @param id     the student ID
     * @param item   the menu item
     * @param qty    the quantity, 1 to 20
     * @param mobile true for the mobile line, false for walk-in
     * @return the new order's number
     * @throws IllegalArgumentException for a blank ID, unknown item, or bad
     *                                  quantity
     * @throws IllegalStateException    if the item is sold out
     */
    public int placeOrder(String id, String item, int qty, boolean mobile) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        String name = menu.find(item);
        if (name == null) {
            throw new IllegalArgumentException(
                "\"" + item + "\" is not on the " + hallName + " menu");
        }
        if (qty < 1 || qty > MAX_QTY) {
            throw new IllegalArgumentException(
                "Quantity must be between 1 and " + MAX_QTY);
        }
        if (!menu.inStock(name)) {
            throw new IllegalStateException(name + " is sold out");
        }

        // Never reuse a number that was restored from saved history.
        int num = Math.max(nextOrderNum, history.getLargestOrderNum() + 1);
        nextOrderNum = num + 1;

        Order order = new Order(num, id.trim(), name, qty, mobile);
        if (mobile) {
            mobileQueue.enqueue(order);
        }
        else {
            walkInQueue.enqueue(order);
        }
        orders.put(num, order);
        history.add(order);
        notifyListeners();
        return num;
    }

    /**
     * Starts the next waiting order chosen by the fairness rule.
     *
     * @throws IllegalStateException  if the kitchen is full
     * @throws NoSuchElementException if no orders are waiting
     */
    public void startNextOrder() {
        if (makingQueue.size() >= kitchenCapacity) {
            throw new IllegalStateException("Kitchen is full");
        }
        if (mobileQueue.isEmpty() && walkInQueue.isEmpty()) {
            throw new NoSuchElementException("No orders are waiting");
        }
        boolean fromMobile = takeMobileNext(consecutiveMobileStarts,
            mobileQueue.isEmpty(), walkInQueue.isEmpty());
        Order next = fromMobile ? mobileQueue.dequeue() : walkInQueue.dequeue();
        consecutiveMobileStarts =
            nextStreak(consecutiveMobileStarts, fromMobile);
        next.setStatus(OrderStatus.BEING_MADE);
        makingQueue.enqueue(next);
        notifyListeners();
    }

    /**
     * Finishes the oldest order in the kitchen and moves it to pickup.
     *
     * @throws NoSuchElementException if nothing is being made
     */
    public void finishOrder() {
        if (makingQueue.isEmpty()) {
            throw new NoSuchElementException("No orders are being made");
        }
        Order done = makingQueue.dequeue();
        done.setStatus(OrderStatus.READY);
        readyQueue.enqueue(done);
        notifyListeners();
    }

    /**
     * Marks an item on this hall's menu as sold out.
     *
     * @param item the item name
     * @throws IllegalArgumentException if the item is unknown
     */
    public void markSoldOut(String item) {
        menu.markSoldOut(item);
        notifyListeners();
    }

    /**
     * Registers a listener once. Repeated registration has no effect.
     *
     * @param l the listener
     * @throws IllegalArgumentException if l is null
     */
    public void addListener(OrderListener l) {
        if (l == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /**
     * Unregisters a listener. Has no effect if it was not registered.
     *
     * @param l the listener
     */
    public void removeListener(OrderListener l) {
        listeners.remove(l);
    }

    // ------------------------------------------------------------- queries

    /**
     * @param orderNum an order number at this hall
     * @return the predicted 1-based position in line, or 0 if the order is
     *         being made or ready
     * @throws IllegalArgumentException if the order is unknown here
     */
    public int getPosition(int orderNum) {
        Order target = find(orderNum);
        if (target.getStatus() != OrderStatus.WAITING) {
            return 0;
        }
        List<Order> line = predictedLine();
        return line.indexOf(target) + 1;
    }

    /**
     * @param orderNum an order number at this hall
     * @return estimated minutes until the order is ready (0 if ready)
     * @throws IllegalArgumentException if the order is unknown here
     */
    public int getWaitTime(int orderNum) {
        Order target = find(orderNum);
        switch (target.getStatus()) {
            case READY:
                return 0;
            case BEING_MADE:
                int index = 0;
                for (Order o : makingQueue) {
                    if (o == target) {
                        break;
                    }
                    index++;
                }
                return (index + 1) * minutesPerOrder;
            default:
                return (makingQueue.size() + getPosition(orderNum))
                    * minutesPerOrder;
        }
    }

    /**
     * @param orderNum an order number at this hall
     * @return the order's current status
     * @throws IllegalArgumentException if the order is unknown here
     */
    public OrderStatus getStatus(int orderNum) {
        return find(orderNum).getStatus();
    }

    /** @return the hall's name */
    public String getHallName() {
        return hallName;
    }

    /** @return a copy of this hall's menu items, including sold-out ones */
    public List<String> getMenuItems() {
        return menu.getItems();
    }

    /**
     * @param item an item name
     * @return true if the item is on this menu and in stock
     */
    public boolean isInStock(String item) {
        return menu.inStock(item);
    }

    /**
     * @param item an item name
     * @return true if the item is on this menu, in stock or not
     */
    public boolean isOnMenu(String item) {
        return menu.find(item) != null;
    }

    /** @return how many orders the kitchen can make at once */
    public int getKitchenCapacity() {
        return kitchenCapacity;
    }

    /**
     * @return copies of the waiting orders, in the predicted order the
     *         kitchen will start them
     */
    public List<Order> getWaitingLine() {
        return copies(predictedLine());
    }

    /** @return copies of the orders being made, oldest first */
    public List<Order> getOrdersBeingMade() {
        return copies(makingQueue);
    }

    /** @return copies of the ready orders, in the order they finished */
    public List<Order> getReadyOrders() {
        return copies(readyQueue);
    }

    // ------------------------------------------------------------- helpers

    private Order find(int orderNum) {
        Order o = orders.get(orderNum);
        if (o == null) {
            throw new IllegalArgumentException(
                "Order #" + orderNum + " not found at " + hallName);
        }
        return o;
    }

    /**
     * Simulates the fairness rule over the two waiting lines, without
     * changing them, to get the order in which they would be started.
     */
    private List<Order> predictedLine() {
        List<Order> mobile = new ArrayList<>();
        for (Order o : mobileQueue) {
            mobile.add(o);
        }
        List<Order> walkIn = new ArrayList<>();
        for (Order o : walkInQueue) {
            walkIn.add(o);
        }
        List<Order> line = new ArrayList<>();
        int streak = consecutiveMobileStarts;
        int m = 0;
        int w = 0;
        while (m < mobile.size() || w < walkIn.size()) {
            boolean fromMobile = takeMobileNext(streak, m >= mobile.size(),
                w >= walkIn.size());
            line.add(fromMobile ? mobile.get(m++) : walkIn.get(w++));
            streak = nextStreak(streak, fromMobile);
        }
        return line;
    }

    private static boolean takeMobileNext(int streak, boolean mobileEmpty,
        boolean walkInEmpty) {
        if (mobileEmpty) {
            return false;
        }
        if (walkInEmpty) {
            return true;
        }
        return streak < MOBILE_STREAK;
    }

    private static int nextStreak(int streak, boolean startedMobile) {
        return startedMobile ? Math.min(streak + 1, MOBILE_STREAK) : 0;
    }

    private static List<Order> copies(Iterable<Order> source) {
        List<Order> result = new ArrayList<>();
        for (Order o : source) {
            result.add(new Order(o));
        }
        return result;
    }

    private void notifyListeners() {
        // Copy so a listener can unregister itself during the callback.
        for (OrderListener l : new ArrayList<>(listeners)) {
            l.modelChanged();
        }
    }
}
