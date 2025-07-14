# User, Admin, Payment Repository

이 레포지토리는 Jigubangbang 프로젝트의 사용자, 관리자, 결제 관련 마이크로서비스를 관리합니다.

## 개요

MSA(Microservice Architecture) 구조에 따라 각 도메인 별 기능이 분리된 서비스들로 구성되어 있습니다.

-   **User Service:** 사용자 인증, 회원 정보 관리, 프로필 등 사용자 관련 모든 기능을 담당합니다.
-   **Admin Service:** 관리자 대시보드, 사용자 관리, 콘텐츠 제어 등 관리자 관련 기능을 담당합니다.
-   **Payment Service:** 프리미엄 구독 결제, 결제 내역 조회, 환불 처리 등 결제 관련 모든 기능을 담당합니다.

## 주요 기술 스택

-   **Backend:** Java 17, Spring Boot 3, Spring Cloud, Mybatis, MySQL, Lombok, Feign Client
-   **Frontend:** React, Vite, Axios, Portone (아임포트)
-   **Infra:** Eureka, Spring Cloud Gateway, Spring Cloud Config

---

## Payment Service

### 주요 기능

-   **프리미엄 구독:** 사용자는 Portone 결제 모듈을 통해 프리미엄 서비스를 구독하고 정기 결제를 위한 빌링키를 발급받을 수 있습니다.
-   **구독 상태 관리:** 사용자의 프리미엄 구독 상태를 조회하고 관리합니다.
-   **구독 해지:** 사용자는 언제든지 프리미엄 구독을 해지할 수 있으며, 해지 시 빌링키가 삭제됩니다.
-   **결제 내역 조회:** 사용자는 자신의 전체 결제 내역을 조회할 수 있습니다.
-   **자동 결제 스케줄링:** 매월 정해진 시각에 만료 예정인 프리미엄 구독에 대해 자동 결제를 시도합니다.
-   **웹훅 연동:** Portone(아임포트) 웹훅을 통해 결제 및 환불 상태의 변경을 실시간으로 시스템에 반영합니다.
-   **관리자 기능:** 관리자는 전체 결제 내역 조회 및 특정 결제에 대한 환불 처리를 할 수 있습니다.

### API Endpoints

| Method | URL                                      | Role   | 설명                                       |
| :----- | :--------------------------------------- | :----- | :----------------------------------------- |
| `POST` | `/api/payment/premium/subscribe`         | USER   | 프리미엄 구독 시작 (첫 결제 및 빌링키 발급) |
| `GET`  | `/api/payment/premium/status`            | USER   | 구독 상태 조회                             |
| `DELETE`| `/api/payment/premium/cancel`           | USER   | 구독 해지 (빌링키 삭제)                    |
| `GET`  | `/api/payment/history`                   | USER   | 결제 내역 조회                             |
| `GET`  | `/api/payment/{paymentId}`               | USER   | 결제 상세 조회                             |
| `POST` | `/api/payment/webhook/iamport`           | SYSTEM | Iamport 웹훅 (결제 결과 수신)              |
| `POST` | `/api/payment/internal/auto-payment`     | SYSTEM | 자동 결제 실행                             |

### 데이터베이스 스키마

#### `payment`

결제 정보를 저장하는 테이블입니다.

| Column             | Type                                               | Description                        |
| :----------------- | :------------------------------------------------- | :--------------------------------- |
| `id`               | INT (PK, AI)                                       | 결제 고유 ID                       |
| `user_id`          | VARCHAR(255) (FK: user.user_id)                    | 결제한 사용자 ID                   |
| `imp_uid`          | VARCHAR(100) (UNIQUE)                              | Iamport 결제 고유 ID               |
| `merchant_uid`     | VARCHAR(100) (UNIQUE)                              | 주문 고유 ID (내부 트래킹용)       |
| `amount`           | INT                                                | 결제 금액                          |
| `pay_status`       | ENUM('PAID', 'CANCELLED', 'READY', 'CARD_UPDATED') | 결제 상태 ('READY'가 기본값)       |
| `paid_at`          | TIMESTAMP                                          | 결제 시각                          |
| `cancelled_at`     | TIMESTAMP                                          | 환불 처리 시각                     |
| `pay_method`       | VARCHAR(50)                                        | 결제 방법                          |
| `card_name`        | VARCHAR(50)                                        | 카드사                             |
| `card_number_masked`| VARCHAR(50)                                        | 마스킹된 카드 번호                 |

#### `premium_log`

프리미엄 구독 이력을 관리하는 테이블입니다.

| Column      | Type                  | Description                        |
| :---------- | :-------------------- | :--------------------------------- |
| `id`        | INT (PK, AI)          | 프리미엄 이력 고유 ID              |
| `user_id`   | VARCHAR(20) (FK: user.user_id) | 프리미엄 구독자 ID                 |
| `start_date`| TIMESTAMP             | 프리미엄 시작 시각                 |
| `end_date`  | TIMESTAMP             | 프리미엄 종료 시각 (해지 또는 만료) |
| `is_active` | BOOLEAN               | 현재 프리미엄 유지 여부            |

### 프론트엔드 구조 (`msa-front/src/pages/payment`)

-   **`Payment.jsx`**: Portone(아임포트) 결제 모듈을 렌더링하고 사용자의 결제를 처리하는 메인 페이지입니다.
-   **`PaymentSuccess.jsx`**: 결제가 성공적으로 완료되었을 때 리디렉션되는 페이지입니다. 결제 결과를 서버에 최종 확인하고 사용자에게 성공 메시지를 보여줍니다.
-   **`PaymentFail.jsx`**: 결제가 실패하거나 사용자가 취소했을 때 리디렉션되는 페이지입니다.
-   **`SubscriptionStatus.jsx`**: 사용자의 현재 프리미엄 구독 상태와 결제 내역을 보여주는 페이지입니다.

## 실행 방법

1.  **Config Server 실행:** 설정 정보를 가져오기 위해 Config Server를 먼저 실행해야 합니다.
2.  **Eureka Server 실행:** 서비스 디스커버리를 위해 Eureka Server를 실행합니다.
3.  **각 서비스 실행:** `user-service`, `admin-service`, `payment-service`를 각각 실행합니다.
    ```bash
    # 각 서비스 디렉토리로 이동하여 아래 명령어 실행
    ./mvnw spring-boot:run
    ```
4.  **프론트엔드 실행:** `msa-front` 디렉토리에서 아래 명령어를 실행합니다.
    ```bash
    npm install
    npm run dev
    ```
5.  **API Gateway 실행:** 라우팅을 위해 API Gateway를 실행합니다.
