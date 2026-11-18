import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * VIEW (Swing). Kitchen controls for the selected hall: start the next
 * order, finish an order, and mark an item sold out. Every button goes
 * through the controller, which reports what happened.
 */
public class KitchenPanel extends JPanel implements OrderListener {

    private final OrderController controller;
    private final JButton startButton = new JButton("Start next order");
    private final JButton finishButton = new JButton("Finish order");
    private final JComboBox<String> soldOutBox = new JComboBox<>();
    private final JButton soldOutButton = new JButton("Mark sold out");
    private final JLabel summaryLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");

    /**
     * @param controller the session's controller
     */
    public KitchenPanel(OrderController controller) {
        super(new BorderLayout(10, 10));
        this.controller = controller;

        startButton.addActionListener(e -> startNext());
        finishButton.addActionListener(e -> finish());
        soldOutButton.addActionListener(e -> markSoldOut());

        JPanel orders = new JPanel(new FlowLayout(FlowLayout.LEFT));
        orders.add(startButton);
        orders.add(finishButton);

        JPanel stock = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stock.add(new JLabel("Item:"));
        stock.add(soldOutBox);
        stock.add(soldOutButton);

        JPanel controls = new JPanel(new GridLayout(0, 1, 4, 4));
        controls.add(orders);
        controls.add(stock);

        JPanel messages = new JPanel(new GridLayout(2, 1, 4, 4));
        messages.add(summaryLabel);
        messages.add(messageLabel);

        setBorder(BorderFactory.createTitledBorder("Kitchen"));
        add(controls, BorderLayout.NORTH);
        add(messages, BorderLayout.CENTER);

        controller.addListener(this);
        refresh();
    }

    /** Reloads the item list and the line counts for the selected hall. */
    public void refresh() {
        Object selected = soldOutBox.getSelectedItem();
        soldOutBox.removeAllItems();
        DiningHallModel hall = controller.getSelectedHall();
        if (hall == null) {
            summaryLabel.setText("Select a dining hall first.");
            return;
        }
        for (String item : hall.getMenuItems()) {
            if (hall.isInStock(item)) {
                soldOutBox.addItem(item);
            }
        }
        if (selected != null) {
            soldOutBox.setSelectedItem(selected);
        }
        summaryLabel.setText(hall.getHallName() + " | waiting: "
            + hall.getWaitingLine().size() + " | being made: "
            + hall.getOrdersBeingMade().size() + " of "
            + hall.getKitchenCapacity() + " | ready: "
            + hall.getReadyOrders().size());
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

    /** Starts the next waiting order, like clicking the button. */
    void startNext() {
        controller.startNextOrder();
        report();
    }

    /** Finishes the oldest order being made, like clicking the button. */
    void finish() {
        controller.finishOrder();
        report();
    }

    /** Marks the chosen item sold out, like clicking the button. */
    void markSoldOut() {
        String item = (String) soldOutBox.getSelectedItem();
        if (item == null) {
            messageLabel.setText("Nothing left to mark sold out.");
            messageLabel.setForeground(new Color(170, 30, 30));
            return;
        }
        controller.markSoldOut(item);
        report();
    }

    /** @return the item list used for marking sold out (for tests) */
    JComboBox<String> getSoldOutBox() {
        return soldOutBox;
    }

    /** @return the last message shown (for tests) */
    String getMessage() {
        return messageLabel.getText();
    }

    /** @return the line counts shown (for tests) */
    String getSummary() {
        return summaryLabel.getText();
    }

    private void report() {
        messageLabel.setText(controller.getLastMessage());
        messageLabel.setForeground(controller.lastActionSucceeded()
            ? new Color(20, 110, 40) : new Color(170, 30, 30));
    }
}
