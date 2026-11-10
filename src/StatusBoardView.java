import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEW. The pickup status board: the current line (waiting and being made)
 * and the orders ready for pickup at the selected hall. Redraws itself
 * whenever the model changes.
 */
public class StatusBoardView implements OrderListener {

    private final OrderController controller;
    private final PrintStream out;
    private final List<String> lineRows = new ArrayList<>();
    private final List<String> readyRows = new ArrayList<>();
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
        refresh();
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
        refresh();
        out.println("----- READY FOR PICKUP -----");
        if (readyRows.isEmpty()) {
            out.println("  No orders ready for pickup.");
        }
        for (String row : readyRows) {
            out.println("  " + row);
        }
    }

    /** @return a copy of the line rows last shown */
    public List<String> getLineRows() {
        return new ArrayList<>(lineRows);
    }

    /** @return a copy of the ready rows last shown */
    public List<String> getReadyRows() {
        return new ArrayList<>(readyRows);
    }

    @Override
    public void modelChanged() {
        if (!autoRefresh || controller.getSelectedHall() == null) {
            refresh();
            return;
        }
        out.println("===== " + controller.getSelectedHall().getHallName()
            + " STATUS BOARD =====");
        displayLine();
        displayReady();
    }

    private void refresh() {
        lineRows.clear();
        readyRows.clear();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            return;
        }
        for (Order o : hall.getOrdersBeingMade()) {
            lineRows.add(o + "  BEING MADE  ~"
                + hall.getWaitTime(o.getOrderNum()) + " min");
        }
        for (Order o : hall.getWaitingLine()) {
            lineRows.add(o + "  WAITING #" + hall.getPosition(o.getOrderNum())
                + "  ~" + hall.getWaitTime(o.getOrderNum()) + " min");
        }
        for (Order o : hall.getReadyOrders()) {
            readyRows.add(o.toString());
        }
    }
}
