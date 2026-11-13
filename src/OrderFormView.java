import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * VIEW. Shared terminal form behind KioskView and MobileView: shows the
 * selected hall's menu, reads an item and quantity, and hands them to the
 * controller. It never changes the model itself.
 */
public abstract class OrderFormView implements OrderListener {

    private final OrderController controller;
    private final Scanner in;
    private final PrintStream out;
    private String itemText = "";
    private String quantityText = "";
    private int trackedOrderNum = 0;

    /**
     * @param controller the session's controller
     * @param in         where to read input
     * @param out        where to print output
     */
    protected OrderFormView(OrderController controller, Scanner in,
        PrintStream out) {
        this.controller = controller;
        this.in = in;
        this.out = out;
        controller.addListener(this);
    }

    /** @return the label shown in this view's messages */
    protected abstract String label();

    /** @return true if this view submits mobile orders */
    protected abstract boolean isMobile();

    /** Shows the selected hall's menu, labelling sold-out items. */
    public void displayMenu() {
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            out.println("[" + label() + "] Select a dining hall first.");
            return;
        }
        List<String> items = hall.getMenuItems();
        out.println("[" + label() + "] " + hall.getHallName() + " menu:");
        if (items.isEmpty()) {
            out.println("  The menu is empty.");
            return;
        }
        for (String item : items) {
            out.println("  - " + item
                + (hall.isInStock(item) ? "" : "  (SOLD OUT)"));
        }
    }

    /**
     * Reads an item and quantity and submits them. Re-prompts for a
     * quantity that is not a whole number, and re-prompts for the whole
     * order when the controller rejects it. Typing "cancel" stops.
     */
    public void readOrder() {
        if (controller.getSelectedHall() == null) {
            out.println("[" + label() + "] Select a dining hall first.");
            return;
        }
        while (true) {
            itemText = prompt("Item (or 'cancel'): ");
            if (itemText == null || itemText.equalsIgnoreCase("cancel")) {
                out.println("Order cancelled.");
                return;
            }
            Integer qty = null;
            while (qty == null) {
                quantityText = prompt("Quantity (1-20): ");
                if (quantityText == null
                    || quantityText.equalsIgnoreCase("cancel")) {
                    out.println("Order cancelled.");
                    return;
                }
                try {
                    qty = Integer.parseInt(quantityText);
                }
                catch (NumberFormatException e) {
                    out.println("Invalid input: Re-enter input (\""
                        + quantityText + "\" is not a whole number).");
                }
            }
            controller.placeOrder(itemText, qty, isMobile());
            out.println(controller.getLastMessage());
            if (controller.lastActionSucceeded()) {
                trackedOrderNum = controller.getLastOrderNum();
                return;
            }
        }
    }

    /** Re-orders the session student's last order through this view. */
    public void reorderLast() {
        controller.reorderLast(controller.getStudentId());
        out.println(controller.getLastMessage());
        if (controller.lastActionSucceeded()) {
            trackedOrderNum = controller.getLastOrderNum();
        }
    }

    /** Refreshes the status of the order this view placed, if any. */
    @Override
    public void modelChanged() {
        if (trackedOrderNum > 0) {
            out.println("[" + label() + "] Your order: "
                + controller.lookupStatus(trackedOrderNum));
        }
    }

    private String prompt(String message) {
        out.print(message);
        if (!in.hasNextLine()) {
            return null;
        }
        return in.nextLine().trim();
    }
}
