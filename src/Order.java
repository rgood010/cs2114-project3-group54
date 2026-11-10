/**
 * MODEL. A single food order. Everything except the status is fixed; the
 * status only moves forward WAITING -> BEING_MADE -> READY.
 */
public class Order {

    private final int orderNum;
    private final String studentId;
    private final String item;
    private final int qty;
    private final boolean mobile;
    private OrderStatus status;

    /**
     * Creates a new WAITING order.
     *
     * @param orderNum  the assigned order number
     * @param studentId the student who placed it
     * @param item      the menu item name
     * @param qty       the quantity ordered
     * @param mobile    true for a mobile order, false for a walk-in (kiosk)
     */
    public Order(int orderNum, String studentId, String item, int qty,
        boolean mobile) {
        this(orderNum, studentId, item, qty, mobile, OrderStatus.WAITING);
    }

    /**
     * Creates an order with an explicit status. Used when restoring saved
     * history records.
     */
    Order(int orderNum, String studentId, String item, int qty,
        boolean mobile, OrderStatus status) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        if (item == null || item.trim().isEmpty()) {
            throw new IllegalArgumentException("Item is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.orderNum = orderNum;
        this.studentId = studentId;
        this.item = item;
        this.qty = qty;
        this.mobile = mobile;
        this.status = status;
    }

    /**
     * Copy constructor. Produces a detached copy that shares no state with
     * the original.
     *
     * @param other the order to copy
     */
    public Order(Order other) {
        this(other.orderNum, other.studentId, other.item, other.qty,
            other.mobile, other.status);
    }

    /** @return the assigned order number */
    public int getOrderNum() {
        return orderNum;
    }

    /** @return the ID of the student who placed the order */
    public String getStudentId() {
        return studentId;
    }

    /** @return the ordered item's name */
    public String getItem() {
        return item;
    }

    /** @return the quantity ordered */
    public int getQty() {
        return qty;
    }

    /** @return true if this was placed as a mobile order */
    public boolean isMobile() {
        return mobile;
    }

    /** @return the current stage */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Moves the order to the next stage.
     *
     * @param s the new status
     * @throws IllegalArgumentException if s is null
     * @throws IllegalStateException    if s skips a stage or moves backward
     */
    public void setStatus(OrderStatus s) {
        if (s == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (s == status) {
            return;
        }
        if (s.ordinal() != status.ordinal() + 1) {
            throw new IllegalStateException(
                "Cannot move order from " + status + " to " + s);
        }
        status = s;
    }

    @Override
    public String toString() {
        return "#" + orderNum + " " + item + " x" + qty
            + (mobile ? " (mobile)" : " (walk-in)");
    }
}
