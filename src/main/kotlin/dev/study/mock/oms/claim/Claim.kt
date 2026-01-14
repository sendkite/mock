package dev.study.mock.oms.claim

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "claims")
class Claim(
    @Id
    @Column(name = "claim_id")
    val claimId: String,

    val orderId: String,

    @Enumerated(EnumType.STRING)
    val claimType: ClaimType,

    @Enumerated(EnumType.STRING)
    var status: ClaimStatus = ClaimStatus.PENDING_APPROVAL,

    @OneToMany(mappedBy = "claim", cascade = [CascadeType.ALL], orphanRemoval = true)
    val claimLines: MutableList<ClaimLine> = mutableListOf(),

    val exchangeOptionId: String? = null,

    val createdAt: Instant = Instant.now(),

    var approvedAt: Instant? = null
)

@Entity
@Table(name = "claim_lines")
class ClaimLine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    var claim: Claim? = null,

    val lineId: String,
    val quantity: Int,
    val reason: String? = null
)
