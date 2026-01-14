package dev.study.mock.oms.order

import dev.study.mock.oms.stock.Stock
import dev.study.mock.oms.stock.StockRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * OrderService 단위 테스트
 */
@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    lateinit var orderRepository: OrderRepository

    @Mock
    lateinit var stockRepository: StockRepository

    @InjectMocks
    lateinit var orderService: OrderService

    @Test
    fun `주문 생성 성공`() {
        // given
        val request = CreateOrderRequest(
            orderId = "ORD-001",
            orderedAt = Instant.now(),
            orderLines = listOf(
                OrderLineRequest(
                    lineId = "LINE-001",
                    productId = "PROD-001",
                    optionId = "OPT-001",
                    productName = "테스트 상품",
                    optionName = "기본",
                    quantity = 2,
                    unitPrice = 15000,
                    totalPrice = 30000
                )
            ),
            shipping = ShippingRequest(
                recipientName = "홍길동",
                phoneNumber = "010-1234-5678",
                zipCode = "12345",
                address = "서울시 강남구"
            ),
            totalAmount = 30000
        )

        whenever(orderRepository.existsByOrderId("ORD-001")).thenReturn(false)
        whenever(stockRepository.findByProductIdAndOptionId("PROD-001", "OPT-001"))
            .thenReturn(Stock("PROD-001", "OPT-001", 100))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }
        whenever(stockRepository.save(any<Stock>())).thenAnswer { it.arguments[0] }

        // when
        val response = orderService.createOrder(request)

        // then
        assertEquals("ORD-001", response.orderId)
        assertEquals(OrderStatus.RECEIVED, response.status)
        assertNotNull(response.omsOrderId)
    }

    @Test
    fun `중복 주문 시 DuplicateOrderException 발생`() {
        // given
        val request = CreateOrderRequest(
            orderId = "ORD-DUPLICATE",
            orderedAt = Instant.now(),
            orderLines = listOf(
                OrderLineRequest(
                    lineId = "LINE-001",
                    productId = "PROD-001",
                    optionId = "OPT-001",
                    productName = "테스트 상품",
                    optionName = "기본",
                    quantity = 1,
                    unitPrice = 10000,
                    totalPrice = 10000
                )
            ),
            shipping = ShippingRequest(
                recipientName = "홍길동",
                phoneNumber = "010-1234-5678",
                zipCode = "12345",
                address = "서울시 강남구"
            ),
            totalAmount = 10000
        )

        whenever(orderRepository.existsByOrderId("ORD-DUPLICATE")).thenReturn(true)

        // when & then
        assertThrows<DuplicateOrderException> {
            orderService.createOrder(request)
        }
    }

    @Test
    fun `재고 부족 시 InsufficientStockException 발생`() {
        // given
        val request = CreateOrderRequest(
            orderId = "ORD-INSUFFICIENT",
            orderedAt = Instant.now(),
            orderLines = listOf(
                OrderLineRequest(
                    lineId = "LINE-001",
                    productId = "PROD-001",
                    optionId = "OPT-001",
                    productName = "테스트 상품",
                    optionName = "기본",
                    quantity = 100, // 재고보다 많이 주문
                    unitPrice = 10000,
                    totalPrice = 1000000
                )
            ),
            shipping = ShippingRequest(
                recipientName = "홍길동",
                phoneNumber = "010-1234-5678",
                zipCode = "12345",
                address = "서울시 강남구"
            ),
            totalAmount = 1000000
        )

        whenever(orderRepository.existsByOrderId("ORD-INSUFFICIENT")).thenReturn(false)
        whenever(stockRepository.findByProductIdAndOptionId("PROD-001", "OPT-001"))
            .thenReturn(Stock("PROD-001", "OPT-001", 50)) // 재고 50개

        // when & then
        val exception = assertThrows<InsufficientStockException> {
            orderService.createOrder(request)
        }
        assertEquals("PROD-001", exception.productId)
        assertEquals(100, exception.requested)
        assertEquals(50, exception.available)
    }

    @Test
    fun `주문 조회 성공`() {
        // given
        val order = Order(
            orderId = "ORD-001",
            omsOrderId = "OMS-ORD-12345",
            shipping = ShippingInfo(
                recipientName = "홍길동",
                phoneNumber = "010-1234-5678",
                zipCode = "12345",
                address = "서울시 강남구",
                addressDetail = null,
                memo = null,
                entranceCode = null
            ),
            totalAmount = 30000,
            orderedAt = Instant.now()
        )

        whenever(orderRepository.findById("ORD-001")).thenReturn(Optional.of(order))

        // when
        val response = orderService.getOrder("ORD-001")

        // then
        assertEquals("ORD-001", response.orderId)
        assertEquals("OMS-ORD-12345", response.omsOrderId)
        assertEquals(OrderStatus.RECEIVED, response.status)
    }

    @Test
    fun `존재하지 않는 주문 조회 시 OrderNotFoundException 발생`() {
        // given
        whenever(orderRepository.findById("ORD-NOT-EXIST")).thenReturn(Optional.empty())

        // when & then
        assertThrows<OrderNotFoundException> {
            orderService.getOrder("ORD-NOT-EXIST")
        }
    }
}
