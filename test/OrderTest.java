import static org.junit.Assert.*;

import org.junit.Test;

public class OrderTest {

    private final Order order = new Order(15, "12345", "Pizza", 2, true);

    @Test
    public void gettersReturnFields() {
        assertEquals(15, order.getOrderNum());
        assertEquals("12345", order.getStudentId());
        assertEquals("Pizza", order.getItem());
        assertEquals(2, order.getQty());
        assertTrue(order.isMobile());
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void statusMovesForwardOneStage() {
        order.setStatus(OrderStatus.BEING_MADE);
        assertEquals(OrderStatus.BEING_MADE, order.getStatus());
        order.setStatus(OrderStatus.BEING_MADE); // same stage: no effect
        order.setStatus(OrderStatus.READY);
        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void skippingStageThrows() {
        order.setStatus(OrderStatus.READY);
    }

    @Test(expected = IllegalStateException.class)
    public void movingBackwardThrows() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.WAITING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullStatusThrows() {
        order.setStatus(null);
    }

    @Test
    public void copyIsDetached() {
        Order copy = new Order(order);
        copy.setStatus(OrderStatus.BEING_MADE);
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }
}
