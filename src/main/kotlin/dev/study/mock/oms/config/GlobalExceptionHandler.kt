package dev.study.mock.oms.config

import dev.study.mock.oms.claim.ClaimNotFoundException
import dev.study.mock.oms.order.DuplicateOrderException
import dev.study.mock.oms.order.ErrorResponse
import dev.study.mock.oms.order.InsufficientStockException
import dev.study.mock.oms.order.OrderNotFoundException
import dev.study.mock.oms.shipment.ShipmentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateOrderException::class)
    fun handleDuplicateOrder(e: DuplicateOrderException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                error = "DUPLICATE_ORDER",
                message = e.message ?: "Order already exists"
            )
        )
    }

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(e: InsufficientStockException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                error = "INSUFFICIENT_STOCK",
                message = e.message ?: "Not enough stock",
                details = mapOf(
                    "productId" to e.productId,
                    "optionId" to e.optionId,
                    "requested" to e.requested,
                    "available" to e.available
                )
            )
        )
    }

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(e: OrderNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = "ORDER_NOT_FOUND",
                message = e.message ?: "Order not found"
            )
        )
    }

    @ExceptionHandler(ClaimNotFoundException::class)
    fun handleClaimNotFound(e: ClaimNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = "CLAIM_NOT_FOUND",
                message = e.message ?: "Claim not found"
            )
        )
    }

    @ExceptionHandler(ShipmentNotFoundException::class)
    fun handleShipmentNotFound(e: ShipmentNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = "SHIPMENT_NOT_FOUND",
                message = e.message ?: "Shipment not found"
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                error = "INTERNAL_ERROR",
                message = e.message ?: "Internal server error"
            )
        )
    }
}
