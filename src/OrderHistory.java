import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * MODEL. The record of every accepted order, grouped by student in placement
 * order. One history is shared by all dining halls.
 *
 * File format: one order per line, tab separated:
 * {@code orderNum  studentId  item  qty  MOBILE|WALK_IN  status}.
 * Blank lines and lines starting with '#' are ignored.
 */
public class OrderHistory {

    private static final String HEADER =
        "# GrubHub order history: orderNum\tstudentId\titem\tqty\tchannel"
            + "\tstatus";

    private final Map<String, List<Order>> byStudent;

    /** Creates an empty history. */
    public OrderHistory() {
        byStudent = new LinkedHashMap<>();
    }

    /**
     * Appends an accepted order to its student's history. The same order
     * object is kept, so later stage changes update it in place rather than
     * adding a new entry.
     *
     * @param o the order
     * @throws IllegalArgumentException if o is null
     */
    public void add(Order o) {
        if (o == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        byStudent.computeIfAbsent(o.getStudentId(), k -> new ArrayList<>())
            .add(o);
    }

    /**
     * @param id a student ID
     * @return a detached copy of that student's most recently placed order
     * @throws IllegalArgumentException if id is null or blank
     * @throws NoSuchElementException   if the student has no orders
     */
    public Order getLast(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        List<Order> orders = byStudent.get(id.trim());
        if (orders == null || orders.isEmpty()) {
            throw new NoSuchElementException(
                "No orders found for student " + id.trim());
        }
        return new Order(orders.get(orders.size() - 1));
    }

    /**
     * @return the largest order number in the history, or 0 if empty
     */
    public int getLargestOrderNum() {
        int max = 0;
        for (List<Order> orders : byStudent.values()) {
            for (Order o : orders) {
                max = Math.max(max, o.getOrderNum());
            }
        }
        return max;
    }

    /**
     * @return the total number of orders recorded
     */
    public int size() {
        int total = 0;
        for (List<Order> orders : byStudent.values()) {
            total += orders.size();
        }
        return total;
    }

    /**
     * Writes the history to a file, keeping each student's order sequence.
     *
     * @param path the file to write
     * @throws IllegalArgumentException if path is null or blank
     * @throws UncheckedIOException     if the file cannot be written
     */
    public void save(String path) {
        Path file = toPath(path);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (List<Order> orders : byStudent.values()) {
            for (Order o : orders) {
                lines.add(o.getOrderNum() + "\t" + o.getStudentId() + "\t"
                    + o.getItem() + "\t" + o.getQty() + "\t"
                    + (o.isMobile() ? "MOBILE" : "WALK_IN") + "\t"
                    + o.getStatus());
            }
        }
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not save history", e);
        }
    }

    /**
     * Replaces the history with the records in a saved file. All records are
     * validated first; if any is malformed the current history is left
     * unchanged. Restored orders are not placed in any kitchen queue.
     *
     * @param path the file to read
     * @throws IllegalArgumentException if path is bad or a record is
     *                                  malformed
     * @throws UncheckedIOException     if the file is missing or unreadable
     */
    public void load(String path) {
        Path file = toPath(path);
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read history", e);
        }

        Map<String, List<Order>> restored = new LinkedHashMap<>();
        Set<Integer> seenNums = new HashSet<>();
        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            Order o = parse(line, lineNum);
            if (!seenNums.add(o.getOrderNum())) {
                throw new IllegalArgumentException("Line " + lineNum
                    + ": duplicate order number " + o.getOrderNum());
            }
            restored.computeIfAbsent(o.getStudentId(), k -> new ArrayList<>())
                .add(o);
        }
        byStudent.clear();
        byStudent.putAll(restored);
    }

    private static Order parse(String line, int lineNum) {
        String[] parts = line.split("\t", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException(
                "Line " + lineNum + ": expected 6 fields");
        }
        try {
            int num = Integer.parseInt(parts[0].trim());
            String id = parts[1].trim();
            String item = parts[2].trim();
            int qty = Integer.parseInt(parts[3].trim());
            String channel = parts[4].trim();
            OrderStatus status = OrderStatus.valueOf(parts[5].trim());
            if (num < 1 || qty < 1 || qty > 20 || id.isEmpty()
                || item.isEmpty()) {
                throw new IllegalArgumentException("bad value");
            }
            boolean mobile;
            if (channel.equals("MOBILE")) {
                mobile = true;
            }
            else if (channel.equals("WALK_IN")) {
                mobile = false;
            }
            else {
                throw new IllegalArgumentException("bad channel");
            }
            return new Order(num, id, item, qty, mobile, status);
        }
        catch (IllegalArgumentException e) {
            // NumberFormatException is an IllegalArgumentException too.
            throw new IllegalArgumentException(
                "Line " + lineNum + ": malformed record", e);
        }
    }

    private static Path toPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("File path is required");
        }
        try {
            return Paths.get(path.trim());
        }
        catch (java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("Bad file path: " + path, e);
        }
    }
}
