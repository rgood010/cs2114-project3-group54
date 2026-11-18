import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEW. The pickup status board: the current line (waiting and being made)
 * and the orders ready for pickup at the selected hall. Redraws itself
 * whenever the model changes. It keeps no copy of model state: every row is
 * built from the model at the moment it is printed or asked for.
 */
public class StatusBoardView implements OrderListener {

    private final OrderController controller;
    private final PrintStream out;
    private boolean autoRefresh = true;

    /**
     * @param controller the session's controller
     * @param out        where to print output
     */
    public StatusBoardView(OrderController controller, PrintStream out) {
        this.controller = controller;
        this.out = out;
        controller.addListener(this);
    }

    /**
     * Turns redrawing on every model change on or off.
     *
     * @param on true to redraw on every change
     */
    public void setAutoRefresh(boolean on) {
        autoRefresh = on;
    }

    /** Prints the current line, or an empty-state message. */
    public void displayLine() {
        List<String> lineRows = getLineRows();
        out.println("----- NOW IN LINE -----");
        if (lineRows.isEmpty()) {
            out.println("  No orders in line.");
        }
        for (String row : lineRows) {
            out.println("  " + row);
        }
    }

    /** Prints the ready orders, or an empty-state message. */
    public void displayReady() {
        List<String> readyRows = getReadyRows();
        out.println("----- READY FOR PICKUP -----");
        if (readyRows.isEmpty()) {
            out.println("  No orders ready for pickup.");
        }
        for (String row : readyRows) {
            out.println("  " + row);
        }
    }

    /** @return the line rows, built from the model now */
    public List<String> getLineRows() {
        List<String> rows = new ArrayList<>();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            return rows;
        }
        for (Order o : hall.getOrdersBeingMade()) {
            rows.add(o + "  BEING MADE  ~"
                + hall.getWaitTime(o.getOrderNum()) + " min");
        }
        for (Order o : hall.getWaitingLine()) {
            rows.add(o + "  WAITING #" + hall.getPosition(o.getOrderNum())
                + "  ~" + hall.getWaitTime(o.getOrderNum()) + " min");
        }
        return rows;
    }

    /** @return the ready rows, built from the model now */
    public List<String> getReadyRows() {
        List<String> rows = new ArrayList<>();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            return rows;
        }
        for (Order o : hall.getReadyOrders()) {
            rows.add(o.toString());
        }
        return rows;
    }

    @Override
    public void modelChanged() {
        if (!autoRefresh || controller.getSelectedHall() == null) {
            return;
        }
        out.println("===== " + controller.getSelectedHall().getHallName()
            + " STATUS BOARD =====");
        displayLine();
        displayReady();
    }
}
