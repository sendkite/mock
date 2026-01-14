# OMS Mock Server

WireMock 학습을 위한 OMS(Order Management System) Mock 서버입니다.
이커머스 자사몰 서버 테스트 용도로 사용됩니다.

## 기술 스택

- Kotlin 2.2 + Java 24
- Spring Boot 4.0.1
- Spring Data JPA + H2 Database
- WireMock 3.10

## 빠른 시작

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

서버 실행 후: http://localhost:8080

## API 문서

### 인증

모든 API는 Basic Auth가 필요합니다.
- Username: `oms-user`
- Password: `oms-pass`

```bash
# 예시
curl -u oms-user:oms-pass http://localhost:8080/api/v1/orders
```

### 주문 API

#### 주문 등록
```http
POST /api/v1/orders
Content-Type: application/json

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
    "memo": "부재시 경비실에 맡겨주세요"
  },
  "totalAmount": 30000
}
```

**응답 (201 Created)**
```json
{
  "orderId": "ORD-2024-001",
  "omsOrderId": "OMS-ORD-A1B2C3D4",
  "status": "RECEIVED",
  "receivedAt": "2024-01-15T10:30:05Z"
}
```

**에러 응답**
- `409 Conflict` - 중복 주문 (`DUPLICATE_ORDER`)
- `409 Conflict` - 재고 부족 (`INSUFFICIENT_STOCK`)

#### 주문 조회
```http
GET /api/v1/orders/{orderId}
```

### 클레임 API

#### 클레임 등록 (반품/교환)
```http
POST /api/v1/claims
Content-Type: application/json

{
  "orderId": "ORD-2024-001",
  "claimType": "RETURN",
  "claimLines": [
    {
      "lineId": "LINE-001",
      "quantity": 1,
      "reason": "단순변심"
    }
  ]
}
```

**응답 (201 Created)**
```json
{
  "claimId": "CLM-A1B2C3D4",
  "status": "APPROVED",
  "approvedAt": "2024-01-15T11:00:00Z"
}
```

### 재고 API

#### 재고 수정
```http
PUT /api/v1/stocks/{productId}/{optionId}
Content-Type: application/json

{
  "availableQuantity": 100
}
```

### 배송 API

#### 배송 생성
```http
POST /api/v1/shipments
Content-Type: application/json

{
  "orderId": "ORD-2024-001",
  "carrierCode": "CJ",
  "trackingNumber": "1234567890",
  "orderLineIds": ["LINE-001"]
}
```

#### 배송 상태 변경
```http
PATCH /api/v1/shipments/{shipmentId}/status
Content-Type: application/json

{
  "status": "OUT_FOR_DELIVERY"
}
```

## 주문 상태 플로우

```
RECEIVED → PAYMENT_CONFIRMED → PREPARING → PACKING → SHIPPED → IN_DELIVERY → DELIVERED
```

## 배송 상태

```
PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED → CONFIRMED
```

## H2 Console

http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:mem:omsdb`
- Username: `sa`
- Password: (빈 값)

## WireMock 학습

이 프로젝트는 WireMock 학습을 목적으로 합니다. 테스트 코드에서 다음 기능들을 연습할 수 있습니다:

### 테스트 파일 위치
- `src/test/kotlin/dev/study/mock/oms/client/MallApiClientWireMockTest.kt`
- `src/test/kotlin/dev/study/mock/payment/PaymentClientWireMockTest.kt`
- `src/test/kotlin/dev/study/mock/payment/PaymentClientAdvancedWireMockTest.kt`

### 학습 포인트

| 기능 | 설명 | 예시 |
|------|------|------|
| Stubbing | 요청-응답 매핑 | `stubFor(post(url).willReturn(aResponse()))` |
| Request Matching | 요청 조건 매칭 | `matchingJsonPath()`, `withHeader()` |
| Scenario | 상태 기반 응답 | `inScenario()`, `whenScenarioStateIs()` |
| Delay | 응답 지연 | `withFixedDelay(5000)` |
| Verification | 호출 검증 | `verify(1, postRequestedFor(url))` |
| Priority | 우선순위 | `atPriority(1)` |

### 예시: Scenario 사용
```kotlin
// 첫 번째 호출: PENDING 반환
wireMockServer.stubFor(
    get(urlEqualTo("/api/payments/txn_123"))
        .inScenario("Payment Status")
        .whenScenarioStateIs(STARTED)
        .willReturn(aResponse().withBody("""{"status": "PENDING"}"""))
        .willSetStateTo("Completed")
)

// 두 번째 호출부터: SUCCESS 반환
wireMockServer.stubFor(
    get(urlEqualTo("/api/payments/txn_123"))
        .inScenario("Payment Status")
        .whenScenarioStateIs("Completed")
        .willReturn(aResponse().withBody("""{"status": "SUCCESS"}"""))
)
```

## 프로젝트 구조

```
src/main/kotlin/dev/study/mock/
├── MockApplication.kt
├── oms/
│   ├── order/       # 주문 도메인
│   ├── claim/       # 클레임 도메인
│   ├── stock/       # 재고 도메인
│   ├── shipment/    # 배송 도메인
│   ├── client/      # 외부 API 클라이언트
│   └── config/      # 설정 (인증, 예외처리)
└── payment/         # 결제 API 클라이언트 (WireMock 학습용)
```
