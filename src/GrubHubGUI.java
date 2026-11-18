import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Swing entry point. Builds the same model as the terminal app, then shows
 * one window with the order form (kiosk or mobile), the status board and
 * the kitchen controls side by side. All user actions go
 * view -> controller -> model, and every panel redraws itself when the model
 * notifies it.
 */
public class GrubHubGUI extends JFrame {

    private final OrderHistory history = new OrderHistory();
    private final Map<String, DiningHallModel> halls =
        GrubHubApp.createHalls(history);

    private final JComboBox<String> hallBox = new JComboBox<>();
    private final JLabel sessionLabel = new JLabel(" ");
    private final JPanel content = new JPanel(new GridLayout(1, 2, 10, 10));

    private OrderController controller;
    private OrderFormPanel orderPanel;
    private StatusBoardPanel boardPanel;
    private KitchenPanel kitchenPanel;

    /** Builds the window. Call {@link #startSession()} before showing it. */
    public GrubHubGUI() {
        super("Hungry Hokies");
        for (String name : halls.keySet()) {
            hallBox.addItem(name);
        }
        hallBox.addActionListener(e -> selectHall());

        JButton newSession = new JButton("New student session");
        newSession.addActionListener(e -> startSession());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        top.add(sessionLabel);
        top.add(new JLabel("   Dining hall:"));
        top.add(hallBox);
        top.add(newSession);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        content.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        loadHistory();
    }

    /**
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GrubHubGUI gui = new GrubHubGUI();
            if (gui.startSession()) {
                gui.setVisible(true);
            }
            else {
                gui.dispose();
            }
        });
    }

    /**
     * Asks for a student ID and channel, then rebuilds the panels for the
     * new session. Cancelling keeps the current session, if there is one.
     *
     * @return true if a session is running afterwards
     */
    public boolean startSession() {
        JTextField idField = new JTextField(12);
        JRadioButton kiosk = new JRadioButton("Kiosk (walk-in)", true);
        JRadioButton mobile = new JRadioButton("Mobile app");
        ButtonGroup group = new ButtonGroup();
        group.add(kiosk);
        group.add(mobile);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Student ID:"));
        form.add(idField);
        form.add(kiosk);
        form.add(mobile);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form,
                "Start ordering", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return controller != null;
            }
            String id = idField.getText().trim();
            if (!id.isEmpty()) {
                startSession(id, mobile.isSelected());
                return true;
            }
            JOptionPane.showMessageDialog(this,
                "Invalid input: Re-enter input (student ID is required).");
        }
    }

    /**
     * Starts a session without asking (used by the dialog and by tests).
     *
     * @param id       the student ID
     * @param isMobile true for the mobile app, false for the kiosk
     */
    void startSession(String id, boolean isMobile) {
        if (controller != null) {
            controller.removeListener(orderPanel);
            controller.removeListener(boardPanel);
            controller.removeListener(kitchenPanel);
        }
        controller = new OrderController(halls, history, id, isMobile);
        controller.selectHall((String) hallBox.getSelectedItem());
        orderPanel = isMobile ? new MobilePanel(controller)
            : new KioskPanel(controller);
        boardPanel = new StatusBoardPanel(controller);
        kitchenPanel = new KitchenPanel(controller);

        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.add(orderPanel, BorderLayout.CENTER);
        left.add(kitchenPanel, BorderLayout.SOUTH);
        boardPanel.setPreferredSize(new Dimension(500, 460));

        content.removeAll();
        content.add(left);
        content.add(boardPanel);
        sessionLabel.setText("Student " + controller.getStudentId() + " | "
            + (isMobile ? "mobile" : "kiosk"));
        pack();
        setLocationRelativeTo(null);
    }

    /** Points the session at the hall chosen in the dropdown. */
    private void selectHall() {
        if (controller == null) {
            return;
        }
        controller.selectHall((String) hallBox.getSelectedItem());
        orderPanel.refresh();
        boardPanel.refresh();
        kitchenPanel.refresh();
    }

    private void loadHistory() {
        if (!new File(GrubHubApp.HISTORY_FILE).exists()) {
            return;
        }
        try {
            history.load(GrubHubApp.HISTORY_FILE);
        }
        catch (IllegalArgumentException | UncheckedIOException e) {
            JOptionPane.showMessageDialog(this,
                "Could not load order history: " + e.getMessage());
        }
    }

    private void quit() {
        try {
            history.save(GrubHubApp.HISTORY_FILE);
        }
        catch (UncheckedIOException e) {
            JOptionPane.showMessageDialog(this,
                "Could not save order history: " + e.getMessage());
        }
        dispose();
    }

    // ------------------------------------------- package access for tests

    OrderFormPanel getOrderPanel() {
        return orderPanel;
    }

    StatusBoardPanel getBoardPanel() {
        return boardPanel;
    }

    KitchenPanel getKitchenPanel() {
        return kitchenPanel;
    }

    JComboBox<String> getHallBox() {
        return hallBox;
    }
}
