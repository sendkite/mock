# OMS Mock Server 스펙

이커머스 서비스에서 OMS(Order Management System) 역할을 수행하는 Mock 서버.
자사몰 서버 테스트 용도로 사용됩니다.

## 시스템 개요

### 아키텍처
- **통신 방식**: 동기 REST API
- **인증**: Basic Auth (username:password)
- **API 버전**: URL path 기반 버전 관리 (`/api/v1/...`)
- **응답 형식**: HTTP 상태코드로 성공/실패 구분, body는 데이터만 반환
- **데이터 저장**: H2 임베디드 DB

### Mock 서버 역할
1. **서버 역할**: 자사몰 → OMS 요청에 대한 응답 (주문 등록, 클레임 등록 등)
2. **클라이언트 역할**: OMS → 자사몰 API 호출 (재고 전송, 배송 상태 전송)

---

## 주요 기능

### 1. 주문 수집

#### 흐름
- 자사몰 → OMS (Push 방식)
- 주문 발생 시 자사몰이 OMS에 주문 등록 API 호출

#### 주문 구조
- 단일 주문에 다수 상품(orderLines) 포함 가능
- 상품 식별자: `productId` + `optionId` 복합키

#### 주문 상태 플로우
```
접수(RECEIVED) → 결제확인(PAYMENT_CONFIRMED) → 상품준비(PREPARING)
→ 포장(PACKING) → 출고(SHIPPED) → 배송중(IN_DELIVERY) → 완료(DELIVERED)
```

#### 중복 주문 처리
- 동일 주문번호로 재전송 시 `409 Conflict` 반환

#### 배송지 정보 (상세)
- 수령인명
- 연락처 (휴대폰, 전화번호)
- 우편번호
- 주소 (기본주소 + 상세주소)
- 배송 메모
- 공동현관 출입코드 (선택)

### 2. 재고 전송

#### 흐름
- OMS → 자사몰 (Push 방식)
- 재고 변경 시 실시간 이벤트로 자사몰 API 호출

#### 재고 유형
- 가용재고(available stock)만 전송

#### 전송 데이터
- productId, optionId
- availableQuantity

### 3. 배송 트래킹

#### 흐름
- OMS → 자사몰 (Push 방식)
- 배송 상태 변경 시 실시간 이벤트로 자사몰 API 호출

#### 분할 배송
- 하나의 주문이 여러 송장으로 분리 배송 가능
- 주문-송장 1:N 관계

#### 배송 상태 (5단계 이상)
```
집화(PICKED_UP) → 간선터미널(IN_TRANSIT) → 배송중(OUT_FOR_DELIVERY)
→ 배송완료(DELIVERED) → 수령확인(CONFIRMED)
```

#### 전송 데이터
- orderId
- shipmentId (송장번호)
- carrierCode (택배사 코드)
- trackingNumber
- status
- statusUpdatedAt

### 4. 클레임 등록 (교환/반품)

#### 흐름
- 자사몰 → OMS (Push 방식)

#### 클레임 유형
- **반품(RETURN)**: 환불 처리
- **교환(EXCHANGE)**: 동일 상품으로만 교환 가능

#### 부분 클레임
- 주문 내 특정 상품/수량만 선택적 클레임 가능

#### 클레임 승인
- 조건부 자동 승인 (금액/기간 등 조건 충족 시 자동, 아니면 수동 승인)

#### 클레임 사유
- 선택적 (reason 필드 optional)

---

## API 설계

### 자사몰 → OMS (서버 역할)

#### 주문 등록
```
POST /api/v1/orders
Authorization: Basic {credentials}
Content-Type: application/json

Request:
{
  "orderId": "ORD-2024-001",
  "orderedAt": "2024-01-15T10:30:00Z",
  "orderLines": [
    {
      "lineId": "LINE-001",
      "productId": "PROD-001",
      "optionId": "OPT-001",
      "productName": "상품명",
      "optionName": "옵션명",
      "quantity": 2,
      "unitPrice": 15000,
      "totalPrice": 30000
    }
  ],
  "shipping": {
    "recipientName": "홍길동",
    "phoneNumber": "010-1234-5678",
    "zipCode": "12345",
    "address": "서울시 강남구 테헤란로 123",
    "addressDetail": "456호",
    "memo": "부재시 경비실에 맡겨주세요",
    "entranceCode": "1234#"
  },
  "totalAmount": 30000
}

Response: 201 Created
{
  "orderId": "ORD-2024-001",
  "omsOrderId": "OMS-ORD-00001",
  "status": "RECEIVED",
  "receivedAt": "2024-01-15T10:30:05Z"
}

Error: 409 Conflict (중복 주문)
{
  "error": "DUPLICATE_ORDER",
  "message": "Order already exists"
}
```

