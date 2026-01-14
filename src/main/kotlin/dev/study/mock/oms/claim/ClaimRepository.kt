package dev.study.mock.oms.claim

import org.springframework.data.jpa.repository.JpaRepository

interface ClaimRepository : JpaRepository<Claim, String> {
    fun findByOrderId(orderId: String): List<Claim>
}
