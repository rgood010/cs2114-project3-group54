/**
 * MODEL. The stages of an order's lifecycle. An order moves forward
 * WAITING -> BEING_MADE -> READY, one stage at a time. A WAITING order may
 * instead be CANCELLED by the student who placed it; nothing follows that.
 */
public enum OrderStatus {
    WAITING, BEING_MADE, READY, CANCELLED
}
