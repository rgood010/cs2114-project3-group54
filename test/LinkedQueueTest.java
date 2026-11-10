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
}