#### 클레임 등록
```
POST /api/v1/claims
Authorization: Basic {credentials}

Request:
{
  "orderId": "ORD-2024-001",
  "claimType": "RETURN",  // RETURN | EXCHANGE
  "claimLines": [
    {
      "lineId": "LINE-001",
      "quantity": 1,
      "reason": "단순변심"  // optional
    }
  ],
  "exchangeOption": {  // 교환 시에만
    "optionId": "OPT-002"
  }
}

Response: 201 Created
{
  "claimId": "CLM-2024-001",
  "status": "APPROVED",  // APPROVED | PENDING_APPROVAL
  "approvedAt": "2024-01-15T11:00:00Z"
}
```

### OMS → 자사몰 (클라이언트 역할)

#### 재고 전송
```
POST {자사몰_BASE_URL}/api/v1/stocks/sync
Authorization: Basic {credentials}

Request:
{
  "stocks": [
    {
      "productId": "PROD-001",
      "optionId": "OPT-001",
      "availableQuantity": 100
    }
  ],
  "syncedAt": "2024-01-15T12:00:00Z"
}

Expected Response: 200 OK
```

#### 배송 상태 전송
```
POST {자사몰_BASE_URL}/api/v1/shipments/status
Authorization: Basic {credentials}

Request:
{
  "orderId": "ORD-2024-001",
  "shipments": [
    {
      "shipmentId": "SHP-001",
      "carrierCode": "CJ",
      "trackingNumber": "1234567890",
      "status": "OUT_FOR_DELIVERY",
      "statusUpdatedAt": "2024-01-16T09:00:00Z",
      "orderLines": ["LINE-001"]
    }
  ]
}

Expected Response: 200 OK
```

---

## 에러 시나리오 (테스트용)

Mock 서버에서 시뮬레이션 가능한 에러 케이스:

### 1. 재고 부족
```
POST /api/v1/orders
→ 409 Conflict
{
  "error": "INSUFFICIENT_STOCK",
  "message": "Not enough stock for PROD-001/OPT-001",
  "details": {
    "productId": "PROD-001",
    "optionId": "OPT-001",
    "requested": 5,
    "available": 2
  }
}
```

### 2. 결제 실패
```
POST /api/v1/orders
→ 402 Payment Required
{
  "error": "PAYMENT_FAILED",
  "message": "Payment verification failed"
}
```

### 3. 타임아웃/지연
- WireMock `withFixedDelay()` 사용하여 응답 지연 시뮬레이션
- 자사몰의 타임아웃 처리 로직 테스트용

---

## WireMock 학습 포인트

이 프로젝트에서 연습할 WireMock 기능:

### 1. Stateful Mock (Scenario)
- 주문 상태 변경: RECEIVED → PAYMENT_CONFIRMED → PREPARING → ...
- 첫 번째 조회와 두 번째 조회 시 다른 상태 반환

### 2. Request Matching
- JSON body의 특정 필드 매칭 (`matchingJsonPath`)
- 금액/수량 조건에 따른 다른 응답
- Header 검증 (Authorization)

### 3. Response Templating
- 요청의 orderId를 응답에 동적으로 포함
- 현재 시간을 응답에 포함
- 요청 데이터 기반 동적 응답 생성

### 4. Verification
- API 호출 횟수 검증
- 요청 body 내용 검증

### 5. Priority
- 특정 조건(고액 주문 등)에 대한 우선 처리

---

## 데이터 모델

### Order
| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | String | 자사몰 주문번호 (PK) |
| omsOrderId | String | OMS 내부 주문번호 |
| status | Enum | 주문 상태 |
| orderLines | List | 주문 상품 목록 |
| shipping | Object | 배송지 정보 |
| totalAmount | Long | 총 주문금액 |
| orderedAt | DateTime | 주문 일시 |
| receivedAt | DateTime | OMS 접수 일시 |

### OrderLine
| 필드 | 타입 | 설명 |
|------|------|------|
| lineId | String | 주문라인 ID |
| productId | String | 상품 ID |
| optionId | String | 옵션 ID |
| productName | String | 상품명 |
| optionName | String | 옵션명 |
| quantity | Int | 수량 |
| unitPrice | Long | 단가 |
| totalPrice | Long | 합계 |

### Shipment
| 필드 | 타입 | 설명 |
|------|------|------|
| shipmentId | String | 송장 ID |
| orderId | String | 주문번호 (FK) |
| carrierCode | String | 택배사 코드 |
| trackingNumber | String | 운송장 번호 |
| status | Enum | 배송 상태 |
| orderLineIds | List | 포함된 주문라인 |

### Claim
| 필드 | 타입 | 설명 |
|------|------|------|
| claimId | String | 클레임 ID |
| orderId | String | 주문번호 (FK) |
| claimType | Enum | RETURN / EXCHANGE |
| status | Enum | 클레임 상태 |
| claimLines | List | 클레임 대상 상품 |
| reason | String | 클레임 사유 (선택) |

### Stock
| 필드 | 타입 | 설명 |
|------|------|------|
| productId | String | 상품 ID (복합 PK) |
| optionId | String | 옵션 ID (복합 PK) |
| availableQuantity | Int | 가용 재고 수량 |
