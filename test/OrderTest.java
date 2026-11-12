import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests order data, status changes, constructor validation, and copying.
 */
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

        order.setStatus(OrderStatus.BEING_MADE);
        assertEquals(OrderStatus.BEING_MADE, order.getStatus());

        order.setStatus(OrderStatus.READY);
        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void skippingStageThrows() {
        try {
            order.setStatus(OrderStatus.READY);
            fail("Skipping directly to READY should throw an exception");
        }
        catch (IllegalStateException expected) {
            // Expected: an order cannot skip BEING_MADE.
        }

        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void movingBackwardThrows() {
        order.setStatus(OrderStatus.BEING_MADE);

        try {
            order.setStatus(OrderStatus.WAITING);
            fail("Moving backward to WAITING should throw an exception");
        }
        catch (IllegalStateException expected) {
            // Expected: an order cannot move backward.
        }

        assertEquals(OrderStatus.BEING_MADE, order.getStatus());
    }

    @Test
    public void nullStatusThrows() {
        for (OrderStatus stage : OrderStatus.values()) {
            order.setStatus(stage);

            try {
                order.setStatus(null);
                fail("A null status should throw an exception at " + stage);
            }
            catch (IllegalArgumentException expected) {
                // Expected: null is not a valid status.
            }

            assertEquals(stage, order.getStatus());
        }
    }

    @Test
    public void copyIsDetached() {
        Order copy = new Order(order);
        assertNotSame(order, copy);

        copy.setStatus(OrderStatus.BEING_MADE);

        assertEquals(OrderStatus.BEING_MADE, copy.getStatus());
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void repeatingWaitingDoesNotChangeStatus() {
        assertEquals(OrderStatus.WAITING, order.getStatus());

        order.setStatus(OrderStatus.WAITING);

        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void repeatingReadyDoesNotChangeStatus() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.READY);
        assertEquals(OrderStatus.READY, order.getStatus());

        order.setStatus(OrderStatus.READY);

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void readyToWaitingThrowsAndKeepsStatus() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.READY);

        try {
            order.setStatus(OrderStatus.WAITING);
            fail("Moving from READY to WAITING should throw an exception");
        }
        catch (IllegalStateException expected) {
            // Expected: a ready order cannot return to WAITING.
        }

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void readyToBeingMadeThrowsAndKeepsStatus() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.READY);

        try {
            order.setStatus(OrderStatus.BEING_MADE);
            fail("Moving from READY to BEING_MADE should throw an exception");
        }
        catch (IllegalStateException expected) {
            // Expected: a ready order cannot return to BEING_MADE.
        }

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullStudentId() {
        new Order(15, null, "Pizza", 2, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsEmptyStudentId() {
        new Order(15, "", "Pizza", 2, true);
    }

    @Test
    public void restoredOrderKeepsSuppliedStatus() {
        for (OrderStatus stage : OrderStatus.values()) {
            Order restored = new Order(15, "12345", "Pizza", 2, true, stage);

            assertEquals(stage, restored.getStatus());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoredOrderRejectsNullStatus() {
        new Order(15, "12345", "Pizza", 2, true, null);
    }

    @Test
    public void changingOriginalDoesNotChangeCopy() {
        order.setStatus(OrderStatus.BEING_MADE);
        Order copy = new Order(order);
        assertEquals(OrderStatus.BEING_MADE, copy.getStatus());

        order.setStatus(OrderStatus.READY);

        assertEquals(OrderStatus.READY, order.getStatus());
        assertEquals(OrderStatus.BEING_MADE, copy.getStatus());
    }
}
