import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;

public class OrderHistoryTest {

    private OrderHistory history;
    private File file;

    @Before
    public void setUp() throws IOException {
        history = new OrderHistory();
        file = File.createTempFile("history", ".txt");
        file.deleteOnExit();
    }

    @Test
    public void addAndGetLastReturnsDetachedCopy() {
        history.add(new Order(1, "12345", "Burger", 1, false));
        history.add(new Order(2, "12345", "Pizza", 2, true));
        Order third = new Order(3, "12345", "Salad", 3, false);
        history.add(third);
        Order last = history.getLast("12345");
        assertEquals(3, last.getOrderNum());
        assertEquals("Salad", last.getItem());
        assertNotSame(third, last);
        last.setStatus(OrderStatus.BEING_MADE);
        assertEquals(OrderStatus.WAITING, third.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullThrows() {
        history.add(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void getLastNoHistoryThrows() {
        history.getLast("99999");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getLastBlankIdThrows() {
        history.getLast("  ");
    }

    @Test
    public void saveThenLoadRoundTrips() {
        history.add(new Order(4, "12345", "Burger", 2, false));
        history.add(new Order(7, "12345", "Pizza", 3, true));
        history.add(new Order(5, "777", "Salad", 1, true));
        history.save(file.getPath());

        OrderHistory restored = new OrderHistory();
        restored.load(file.getPath());
        assertEquals(3, restored.size());
        assertEquals(7, restored.getLargestOrderNum());
        Order last = restored.getLast("12345");
        assertEquals("Pizza", last.getItem());
        assertEquals(3, last.getQty());
        assertTrue(last.isMobile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveBlankPathThrows() {
        history.save(" ");
    }

    @Test
    public void loadMalformedLeavesHistoryUnchanged() throws IOException {
        history.add(new Order(1, "12345", "Burger", 2, false));
        Files.write(file.toPath(), Arrays.asList(
            "2\t12345\tPizza\t1\tMOBILE\tWAITING",
            "not a record"));
        try {
            history.load(file.getPath());
            fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // ok
        }
        assertEquals(1, history.size());
        assertEquals("Burger", history.getLast("12345").getItem());
    }

    @Test(expected = UncheckedIOException.class)
    public void loadMissingFileThrows() {
        history.load(file.getPath() + ".missing");
    }

    @Test
    public void newOrderNumbersExceedRestoredNumbers() throws IOException {
        Files.write(file.toPath(), Arrays.asList(
            "500\t12345\tPizza\t1\tMOBILE\tREADY"));
        OrderHistory shared = new OrderHistory();
        shared.load(file.getPath());
        DiningHallModel hall = new DiningHallModel("Test",
            new Menu("Pizza"), shared, 1, 5);
        assertTrue(hall.placeOrder("12345", "Pizza", 1, true) > 500);
    }
}
