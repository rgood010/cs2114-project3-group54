import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class OrderControllerTest {

    private OrderHistory history;
    private DiningHallModel north;
    private DiningHallModel south;
    private OrderController controller;

    @Before
    public void setUp() {
        history = new OrderHistory();
        north = new DiningHallModel("North Dining Hall",
            new Menu("Burger", "Pizza"), history, 2, 5);
        south = new DiningHallModel("South Dining Hall",
            new Menu("Tacos"), history, 2, 5);
        Map<String, DiningHallModel> halls = new LinkedHashMap<>();
        halls.put("North Dining Hall", north);
        halls.put("South Dining Hall", south);
        controller = new OrderController(halls, history, "12345", true);
    }

    @Test
    public void selectHallSwitchesModel() {
        controller.selectHall("north dining hall");
        assertSame(north, controller.getSelectedHall());
    }

    @Test
    public void selectUnknownHallKeepsPrevious() {
        controller.selectHall("North Dining Hall");
        try {
            controller.selectHall("Unknown Hall");
            fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // ok
        }
        assertSame(north, controller.getSelectedHall());
    }

    @Test
    public void placeOrderUsesSessionStudent() {
        controller.selectHall("North Dining Hall");
        controller.placeOrder("Burger", 2, true);
        assertTrue(controller.lastActionSucceeded());
        int num = controller.getLastOrderNum();
        assertTrue(controller.getLastMessage().contains("#" + num));
        assertEquals("12345", history.getLast("12345").getStudentId());
        assertEquals(OrderStatus.WAITING, north.getStatus(num));
    }

    @Test
    public void placeOrderTooManyIsReported() {
        controller.selectHall("North Dining Hall");
        controller.placeOrder("Burger", 25, true);
        assertFalse(controller.lastActionSucceeded());
        assertTrue(controller.getLastMessage().contains("quantity"));
        assertEquals(0, history.size());
    }

    @Test
    public void placeOrderSoldOutIsReported() {
        controller.selectHall("North Dining Hall");
        controller.markSoldOut("Pizza");
        controller.placeOrder("Pizza", 1, true);
        assertFalse(controller.lastActionSucceeded());
        assertTrue(controller.getLastMessage().contains("sold out"));
    }

    @Test(expected = IllegalStateException.class)
    public void placeOrderNoHallThrows() {
        controller.placeOrder("Burger", 1, true);
    }

    @Test
    public void lookupStatus() {
        assertEquals("Select a dining hall first.",
            controller.lookupStatus(1));
        controller.selectHall("North Dining Hall");
        controller.placeOrder("Burger", 1, true);
        int num = controller.getLastOrderNum();
        controller.startNextOrder();
        String text = controller.lookupStatus(num);
        assertTrue(text.contains("BEING_MADE"));
        assertTrue(text.contains("position 0"));
        assertTrue(text.contains("5 min"));
        assertEquals("Order not found.", controller.lookupStatus(999));
    }

    @Test
    public void reorderLastPlacesNewOrder() {
        controller.selectHall("North Dining Hall");
        controller.placeOrder("Burger", 2, false);
        int first = controller.getLastOrderNum();
        controller.reorderLast("12345");
        assertTrue(controller.lastActionSucceeded());
        int second = controller.getLastOrderNum();
        assertNotEquals(first, second);
        Order last = history.getLast("12345");
        assertEquals("Burger", last.getItem());
        assertEquals(2, last.getQty());
        assertTrue(last.isMobile()); // uses the session's channel
    }

    @Test
    public void reorderLastNoHistoryIsReported() {
        OrderController other = new OrderController(
            Map.of("North Dining Hall", north), history, "99999", true);
        other.selectHall("North Dining Hall");
        other.reorderLast("99999");
        assertFalse(other.lastActionSucceeded());
        assertTrue(other.getLastMessage().contains("No previous orders"));
    }

    @Test
    public void reorderLastItemNotAtThisHallIsReported() {
        controller.selectHall("North Dining Hall");
        controller.placeOrder("Burger", 1, true);
        controller.selectHall("South Dining Hall");
        controller.reorderLast("12345");
        assertFalse(controller.lastActionSucceeded());
    }

    @Test
    public void reorderLastNoHallIsReported() {
        controller.reorderLast("12345");
        assertFalse(controller.lastActionSucceeded());
        assertEquals("Select a dining hall first.",
            controller.getLastMessage());
    }

    @Test
    public void reorderLastWrongStudentIsReported() {
        controller.selectHall("North Dining Hall");
        controller.reorderLast("55555");
        assertFalse(controller.lastActionSucceeded());
    }
}
