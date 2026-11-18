import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the Swing views. They drive each panel the way a click would
 * (calling the method the button calls) and check what the panel shows.
 * The panel tests also run headless; the one window test is skipped there.
 */
public class GuiTest {

    private OrderHistory history;
    private DiningHallModel north;
    private DiningHallModel south;
    private Map<String, DiningHallModel> halls;

    @Before
    public void setUp() {
        history = new OrderHistory();
        north = new DiningHallModel("North Dining Hall",
            new Menu("Burger", "Pizza", "Salad"), history, 1, 5);
        south = new DiningHallModel("South Dining Hall",
            new Menu("Tacos"), history, 2, 4);
        halls = new LinkedHashMap<>();
        halls.put("North Dining Hall", north);
        halls.put("South Dining Hall", south);
    }

    private OrderController controller(String id, boolean mobile) {
        OrderController c = new OrderController(halls, history, id, mobile);
        c.selectHall("North Dining Hall");
        return c;
    }

    private void order(OrderFormPanel form, String item, String qty)
        throws Exception {
        form.getItemBox().setSelectedItem(item);
        form.getQuantityField().setText(qty);
        form.placeOrder();
        flush();
    }

    /**
     * Panels redraw on the Swing thread when the model changes. Tests run
     * on another thread, so wait for those redraws before checking.
     */
    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    // ------------------------------------------------------- order form

