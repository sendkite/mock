package dev.study.mock.oms.order

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<CreateOrderResponse> {
        val response = orderService.createOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: String): ResponseEntity<OrderResponse> {
        val response = orderService.getOrder(orderId)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: String,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<OrderResponse> {
        val response = orderService.updateOrderStatus(orderId, request.status)
        return ResponseEntity.ok(response)
    }
}

data class UpdateStatusRequest(val status: OrderStatus)
