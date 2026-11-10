import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * MODEL. A generic FIFO queue built from singly linked nodes.
 *
 * @param <T> the type of entry stored in the queue
 */
public class LinkedQueue<T> implements Iterable<T> {

    private Node<T> front;
    private Node<T> rear;
    private int count;

    private static class Node<T> {
        private final T data;
        private Node<T> next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    /** Creates an empty queue (null ends, count 0). */
    public LinkedQueue() {
        front = null;
        rear = null;
        count = 0;
    }

    /**
     * Adds an item to the rear of the queue.
     *
     * @param item the item to add
     * @throws IllegalArgumentException if item is null
     */
    public void enqueue(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue null");
        }
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            front = newNode;
        }
        else {
            rear.next = newNode;
        }
        rear = newNode;
        count++;
    }

    /**
     * Removes and returns the front item.
     *
     * @return the front item
     * @throws NoSuchElementException if the queue is empty
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        T removed = front.data;
        front = front.next;
        count--;
        if (count == 0) {
            front = null;
            rear = null;
        }
        return removed;
    }

    /**
     * Returns the front item without removing it.
     *
     * @return the front item
     * @throws NoSuchElementException if the queue is empty
     */
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return front.data;
    }

    /** @return true if the queue has no entries */
    public boolean isEmpty() {
        return count == 0;
    }

    /** @return the number of entries in the queue */
    public int size() {
        return count;
    }

    /**
     * Read-only, front-to-rear iteration. Lets the model inspect the line
     * (for positions and wait times) without removing anything.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = front;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}
