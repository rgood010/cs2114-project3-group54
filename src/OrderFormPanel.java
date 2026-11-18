import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * VIEW (Swing). Shared order form behind KioskPanel and MobilePanel: shows
 * the selected hall's menu, reads an item and quantity, and hands them to
 * the controller. Like the terminal OrderFormView, it never changes the
 * model itself; it only calls the controller and reads the model.
 *
 * <p>It keeps no copy of model state: the item list is read live from the
 * model by {@link MenuComboModel}, and the "Your order" line is rebuilt from
 * the model each time. It only remembers the number (and hall) of the order
 * it placed, so it knows which order to show.
 */
public abstract class OrderFormPanel extends JPanel implements OrderListener {

    private static final Color ERROR = new Color(170, 30, 30);
    private static final Color OK = new Color(20, 110, 40);

    private final OrderController controller;
    private final MenuComboModel menuModel;
    private final JComboBox<String> itemBox;
    private final JTextField quantityField = new JTextField("1", 5);
    private final JTextField orderNumField = new JTextField(5);
    private final JButton orderButton = new JButton("Place order");
    private final JButton reorderButton = new JButton("Reorder my last order");
    private final JButton lookupButton = new JButton("Look up");
    private final JButton cancelButton = new JButton("Cancel order");
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");
    private int trackedOrderNum = 0;
    private DiningHallModel trackedHall;

    /**
     * @param controller the session's controller
     */
    protected OrderFormPanel(OrderController controller) {
        super(new BorderLayout(10, 10));
        this.controller = controller;
        menuModel = new MenuComboModel(controller, false);
        itemBox = new JComboBox<>(menuModel);

        itemBox.setRenderer(new SoldOutRenderer());
        orderButton.addActionListener(e -> placeOrder());
        reorderButton.addActionListener(e -> reorderLast());
        lookupButton.addActionListener(e -> lookUp());
        cancelButton.addActionListener(e -> cancelOrder());
        quantityField.addActionListener(e -> placeOrder());
        orderNumField.addActionListener(e -> lookUp());

        JPanel lookupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lookupRow.add(orderNumField);
        lookupRow.add(lookupButton);
        lookupRow.add(cancelButton);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Item:"));
        form.add(itemBox);
        form.add(new JLabel("Quantity (1-20):"));
        form.add(quantityField);
        form.add(new JLabel(""));
        form.add(orderButton);
        form.add(new JLabel(""));
        form.add(reorderButton);
        form.add(new JLabel("Order #:"));
        form.add(lookupRow);

        JPanel messages = new JPanel(new GridLayout(2, 1, 4, 4));
        messages.add(messageLabel);
        messages.add(statusLabel);

        setBorder(BorderFactory.createTitledBorder(label()));
        add(form, BorderLayout.NORTH);
        add(messages, BorderLayout.CENTER);

        controller.addListener(this);
        refresh();
    }

    /** @return the title shown on this form */
    protected abstract String label();

    /** @return true if this form submits mobile orders */
    protected abstract boolean isMobile();

    /**
     * Reloads the menu for the selected hall and the tracked order's
     * status. Called when the model changes or a new hall is selected.
     */
    public void refresh() {
        menuModel.refresh();
        refreshStatus();
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

    // ------------------------------------------------------------ actions

    /** Submits the item and quantity on the form, like clicking the button. */
    void placeOrder() {
        if (controller.getSelectedHall() == null) {
            show("Select a dining hall first.", false);
            return;
        }
        String item = (String) itemBox.getSelectedItem();
        if (item == null) {
            show("The menu is empty.", false);
            return;
        }
        String text = quantityField.getText().trim();
        int qty;
        try {
            qty = Integer.parseInt(text);
        }
        catch (NumberFormatException e) {
            show("Invalid input: Re-enter input (\"" + text
                + "\" is not a whole number).", false);
            return;
        }
        controller.placeOrder(item, qty, isMobile());
        afterOrder();
    }

    /** Re-orders the session student's last order. */
    void reorderLast() {
        controller.reorderLast(controller.getStudentId());
        afterOrder();
    }

    /** Looks up the order number typed in the order # box. */
    void lookUp() {
        String text = orderNumField.getText().trim();
        try {
            int num = Integer.parseInt(text);
            String result = controller.lookupStatus(num);
            show(result, result.startsWith("Order #"));
        }
        catch (NumberFormatException e) {
            show("Order numbers are whole numbers.", false);
        }
    }

    /** Cancels the order number typed in the order # box. */
    void cancelOrder() {
        String text = orderNumField.getText().trim();
        int num;
        try {
            num = Integer.parseInt(text);
        }
        catch (NumberFormatException e) {
            show("Invalid input: Re-enter input (\"" + text
                + "\" is not a whole number).", false);
            return;
        }
        controller.cancelOrder(num);
        show(controller.getLastMessage(), controller.lastActionSucceeded());
        refreshStatus();
    }

    private void afterOrder() {
        boolean ok = controller.lastActionSucceeded();
        show(controller.getLastMessage(), ok);
        if (ok) {
            trackedOrderNum = controller.getLastOrderNum();
            trackedHall = controller.getSelectedHall();
            quantityField.setText("1");
        }
        refreshStatus();
    }

    private void refreshStatus() {
        if (trackedOrderNum == 0) {
            statusLabel.setText(" ");
            return;
        }
        // Read-only calls on the model, as in the terminal views.
        OrderStatus status = trackedHall.getStatus(trackedOrderNum);
        String text = "Your order #" + trackedOrderNum + " at "
            + trackedHall.getHallName() + ": " + status;
        if (status != OrderStatus.CANCELLED) {
            text += " | position " + trackedHall.getPosition(trackedOrderNum)
                + " | wait ~" + trackedHall.getWaitTime(trackedOrderNum)
                + " min";
        }
        statusLabel.setText(text);
    }

    private void show(String message, boolean ok) {
        messageLabel.setText(message);
        messageLabel.setForeground(ok ? OK : ERROR);
    }

    // ------------------------------------------- package access for tests

    JComboBox<String> getItemBox() {
        return itemBox;
    }

    JTextField getQuantityField() {
        return quantityField;
    }

    JTextField getOrderNumField() {
        return orderNumField;
    }

    String getMessage() {
        return messageLabel.getText();
    }

    String getStatusText() {
        return statusLabel.getText();
    }

    /** Shows "(SOLD OUT)" next to items that are out of stock. */
    private class SoldOutRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value, int index, boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected,
                focused);
            DiningHallModel hall = controller.getSelectedHall();
            if (value != null && hall != null
                && !hall.isInStock(value.toString())) {
                setText(value + "  (SOLD OUT)");
                if (!selected) {
                    setForeground(Color.GRAY);
                }
            }
            return this;
        }
    }
}
