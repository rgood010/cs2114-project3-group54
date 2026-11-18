/** VIEW (Swing). The walk-in kiosk form. Submits orders with mobile = false. */
public class KioskPanel extends OrderFormPanel {

    /**
     * @param controller the session's controller
     */
    public KioskPanel(OrderController controller) {
        super(controller);
    }

    @Override
    protected String label() {
        return "Kiosk (walk-in)";
    }

    @Override
    protected boolean isMobile() {
        return false;
    }
}
