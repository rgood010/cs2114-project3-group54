/** VIEW (Swing). The mobile app form. Submits orders with mobile = true. */
public class MobilePanel extends OrderFormPanel {

    /**
     * @param controller the session's controller
     */
    public MobilePanel(OrderController controller) {
        super(controller);
    }

    @Override
    protected String label() {
        return "Mobile app";
    }

    @Override
    protected boolean isMobile() {
        return true;
    }
}
