import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests order data, status changes, constructor validation,
 * restoring orders, and copying.
 */
public class OrderTest {

    private final Order order =
        new Order(15, "12345", "Pizza", 2, true);

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

        order.setStatus(OrderStatus.READY);
        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void skippingStageThrowsAndKeepsStatus() {
        assertEquals(OrderStatus.WAITING, order.getStatus());

        try {
            order.setStatus(OrderStatus.READY);
            fail("Skipping directly to READY should throw");
        }
        catch (IllegalStateException expected) {
            // Expected exception.
        }

        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void movingBackwardThrowsAndKeepsStatus() {
        order.setStatus(OrderStatus.BEING_MADE);

        try {
            order.setStatus(OrderStatus.WAITING);
            fail("Moving backward should throw");
        }
        catch (IllegalStateException expected) {
            // Expected exception.
        }

        assertEquals(OrderStatus.BEING_MADE, order.getStatus());
    }

    @Test
    public void nullStatusThrowsAndKeepsStatus() {
        for (OrderStatus stage : OrderStatus.values()) {
            Order testOrder = new Order(
                15, "12345", "Pizza", 2, true, stage);

            try {
                testOrder.setStatus(null);
                fail("Null status should throw");
            }
            catch (IllegalArgumentException expected) {
                // Expected exception.
            }

            assertEquals(stage, testOrder.getStatus());
        }
    }

    @Test
    public void repeatingWaitingDoesNotChangeStatus() {
        assertEquals(OrderStatus.WAITING, order.getStatus());

        order.setStatus(OrderStatus.WAITING);

        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void repeatingBeingMadeDoesNotChangeStatus() {
        order.setStatus(OrderStatus.BEING_MADE);

        order.setStatus(OrderStatus.BEING_MADE);

        assertEquals(OrderStatus.BEING_MADE, order.getStatus());
    }

    @Test
    public void repeatingReadyDoesNotChangeStatus() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.READY);

        order.setStatus(OrderStatus.READY);

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void readyToWaitingThrowsAndKeepsStatus() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.READY);

        try {
            order.setStatus(OrderStatus.WAITING);
            fail("READY to WAITING should throw");
        }
        catch (IllegalStateException expected) {
            // Expected exception.
        }

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void readyToBeingMadeThrowsAndKeepsStatus() {
        order.setStatus(OrderStatus.BEING_MADE);
        order.setStatus(OrderStatus.READY);

        try {
            order.setStatus(OrderStatus.BEING_MADE);
            fail("READY to BEING_MADE should throw");
        }
        catch (IllegalStateException expected) {
            // Expected exception.
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

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsBlankStudentId() {
        new Order(15, "   ", "Pizza", 2, true);
    }

    @Test
    public void restoredOrderKeepsSuppliedStatus() {
        for (OrderStatus stage : OrderStatus.values()) {
            Order restored = new Order(
                15, "12345", "Pizza", 2, true, stage);

            assertEquals(stage, restored.getStatus());
            assertEquals(15, restored.getOrderNum());
            assertEquals("12345", restored.getStudentId());
            assertEquals("Pizza", restored.getItem());
            assertEquals(2, restored.getQty());
            assertTrue(restored.isMobile());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoredOrderRejectsNullStatus() {
        new Order(15, "12345", "Pizza", 2, true, null);
    }

    @Test
    public void copyIsDetached() {
        Order copy = new Order(order);

        assertNotSame(order, copy);
        assertEquals(order.getOrderNum(), copy.getOrderNum());
        assertEquals(order.getStudentId(), copy.getStudentId());
        assertEquals(order.getItem(), copy.getItem());
        assertEquals(order.getQty(), copy.getQty());
        assertEquals(order.isMobile(), copy.isMobile());
        assertEquals(order.getStatus(), copy.getStatus());

        copy.setStatus(OrderStatus.BEING_MADE);

        assertEquals(OrderStatus.BEING_MADE, copy.getStatus());
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void changingOriginalDoesNotChangeCopy() {
        order.setStatus(OrderStatus.BEING_MADE);

        Order copy = new Order(order);

        assertNotSame(order, copy);
        assertEquals(OrderStatus.BEING_MADE, copy.getStatus());

        order.setStatus(OrderStatus.READY);

        assertEquals(OrderStatus.READY, order.getStatus());
        assertEquals(OrderStatus.BEING_MADE, copy.getStatus());
    }
}

