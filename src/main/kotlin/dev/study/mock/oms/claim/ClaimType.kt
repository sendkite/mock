package dev.study.mock.oms.claim

enum class ClaimType {
    RETURN,   // 반품
    EXCHANGE  // 교환
}

enum class ClaimStatus {
    PENDING_APPROVAL,  // 승인 대기
    APPROVED,          // 승인됨
    REJECTED,          // 거부됨
    COMPLETED          // 처리 완료
}
