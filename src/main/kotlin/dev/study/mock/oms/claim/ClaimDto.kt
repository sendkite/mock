package dev.study.mock.oms.claim

import java.time.Instant

// Request DTOs
data class CreateClaimRequest(
    val orderId: String,
    val claimType: ClaimType,
    val claimLines: List<ClaimLineRequest>,
    val exchangeOption: ExchangeOptionRequest? = null
)

data class ClaimLineRequest(
    val lineId: String,
    val quantity: Int,
    val reason: String? = null
)

data class ExchangeOptionRequest(
    val optionId: String
)

// Response DTOs
data class CreateClaimResponse(
    val claimId: String,
    val status: ClaimStatus,
    val approvedAt: Instant?
)

data class ClaimResponse(
    val claimId: String,
    val orderId: String,
    val claimType: ClaimType,
    val status: ClaimStatus,
    val claimLines: List<ClaimLineResponse>,
    val exchangeOptionId: String?,
    val createdAt: Instant,
    val approvedAt: Instant?
)

data class ClaimLineResponse(
    val lineId: String,
    val quantity: Int,
    val reason: String?
)
