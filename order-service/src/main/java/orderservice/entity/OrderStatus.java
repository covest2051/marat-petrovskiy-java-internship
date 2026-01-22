package orderservice.entity;

public enum OrderStatus {
    CREATED,
    PROCESSING,
    PROCEED,
    PAYED,
    DELIVERING,
    DELIVERED,
    COMPLETE,
    RETURNED, // Если пользователь вернул товар
    DECLINED
}
