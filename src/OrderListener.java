/**
 * MODEL. Observer interface. Views implement this and register with
 * DiningHallModel.addListener so they are told when the model changes.
 */
public interface OrderListener {
    /**
     * Signals that the model state changed. Carries no order data; the
     * listener asks the model for whatever it needs to redraw.
     */
    void modelChanged();
}
