import java.io.PrintStream;
import java.util.Scanner;

/** VIEW. The mobile app. Submits orders with mobile = true. */
public class MobileView extends OrderFormView {

    /**
     * @param controller the session's controller
     * @param in         where to read input
     * @param out        where to print output
     */
    public MobileView(OrderController controller, Scanner in,
        PrintStream out) {
        super(controller, in, out);
    }

    @Override
    protected String label() {
        return "Mobile";
    }

    @Override
    protected boolean isMobile() {
        return true;
    }
}
