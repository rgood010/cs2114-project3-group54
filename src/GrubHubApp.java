import java.io.File;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Entry point. Builds the model (halls, menus, shared history), then runs
 * ordering sessions. Each session gets its own controller and views; all
 * user actions go view -> controller -> model.
 */
public class GrubHubApp {

    /** Where order history is saved between runs. */
    public static final String HISTORY_FILE = "order_history.txt";

    private final Scanner in;
    private final PrintStream out;
    private final OrderHistory history = new OrderHistory();
    private final Map<String, DiningHallModel> halls = new LinkedHashMap<>();

    private OrderController controller;
    private OrderFormView orderView;
    private StatusBoardView board;

    /**
     * @param in  input source
     * @param out output destination
     */
    public GrubHubApp(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
        halls.put("North Dining Hall", new DiningHallModel("North Dining Hall",
            new Menu("Cheeseburger", "Pizza", "Salad", "Fries"), history, 2,
            5));
        halls.put("South Dining Hall", new DiningHallModel("South Dining Hall",
            new Menu("Burrito", "Tacos", "Chicken Tenders", "Fries"), history,
            3, 4));
        halls.put("West End", new DiningHallModel("West End",
            new Menu("Steak Sandwich", "Pasta", "Burger", "Salad"), history, 2,
            6));
    }

    /**
     * @param args unused
     */
    public static void main(String[] args) {
        new GrubHubApp(new Scanner(System.in), System.out).run();
    }

    /** Loads history, runs the menu loop, and saves on quit. */
    public void run() {
        loadHistory();
        if (!startSession()) {
            return;
        }
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Choose: ");
            if (choice == null) {
                break;
            }
            switch (choice) {
                case "1":
                    selectHall();
                    break;
                case "2":
                    orderView.displayMenu();
                    break;
                case "3":
                    orderView.readOrder();
                    break;
                case "4":
                    orderView.reorderLast();
                    break;
                case "5":
                    lookup();
                    break;
                case "6":
                    board.displayLine();
                    board.displayReady();
                    break;
                case "7":
                    controller.startNextOrder();
                    out.println(controller.getLastMessage());
                    break;
                case "8":
                    controller.finishOrder();
                    out.println(controller.getLastMessage());
                    break;
                case "9":
                    String item = readLine("Item to mark sold out: ");
                    if (item != null) {
                        controller.markSoldOut(item);
                        out.println(controller.getLastMessage());
                    }
                    break;
                case "10":
                    String hall = controller.getSelectedHall() == null ? null
                        : controller.getSelectedHall().getHallName();
                    endSession();
                    if (!startSession()) {
                        running = false;
                    }
                    else if (hall != null) {
                        controller.selectHall(hall);
                    }
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    out.println("Please choose one of the listed options.");
            }
        }
        saveHistory();
    }

    private void printMenu() {
        String hall = controller.getSelectedHall() == null ? "none"
            : controller.getSelectedHall().getHallName();
        out.println();
        out.println("=== GrubHub | student " + controller.getStudentId()
            + " | " + (controller.isMobileSession() ? "mobile" : "kiosk")
            + " | hall: " + hall + " ===");
        out.println(" 1) Select dining hall      2) Show menu");
        out.println(" 3) Place order             4) Reorder my last order");
        out.println(" 5) Look up order status    6) Show status board");
        out.println(" 7) Kitchen: start next     8) Kitchen: finish order");
        out.println(" 9) Kitchen: mark sold out 10) New student session");
        out.println(" 0) Save and quit");
    }

    private boolean startSession() {
        String id = null;
        while (id == null || id.isEmpty()) {
            id = readLine("Student ID: ");
            if (id == null) {
                return false;
            }
        }
        Boolean mobile = null;
        while (mobile == null) {
            String channel = readLine("Order from (k)iosk or (m)obile? ");
            if (channel == null) {
                return false;
            }
            if (channel.equalsIgnoreCase("k")) {
                mobile = false;
            }
            else if (channel.equalsIgnoreCase("m")) {
                mobile = true;
            }
        }
        controller = new OrderController(halls, history, id, mobile);
        orderView = mobile ? new MobileView(controller, in, out)
            : new KioskView(controller, in, out);
        board = new StatusBoardView(controller, out);
        return true;
    }

    private void endSession() {
        controller.removeListener(orderView);
        controller.removeListener(board);
    }

    private void selectHall() {
        out.println("Dining halls: " + String.join(", ",
            controller.getHallNames()));
        String name = readLine("Hall name: ");
        if (name == null) {
            return;
        }
        try {
            controller.selectHall(name);
            out.println("Selected " + controller.getSelectedHall()
                .getHallName() + ".");
        }
        catch (IllegalArgumentException e) {
            out.println(e.getMessage());
        }
    }

    private void lookup() {
        String text = readLine("Order number: ");
        if (text == null) {
            return;
        }
        try {
            out.println(controller.lookupStatus(Integer.parseInt(text)));
        }
        catch (NumberFormatException e) {
            out.println("Order numbers are whole numbers.");
        }
    }

    private void loadHistory() {
        if (!new File(HISTORY_FILE).exists()) {
            return;
        }
        try {
            history.load(HISTORY_FILE);
            out.println("Loaded " + history.size() + " past orders.");
        }
        catch (IllegalArgumentException | UncheckedIOException e) {
            out.println("Could not load order history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try {
            history.save(HISTORY_FILE);
            out.println("Order history saved. Goodbye!");
        }
        catch (UncheckedIOException e) {
            out.println("Could not save order history: " + e.getMessage());
        }
    }

    private String readLine(String prompt) {
        out.print(prompt);
        if (!in.hasNextLine()) {
            return null;
        }
        return in.nextLine().trim();
    }
}
