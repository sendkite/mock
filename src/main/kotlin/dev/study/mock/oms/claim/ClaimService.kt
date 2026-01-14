package dev.study.mock.oms.claim

import dev.study.mock.oms.order.OrderNotFoundException
import dev.study.mock.oms.order.OrderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ClaimService(
    private val claimRepository: ClaimRepository,
    private val orderRepository: OrderRepository,
    @Value("\${oms.claim.auto-approve-threshold:100000}") private val autoApproveThreshold: Long
) {
    @Transactional
    fun createClaim(request: CreateClaimRequest): CreateClaimResponse {
        // 1. 주문 존재 확인
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { OrderNotFoundException(request.orderId) }

        // 2. 클레임 생성
        val claimId = "CLM-${UUID.randomUUID().toString().take(8).uppercase()}"
        val claim = Claim(
            claimId = claimId,
            orderId = request.orderId,
            claimType = request.claimType,
            exchangeOptionId = request.exchangeOption?.optionId
        )

        // 3. 클레임 라인 추가
        request.claimLines.forEach { lineRequest ->
            val claimLine = ClaimLine(
                lineId = lineRequest.lineId,
                quantity = lineRequest.quantity,
                reason = lineRequest.reason
            )
            claimLine.claim = claim
            claim.claimLines.add(claimLine)
        }

        // 4. 조건부 자동 승인 (주문 금액이 threshold 이하면 자동 승인)
        if (order.totalAmount <= autoApproveThreshold) {
            claim.status = ClaimStatus.APPROVED
            claim.approvedAt = Instant.now()
        }

        val savedClaim = claimRepository.save(claim)

        return CreateClaimResponse(
            claimId = savedClaim.claimId,
            status = savedClaim.status,
            approvedAt = savedClaim.approvedAt
        )
    }

    @Transactional(readOnly = true)
    fun getClaim(claimId: String): ClaimResponse {
        val claim = claimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }

        return claim.toResponse()
    }

    @Transactional(readOnly = true)
    fun getClaimsByOrderId(orderId: String): List<ClaimResponse> {
        return claimRepository.findByOrderId(orderId).map { it.toResponse() }
    }

    @Transactional
    fun approveClaim(claimId: String): ClaimResponse {
        val claim = claimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }

        claim.status = ClaimStatus.APPROVED
        claim.approvedAt = Instant.now()

        return claimRepository.save(claim).toResponse()
    }

    @Transactional
    fun rejectClaim(claimId: String): ClaimResponse {
        val claim = claimRepository.findById(claimId)
            .orElseThrow { ClaimNotFoundException(claimId) }

        claim.status = ClaimStatus.REJECTED

        return claimRepository.save(claim).toResponse()
    }

    private fun Claim.toResponse() = ClaimResponse(
        claimId = claimId,
        orderId = orderId,
        claimType = claimType,
        status = status,
        claimLines = claimLines.map { line ->
            ClaimLineResponse(
                lineId = line.lineId,
                quantity = line.quantity,
                reason = line.reason
            )
        },
        exchangeOptionId = exchangeOptionId,
        createdAt = createdAt,
        approvedAt = approvedAt
    )
}

class ClaimNotFoundException(claimId: String) : RuntimeException("Claim not found: $claimId")
