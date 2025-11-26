package paymentservice.entity;

public enum PaymentStatus {
    CREATED,
    AUTHORISED_PENDING,
    EXPIRED,
    AUTHORISED,
    PROCESSING,
    CANCELLED,
    DECLINED,
    COMPLETE,
    REFUND_REQUEST,
    REFUND_DECLINED,
    REFUND_COMPLETE,
    ERROR
}