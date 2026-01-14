package dev.study.mock.oms.claim

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/claims")
class ClaimController(
    private val claimService: ClaimService
) {
    @PostMapping
    fun createClaim(@RequestBody request: CreateClaimRequest): ResponseEntity<CreateClaimResponse> {
        val response = claimService.createClaim(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{claimId}")
    fun getClaim(@PathVariable claimId: String): ResponseEntity<ClaimResponse> {
        val response = claimService.getClaim(claimId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getClaimsByOrderId(@RequestParam orderId: String): ResponseEntity<List<ClaimResponse>> {
        val response = claimService.getClaimsByOrderId(orderId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{claimId}/approve")
    fun approveClaim(@PathVariable claimId: String): ResponseEntity<ClaimResponse> {
        val response = claimService.approveClaim(claimId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{claimId}/reject")
    fun rejectClaim(@PathVariable claimId: String): ResponseEntity<ClaimResponse> {
        val response = claimService.rejectClaim(claimId)
        return ResponseEntity.ok(response)
    }
}
