import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.junit.Before;
import org.junit.Test;

public class ViewTest {

    private OrderHistory history;
    private DiningHallModel hall;
    private Map<String, DiningHallModel> halls;
    private ByteArrayOutputStream bytes;
    private PrintStream out;

    @Before
    public void setUp() {
        history = new OrderHistory();
        hall = new DiningHallModel("North Dining Hall",
            new Menu("Burger", "Pizza"), history, 2, 5);
        halls = new LinkedHashMap<>();
        halls.put("North Dining Hall", hall);
        bytes = new ByteArrayOutputStream();
        out = new PrintStream(bytes);
    }

    private OrderController controller(boolean mobile) {
        OrderController c = new OrderController(halls, history, "12345",
            mobile);
        c.selectHall("North Dining Hall");
        return c;
    }

    @Test
    public void kioskReadOrderRepromptsBadQuantity() {
        KioskView view = new KioskView(controller(false),
            new Scanner("Burger\nabc\n2\n"), out);
        view.readOrder();
        String text = bytes.toString();
        assertTrue(text.contains("not a whole number"));
        assertTrue(text.contains("placed"));
        assertFalse(history.getLast("12345").isMobile());
    }

    @Test
    public void mobileReadOrderRepromptsAfterRejection() {
        MobileView view = new MobileView(controller(true),
            new Scanner("Burger\n25\nBurger\n3\n"), out);
        view.readOrder();
        assertTrue(bytes.toString().contains("between 1 and 20"));
        Order last = history.getLast("12345");
        assertEquals(3, last.getQty());
        assertTrue(last.isMobile());
    }

    @Test
    public void displayMenuLabelsSoldOut() {
        hall.markSoldOut("Pizza");
        new KioskView(controller(false), new Scanner(""), out).displayMenu();
        assertTrue(bytes.toString().contains("Pizza  (SOLD OUT)"));
    }

    @Test
    public void displayMenuEmpty() {
        halls.put("Empty", new DiningHallModel("Empty", new Menu(), history,
            1, 1));
        OrderController c = new OrderController(halls, history, "1", false);
        c.selectHall("Empty");
        new KioskView(c, new Scanner(""), out).displayMenu();
        assertTrue(bytes.toString().contains("The menu is empty."));
    }

    @Test
    public void statusBoardShowsEmptyStatesAndRefreshes() {
        OrderController c = controller(false);
        StatusBoardView board = new StatusBoardView(c, out);
        board.displayLine();
        board.displayReady();
        assertTrue(bytes.toString().contains("No orders in line."));
        assertTrue(bytes.toString().contains("No orders ready for pickup."));

        c.placeOrder("Burger", 1, false);
        c.placeOrder("Pizza", 1, true);
        c.placeOrder("Burger", 2, false);
        assertEquals(3, board.getLineRows().size());

        c.startNextOrder();
        c.finishOrder();
        assertEquals(2, board.getLineRows().size());
        assertEquals(1, board.getReadyRows().size());
    }
}
