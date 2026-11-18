import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * VIEW (Swing). The pickup status board for the selected hall: the current
 * line (being made, then waiting in predicted order) and the orders ready
 * for pickup. Rows are color-coded by status (a stretch goal from our scope
 * doc). Redraws itself whenever the model changes.
 *
 * <p>It keeps no copy of model state: each list's rows are rebuilt from the
 * model every time Swing asks for them (see {@link BoardListModel}).
 */
public class StatusBoardPanel extends JPanel implements OrderListener {

    /** Text color for each status, and for the empty-list message. */
    static final Color WAITING_COLOR = new Color(190, 110, 0);
    static final Color BEING_MADE_COLOR = new Color(30, 90, 190);
    static final Color READY_COLOR = new Color(20, 130, 50);
    static final Color EMPTY_COLOR = Color.GRAY;

    private final OrderController controller;
    private final BoardListModel lineModel = new BoardListModel(false);
    private final BoardListModel readyModel = new BoardListModel(true);
    private final JLabel titleLabel = new JLabel(" ");

    /**
     * @param controller the session's controller
     */
    public StatusBoardPanel(OrderController controller) {
        super(new BorderLayout(10, 10));
        this.controller = controller;

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel lists = new JPanel(new GridLayout(2, 1, 10, 10));
        lists.add(listPane("Now in line", lineModel));
        lists.add(listPane("Ready for pickup", readyModel));

        JLabel legend = new JLabel("<html><font color='#BE6E00'>WAITING</font>"
            + " &nbsp; <font color='#1E5ABE'>BEING MADE</font>"
            + " &nbsp; <font color='#148232'>READY</font></html>");

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(titleLabel, BorderLayout.NORTH);
        add(lists, BorderLayout.CENTER);
        add(legend, BorderLayout.SOUTH);

        controller.addListener(this);
        refresh();
    }

    /** Tells both lists the model changed, so they read it again. */
    public void refresh() {
        DiningHallModel hall = controller.getSelectedHall();
        titleLabel.setText(hall == null
            ? "Select a dining hall to see its board."
            : hall.getHallName() + " status board");
        lineModel.changed();
        readyModel.changed();
    }

    /**
     * Builds the board's rows from the model right now.
     *
     * @param ready true for the ready list, false for the line
     * @return the rows, or one gray message row if the list is empty
     */
    private List<Row> rows(boolean ready) {
        List<Row> rows = new ArrayList<>();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall != null && ready) {
            for (Order o : hall.getReadyOrders()) {
                rows.add(new Row(o + "  READY", OrderStatus.READY));
            }
        }
        else if (hall != null) {
            for (Order o : hall.getOrdersBeingMade()) {
                rows.add(new Row(o + "  BEING MADE  ~"
                    + hall.getWaitTime(o.getOrderNum()) + " min",
                    OrderStatus.BEING_MADE));
            }
            for (Order o : hall.getWaitingLine()) {
                rows.add(new Row(o + "  WAITING #"
                    + hall.getPosition(o.getOrderNum()) + "  ~"
                    + hall.getWaitTime(o.getOrderNum()) + " min",
                    OrderStatus.WAITING));
            }
        }
        if (rows.isEmpty()) {
            rows.add(new Row(ready ? "No orders ready for pickup."
                : "No orders in line.", null));
        }
        return rows;
    }

    @Override
    public void modelChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            refresh();
        }
        else {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    /** @return the text of each row in the line list */
    List<String> getLineRows() {
        return texts(rows(false));
    }

    /** @return the text of each row in the ready list */
    List<String> getReadyRows() {
        return texts(rows(true));
    }

    /**
     * @param status an order status, or null for the empty-list message
     * @return the color the board uses for it
     */
    static Color colorFor(OrderStatus status) {
        if (status == null) {
            return EMPTY_COLOR;
        }
        switch (status) {
            case WAITING:
                return WAITING_COLOR;
            case BEING_MADE:
                return BEING_MADE_COLOR;
            default:
                return READY_COLOR;
        }
    }

    private static List<String> texts(List<Row> rows) {
        List<String> out = new ArrayList<>();
        for (Row row : rows) {
            out.add(row.text);
        }
        return out;
    }

    private static JPanel listPane(String title, BoardListModel model) {
        JList<Row> list = new JList<>(model);
        list.setCellRenderer(new StatusRenderer());
        JPanel pane = new JPanel(new BorderLayout());
        pane.setBorder(BorderFactory.createTitledBorder(title));
        pane.add(new JScrollPane(list), BorderLayout.CENTER);
        return pane;
    }

    /** A list model that asks the model for its rows on every read. */
    private class BoardListModel extends AbstractListModel<Row> {
        private final boolean ready;

        BoardListModel(boolean ready) {
            this.ready = ready;
        }

        void changed() {
            fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        @Override
        public int getSize() {
            return rows(ready).size();
        }

        @Override
        public Row getElementAt(int index) {
            List<Row> rows = rows(ready);
            return index < rows.size() ? rows.get(index) : null;
        }
    }

    /** One line on the board: its text and the status that sets its color. */
    private static class Row {
        private final String text;
        private final OrderStatus status;

        Row(String text, OrderStatus status) {
            this.text = text;
            this.status = status;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Colors each row by its order's status. */
    private static class StatusRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value, int index, boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected,
                focused);
            if (!selected && value instanceof Row) {
                setForeground(colorFor(((Row) value).status));
            }
            return this;
        }
    }
}