    @Test
    public void formListsSelectedHallMenu() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        assertEquals(3, kiosk.getItemBox().getItemCount());
        assertEquals("Burger", kiosk.getItemBox().getItemAt(0));
    }

    @Test
    public void kioskPlacesWalkInOrder() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        order(kiosk, "Pizza", "2");
        assertTrue(kiosk.getMessage().startsWith("Order #"));
        assertTrue(kiosk.getStatusText().contains("WAITING"));
        assertEquals("1", kiosk.getQuantityField().getText());
        Order last = history.getLast("1");
        assertEquals("Pizza", last.getItem());
        assertEquals(2, last.getQty());
        assertFalse(last.isMobile());
    }

    @Test
    public void mobilePlacesMobileOrder() throws Exception {
        MobilePanel mobile = new MobilePanel(controller("2", true));
        order(mobile, "Burger", "1");
        assertTrue(history.getLast("2").isMobile());
    }

    @Test
    public void nonNumberQuantityIsRejected() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        order(kiosk, "Pizza", "abc");
        assertEquals("Invalid input: Re-enter input (\"abc\" is not a whole "
            + "number).", kiosk.getMessage());
        assertEquals(0, history.size());
    }

    @Test
    public void quantityOverTwentyIsRejected() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        order(kiosk, "Pizza", "25");
        assertTrue(kiosk.getMessage().startsWith("Invalid input: Re-enter "
            + "input"));
        assertEquals("25", kiosk.getQuantityField().getText());
        assertEquals(0, history.size());
    }

    @Test
    public void soldOutItemIsRejected() throws Exception {
        OrderController c = controller("1", false);
        KioskPanel kiosk = new KioskPanel(c);
        c.markSoldOut("Salad");
        flush();
        order(kiosk, "Salad", "1");
        assertTrue(kiosk.getMessage().startsWith("Not available: Salad"));
        assertEquals(0, history.size());
    }

    @Test
    public void statusUpdatesWhenKitchenActs() throws Exception {
        OrderController c = controller("1", false);
        KioskPanel kiosk = new KioskPanel(c);
        KitchenPanel kitchen = new KitchenPanel(c);
        order(kiosk, "Burger", "1");
        kitchen.startNext();
        flush();
        assertTrue(kiosk.getStatusText().contains("BEING_MADE"));
        kitchen.finish();
        flush();
        assertTrue(kiosk.getStatusText().contains("READY"));
        assertTrue(kiosk.getStatusText().contains("wait ~0 min"));
    }

    @Test
    public void statusFollowsOrderAfterHallSwitch() throws Exception {
        OrderController c = controller("1", false);
        KioskPanel kiosk = new KioskPanel(c);
        order(kiosk, "Burger", "1");
        c.selectHall("South Dining Hall");
        kiosk.refresh();
        assertEquals("Tacos", kiosk.getItemBox().getItemAt(0));
        assertTrue(kiosk.getStatusText().contains("North Dining Hall"));
    }

    @Test
    public void lookUpHandlesBadAndUnknownNumbers() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        kiosk.getOrderNumField().setText("x");
        kiosk.lookUp();
        assertEquals("Order numbers are whole numbers.", kiosk.getMessage());
        kiosk.getOrderNumField().setText("999");
        kiosk.lookUp();
        assertEquals("Order not found.", kiosk.getMessage());
    }

    @Test
    public void reorderNeedsHistoryThenRepeatsLastOrder() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        kiosk.reorderLast();
        flush();
        assertTrue(kiosk.getMessage().startsWith("No previous orders"));
        order(kiosk, "Pizza", "3");
        kiosk.reorderLast();
        flush();
        assertEquals(2, history.size());
        assertEquals(3, history.getLast("1").getQty());
    }

    // ------------------------------------------------------ status board

    @Test
    public void boardShowsEmptyMessages() throws Exception {
        StatusBoardPanel board = new StatusBoardPanel(controller("1", false));
        assertEquals("No orders in line.", board.getLineRows().get(0));
        assertEquals("No orders ready for pickup.",
            board.getReadyRows().get(0));
    }

    @Test
    public void boardListsBeingMadeThenWaitingThenReady() throws Exception {
        OrderController c = controller("1", false);
        StatusBoardPanel board = new StatusBoardPanel(c);
        KioskPanel kiosk = new KioskPanel(c);
        order(kiosk, "Burger", "1");
        order(kiosk, "Pizza", "1");
        c.startNextOrder();
        flush();
        List<String> line = board.getLineRows();
        assertEquals(2, line.size());
        assertTrue(line.get(0).contains("Burger")
            && line.get(0).contains("BEING MADE"));
        assertTrue(line.get(1).contains("Pizza")
            && line.get(1).contains("WAITING #1"));
        c.finishOrder();
        flush();
        assertTrue(board.getReadyRows().get(0).contains("Burger"));
    }

    @Test
    public void boardColorsByStatus() throws Exception {
        assertEquals(StatusBoardPanel.WAITING_COLOR,
            StatusBoardPanel.colorFor(OrderStatus.WAITING));
        assertEquals(StatusBoardPanel.BEING_MADE_COLOR,
            StatusBoardPanel.colorFor(OrderStatus.BEING_MADE));
        assertEquals(StatusBoardPanel.READY_COLOR,
            StatusBoardPanel.colorFor(OrderStatus.READY));
        assertEquals(StatusBoardPanel.EMPTY_COLOR,
            StatusBoardPanel.colorFor(null));
    }

    // ------------------------------------------------------------ kitchen

    @Test
    public void kitchenReportsNothingToDo() throws Exception {
        KitchenPanel kitchen = new KitchenPanel(controller("1", false));
        kitchen.startNext();
        flush();
        assertEquals("No orders are waiting.", kitchen.getMessage());
        kitchen.finish();
        flush();
        assertEquals("No orders are being made.", kitchen.getMessage());
    }

    @Test
    public void kitchenReportsFullKitchenAndCounts() throws Exception {
        OrderController c = controller("1", false);
        KitchenPanel kitchen = new KitchenPanel(c);
        KioskPanel kiosk = new KioskPanel(c);
        order(kiosk, "Burger", "1");
        order(kiosk, "Pizza", "1");
        kitchen.startNext();
        flush();
        kitchen.startNext();
        flush();
        assertEquals("The kitchen is full. Finish an order first.",
            kitchen.getMessage());
        assertTrue(kitchen.getSummary().contains("waiting: 1"));
        assertTrue(kitchen.getSummary().contains("being made: 1 of 1"));
    }

    @Test
    public void kitchenMarksSoldOutAndListUpdates() throws Exception {
        OrderController c = controller("1", false);
        KitchenPanel kitchen = new KitchenPanel(c);
        KioskPanel kiosk = new KioskPanel(c);
        kitchen.getSoldOutBox().setSelectedItem("Salad");
        kitchen.markSoldOut();
        flush();
        assertEquals("Salad is now marked sold out.", kitchen.getMessage());
        assertEquals(2, kitchen.getSoldOutBox().getItemCount());
        assertEquals(3, kiosk.getItemBox().getItemCount());
        assertFalse(north.isInStock("Salad"));
    }

    // ------------------------------------------------------------- window

    @Test
    public void windowBuildsPanelsAndSwitchesHalls() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        GrubHubGUI gui = new GrubHubGUI();
        try {
            gui.startSession("s1", true);
            assertTrue(gui.getOrderPanel() instanceof MobilePanel);
            gui.getHallBox().setSelectedItem("West End");
            assertTrue(gui.getKitchenPanel().getSummary()
                .startsWith("West End"));
            gui.startSession("s2", false);
            assertTrue(gui.getOrderPanel() instanceof KioskPanel);
        }
        finally {
            gui.dispose();
        }
    }

    // ------------------------------------------- cancel a waiting order (D4)

    @Test
    public void cancelButtonCancelsAndBoardDropsOrder() throws Exception {
        OrderController c = controller("1", false);
        KioskPanel kiosk = new KioskPanel(c);
        StatusBoardPanel board = new StatusBoardPanel(c);
        order(kiosk, "Burger", "1");
        int num = c.getLastOrderNum();
        kiosk.getOrderNumField().setText(String.valueOf(num));
        kiosk.cancelOrder();
        flush();
        assertEquals("Order #" + num + " cancelled.", kiosk.getMessage());
        assertTrue(kiosk.getStatusText().endsWith("CANCELLED"));
        assertEquals("No orders in line.", board.getLineRows().get(0));
    }

    @Test
    public void cancelButtonRejectsBadNumbers() throws Exception {
        KioskPanel kiosk = new KioskPanel(controller("1", false));
        kiosk.getOrderNumField().setText("abc");
        kiosk.cancelOrder();
        assertTrue(kiosk.getMessage().startsWith("Invalid input: Re-enter "
            + "input"));
        kiosk.getOrderNumField().setText("999");
        kiosk.cancelOrder();
        assertEquals("Order not found.", kiosk.getMessage());
    }

    @Test
    public void menuDropdownReadsModelLive() throws Exception {
        OrderController c = controller("1", false);
        KitchenPanel kitchen = new KitchenPanel(c);
        north.markSoldOut("Pizza"); // straight on the model
        flush();
        assertEquals(2, kitchen.getSoldOutBox().getItemCount());
    }
}
