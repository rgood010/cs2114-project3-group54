import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;

public class LinkedQueueTest {

    private LinkedQueue<String> queue;

    @Before
    public void setUp() {
        queue = new LinkedQueue<>();
    }

    @Test
    public void enqueueAddsToRear() {
        queue.enqueue("Burger");
        queue.enqueue("Pizza");
        assertEquals(2, queue.size());
        assertEquals("Burger", queue.peek());
    }

    @Test(expected = IllegalArgumentException.class)
    public void enqueueNullThrows() {
        queue.enqueue(null);
    }

    @Test
    public void dequeueReturnsFrontAndRemovesIt() {
        queue.enqueue("Burger");
        queue.enqueue("Pizza");
        assertEquals("Burger", queue.dequeue());
        assertEquals(1, queue.size());
        assertEquals("Pizza", queue.dequeue());
        assertTrue(queue.isEmpty());
        queue.enqueue("Salad"); // ends were cleared, so reuse still works
        assertEquals("Salad", queue.peek());
    }

    @Test(expected = NoSuchElementException.class)
    public void dequeueEmptyThrows() {
        queue.dequeue();
    }

    @Test
    public void peekDoesNotRemove() {
        queue.enqueue("Burger");
        queue.enqueue("Pizza");
        assertEquals("Burger", queue.peek());
        assertEquals(2, queue.size());
    }

    @Test(expected = NoSuchElementException.class)
    public void peekEmptyThrows() {
        queue.peek();
    }

    @Test
    public void isEmptyAndSize() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        for (int i = 0; i < 4; i++) {
            queue.enqueue("x" + i);
        }
        assertFalse(queue.isEmpty());
        assertEquals(4, queue.size());
    }

    @Test
    public void iteratesFrontToRear() {
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");
        StringBuilder sb = new StringBuilder();
        for (String s : queue) {
            sb.append(s);
        }
        assertEquals("abc", sb.toString());
        assertEquals(3, queue.size());
    }

    // ------------------------------------- interleaving and remove (D4)

    @Test
    public void interleavedEnqueueDequeueKeepsFifoOrder() {
        queue.enqueue("A");
        queue.enqueue("B");
        assertEquals("A", queue.dequeue());
        queue.enqueue("C");
        queue.enqueue("D");
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
        queue.enqueue("E");
        assertEquals("D", queue.peek());
        assertEquals(2, queue.size());
        assertEquals("D", queue.dequeue());
        assertEquals("E", queue.dequeue());
        assertTrue(queue.isEmpty());
        queue.enqueue("F");
        assertEquals("F", queue.peek());
        assertEquals(1, queue.size());
    }

    @Test
    public void iteratorPastEndThrows() {
        queue.enqueue("A");
        java.util.Iterator<String> it = queue.iterator();
        assertEquals("A", it.next());
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("expected NoSuchElementException");
        }
        catch (NoSuchElementException expected) {
            // ok
        }
    }

    @Test
    public void removeFront() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertTrue(queue.remove("A"));
        assertEquals(2, queue.size());
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
    }

    @Test
    public void removeMiddleLinksNeighbours() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertTrue(queue.remove("B"));
        assertEquals(2, queue.size());
        assertEquals("A", queue.dequeue());
        assertEquals("C", queue.dequeue());
    }

    @Test
    public void removeRearMovesRearBack() {
        queue.enqueue("A");
        queue.enqueue("B");
        assertTrue(queue.remove("B"));
        queue.enqueue("C"); // must link after A, the new rear
        assertEquals("A", queue.dequeue());
        assertEquals("C", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void removeOnlyItemClearsBothEnds() {
        queue.enqueue("A");
        assertTrue(queue.remove("A"));
        assertTrue(queue.isEmpty());
        queue.enqueue("B");
        assertEquals("B", queue.peek());
        assertEquals("B", queue.dequeue());
    }

    @Test
    public void removeMissingReturnsFalse() {
        assertFalse(queue.remove("A"));
        queue.enqueue("A");
        assertFalse(queue.remove("Z"));
        assertEquals(1, queue.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullThrows() {
        queue.remove(null);
    }

    @Test
    public void removeBetweenInterleavedOperations() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals("A", queue.dequeue());
        assertTrue(queue.remove("C"));
        queue.enqueue("D");
        assertEquals("B", queue.dequeue());
        assertEquals("D", queue.dequeue());
        assertTrue(queue.isEmpty());
    }
}
