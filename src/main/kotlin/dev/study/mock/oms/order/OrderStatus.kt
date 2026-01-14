package dev.study.mock.oms.order

enum class OrderStatus {
    RECEIVED,           // 접수
    PAYMENT_CONFIRMED,  // 결제확인
    PREPARING,          // 상품준비
    PACKING,            // 포장
    SHIPPED,            // 출고
    IN_DELIVERY,        // 배송중
    DELIVERED           // 완료
}
