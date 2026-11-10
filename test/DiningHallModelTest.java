import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;

/** Tests the model on its own: no views or controller involved. */
public class DiningHallModelTest {

    private DiningHallModel hall;
    private OrderHistory history;
    private int notifications;

    @Before
    public void setUp() {
        history = new OrderHistory();
        hall = new DiningHallModel("North Dining Hall",
            new Menu("Cheeseburger", "Pizza", "Salad"), history, 2, 5);
        notifications = 0;
        hall.addListener(() -> notifications++);
    }

    @Test
    public void placeOrderQueuesRecordsAndNotifies() {
        int num = hall.placeOrder("12345", "Cheeseburger", 2, true);
        assertEquals(OrderStatus.WAITING, hall.getStatus(num));
        assertEquals(1, hall.getPosition(num));
        assertEquals(1, notifications);
        assertEquals(num, history.getLast("12345").getOrderNum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void placeOrderQuantityOver20Throws() {
        hall.placeOrder("12345", "Cheeseburger", 25, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void placeOrderQuantityZeroThrows() {
        hall.placeOrder("12345", "Cheeseburger", 0, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void placeOrderBlankIdThrows() {
        hall.placeOrder(" ", "Cheeseburger", 1, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void placeOrderUnknownItemThrows() {
        hall.placeOrder("12345", "Tacos", 1, true);
    }

    @Test
    public void placeOrderSoldOutThrowsAndDoesNotNotify() {
        hall.markSoldOut("Pizza");
        notifications = 0;
        try {
            hall.placeOrder("12345", "Pizza", 1, false);
            fail("expected IllegalStateException");
        }
        catch (IllegalStateException expected) {
            // ok
        }
        assertEquals(0, notifications);
        assertEquals(0, history.size());
    }

    @Test
    public void startNextOrderMovesToBeingMade() {
        int num = hall.placeOrder("12345", "Pizza", 1, false);
        hall.startNextOrder();
        assertEquals(OrderStatus.BEING_MADE, hall.getStatus(num));
        assertEquals(0, hall.getPosition(num));
        assertEquals(2, notifications);
    }

    @Test(expected = IllegalStateException.class)
    public void startNextOrderKitchenFullThrows() {
        for (int i = 0; i < 3; i++) {
            hall.placeOrder("12345", "Pizza", 1, false);
        }
        hall.startNextOrder();
        hall.startNextOrder();
        hall.startNextOrder(); // capacity is 2
    }

    @Test(expected = NoSuchElementException.class)
    public void startNextOrderNothingWaitingThrows() {
        hall.startNextOrder();
    }

    @Test
    public void finishOrderMovesFrontToReady() {
        int a = hall.placeOrder("1", "Pizza", 1, false);
        int b = hall.placeOrder("2", "Salad", 1, false);
        hall.startNextOrder();
        hall.startNextOrder();
        hall.finishOrder();
        assertEquals(OrderStatus.READY, hall.getStatus(a));
        assertEquals(OrderStatus.BEING_MADE, hall.getStatus(b));
        assertEquals(1, hall.getReadyOrders().size());
    }

    @Test(expected = NoSuchElementException.class)
    public void finishOrderEmptyThrows() {
        hall.finishOrder();
    }

    @Test
    public void fairnessThreeMobileThenOneWalkIn() {
        DiningHallModel big = new DiningHallModel("Big", new Menu("Pizza"),
            history, 10, 5);
        int m1 = big.placeOrder("a", "Pizza", 1, true);
        int m2 = big.placeOrder("b", "Pizza", 1, true);
        int w1 = big.placeOrder("c", "Pizza", 1, false);
        int m3 = big.placeOrder("d", "Pizza", 1, true);
        int m4 = big.placeOrder("e", "Pizza", 1, true);

        assertEquals(1, big.getPosition(m1));
        assertEquals(2, big.getPosition(m2));
        assertEquals(3, big.getPosition(m3));
        assertEquals(4, big.getPosition(w1));
        assertEquals(5, big.getPosition(m4));

        big.startNextOrder();
        big.startNextOrder();
        big.startNextOrder();
        assertEquals(OrderStatus.WAITING, big.getStatus(w1));
        big.startNextOrder();
        assertEquals(OrderStatus.BEING_MADE, big.getStatus(w1));
        assertEquals(OrderStatus.WAITING, big.getStatus(m4));
    }

    @Test
    public void walkInsServedWhenNoMobileWaiting() {
        int w1 = hall.placeOrder("a", "Pizza", 1, false);
        int m1 = hall.placeOrder("b", "Pizza", 1, true);
        assertEquals(1, hall.getPosition(m1));
        assertEquals(2, hall.getPosition(w1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPositionUnknownThrows() {
        hall.getPosition(999);
    }

    @Test
    public void waitTimes() {
        int a = hall.placeOrder("1", "Pizza", 1, false);
        int b = hall.placeOrder("2", "Pizza", 1, false);
        int c = hall.placeOrder("3", "Pizza", 1, false);
        assertEquals(15, hall.getWaitTime(c)); // third in line, kitchen empty
        hall.startNextOrder();
        assertEquals(5, hall.getWaitTime(a)); // first in kitchen
        assertEquals(10, hall.getWaitTime(b)); // 1 being made + position 1
        hall.finishOrder();
        assertEquals(0, hall.getWaitTime(a));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWaitTimeUnknownThrows() {
        hall.getWaitTime(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getStatusUnknownThrows() {
        hall.getStatus(999);
    }

    @Test
    public void orderAtOtherHallIsUnknownHere() {
        DiningHallModel other = new DiningHallModel("South", new Menu("Pizza"),
            history, 1, 5);
        int num = other.placeOrder("1", "Pizza", 1, true);
        try {
            hall.getStatus(num);
            fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void addListenerTwiceNotifiesOnce() {
        int[] count = {0};
        OrderListener l = () -> count[0]++;
        hall.addListener(l);
        hall.addListener(l);
        hall.placeOrder("1", "Pizza", 1, true);
        assertEquals(1, count[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullListenerThrows() {
        hall.addListener(null);
    }

    @Test
    public void orderNumbersIncrease() {
        int a = hall.placeOrder("1", "Pizza", 1, true);
        int b = hall.placeOrder("1", "Pizza", 1, true);
        assertTrue(b > a);
    }
}
