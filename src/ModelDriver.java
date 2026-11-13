/**
 * A tiny driver that exercises the model on its own: no views, no
 * controller, no keyboard input. Run it to see the queue-backed logic work.
 */
public class ModelDriver {

    /**
     * @param args unused
     */
    public static void main(String[] args) {
        OrderHistory history = new OrderHistory();
        DiningHallModel hall = new DiningHallModel("North Dining Hall",
            new Menu("Cheeseburger", "Pizza", "Salad"), history, 2, 5);
        hall.addListener(() -> System.out.println("  (model changed)"));

        System.out.println("Placing 4 mobile orders and 1 walk-in order:");
        int m1 = hall.placeOrder("111", "Pizza", 1, true);
        int m2 = hall.placeOrder("222", "Salad", 2, true);
        int w1 = hall.placeOrder("333", "Cheeseburger", 1, false);
        int m3 = hall.placeOrder("444", "Pizza", 3, true);
        int m4 = hall.placeOrder("555", "Salad", 1, true);
        int[] all = {m1, m2, w1, m3, m4};
        printAll(hall, all);

        System.out.println("\nKitchen starts 2 orders (capacity is 2):");
        hall.startNextOrder();
        hall.startNextOrder();
        printAll(hall, all);

        System.out.println("\nKitchen finishes 1 order and starts 1 more:");
        hall.finishOrder();
        hall.startNextOrder();
        printAll(hall, all);

        System.out.println("\nBad input is rejected by the model:");
        tryPlace(hall, "111", "Pizza", 25);
        tryPlace(hall, "111", "Tacos", 1);
        hall.markSoldOut("Salad");
        tryPlace(hall, "111", "Salad", 1);
        try {
            hall.startNextOrder();
        }
        catch (IllegalStateException e) {
            System.out.println("  startNextOrder -> " + e.getMessage());
        }

        System.out.println("\nStudent 222's last order: "
            + history.getLast("222"));
    }

    private static void printAll(DiningHallModel hall, int[] nums) {
        for (int n : nums) {
            System.out.printf("  #%d  %-10s position %d  wait ~%d min%n", n,
                hall.getStatus(n), hall.getPosition(n), hall.getWaitTime(n));
        }
    }

    private static void tryPlace(DiningHallModel hall, String id, String item,
        int qty) {
        try {
            hall.placeOrder(id, item, qty, true);
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  placeOrder(" + item + ", " + qty + ") -> "
                + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
