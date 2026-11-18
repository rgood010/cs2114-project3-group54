import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
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
 */
public class StatusBoardPanel extends JPanel implements OrderListener {

    /** Text color for each status, and for the empty-list message. */
    static final Color WAITING_COLOR = new Color(190, 110, 0);
    static final Color BEING_MADE_COLOR = new Color(30, 90, 190);
    static final Color READY_COLOR = new Color(20, 130, 50);
    static final Color EMPTY_COLOR = Color.GRAY;

    private final OrderController controller;
    private final DefaultListModel<Row> lineModel = new DefaultListModel<>();
    private final DefaultListModel<Row> readyModel = new DefaultListModel<>();
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

    /** Rebuilds both lists from the selected hall. */
    public void refresh() {
        lineModel.clear();
        readyModel.clear();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            titleLabel.setText("Select a dining hall to see its board.");
            return;
        }
        titleLabel.setText(hall.getHallName() + " status board");
        for (Order o : hall.getOrdersBeingMade()) {
            lineModel.addElement(new Row(o + "  BEING MADE  ~"
                + hall.getWaitTime(o.getOrderNum()) + " min",
                OrderStatus.BEING_MADE));
        }
        for (Order o : hall.getWaitingLine()) {
            lineModel.addElement(new Row(o + "  WAITING #"
                + hall.getPosition(o.getOrderNum()) + "  ~"
                + hall.getWaitTime(o.getOrderNum()) + " min",
                OrderStatus.WAITING));
        }
        for (Order o : hall.getReadyOrders()) {
            readyModel.addElement(new Row(o + "  READY", OrderStatus.READY));
        }
        if (lineModel.isEmpty()) {
            lineModel.addElement(new Row("No orders in line.", null));
        }
        if (readyModel.isEmpty()) {
            readyModel.addElement(new Row("No orders ready for pickup.",
                null));
        }
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
        return texts(lineModel);
    }

    /** @return the text of each row in the ready list */
    List<String> getReadyRows() {
        return texts(readyModel);
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

    private static List<String> texts(DefaultListModel<Row> model) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            out.add(model.get(i).text);
        }
        return out;
    }

    private static JPanel listPane(String title, DefaultListModel<Row> model) {
        JList<Row> list = new JList<>(model);
        list.setCellRenderer(new StatusRenderer());
        JPanel pane = new JPanel(new BorderLayout());
        pane.setBorder(BorderFactory.createTitledBorder(title));
        pane.add(new JScrollPane(list), BorderLayout.CENTER);
        return pane;
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
