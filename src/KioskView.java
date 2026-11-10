import java.io.PrintStream;
import java.util.Scanner;

/** VIEW. The walk-in kiosk. Submits orders with mobile = false. */
public class KioskView extends OrderFormView {

    /**
     * @param controller the session's controller
     * @param in         where to read input
     * @param out        where to print output
     */
    public KioskView(OrderController controller, Scanner in, PrintStream out) {
        super(controller, in, out);
    }

    @Override
    protected String label() {
        return "Kiosk";
    }

    @Override
    protected boolean isMobile() {
        return false;
    }
}
