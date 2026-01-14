package dev.study.mock.oms.shipment

enum class ShipmentStatus {
    PICKED_UP,         // 집화
    IN_TRANSIT,        // 간선터미널
    OUT_FOR_DELIVERY,  // 배송중
    DELIVERED,         // 배송완료
    CONFIRMED          // 수령확인
}
