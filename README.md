# User-Admin-Repo Repository

**Core Business Services for User Management and Monetization**
: *User Service, Admin Service, Payment Service*

이 레포지토리는 **Jigubangbang✈** 프로젝트의 핵심 비즈니스 로직인 사용자, 관리자, 결제 도메인을 담당하는 마이크로서비스들을 관리합니다.

## 📋 프로젝트 개요

**Jigubangbang**은 Spring Cloud 기반의 마이크로서비스 여행 커뮤니티 플랫폼입니다.
- **13개 마이크로서비스** 구성 (3개 인프라 + 9개 비즈니스 + 1개 프론트엔드)
- **실시간 통신, 결제 시스템, 게이미피케이션 요소** 통합
- **AWS 클라우드 네이티브** 아키텍처

## 🏛️ 시스템 아키텍처

![Domain Architecture](https://user-images.githubusercontent.com/87534343/285595333-69c19cf3-6353-4b32-a1a9-85569c31a15e.png)
*위 이미지는 전체 도메인 아키텍처입니다.*

## 🎯 서비스 구성

### 1. 👤 User Service
**사용자 도메인 총괄**
- **인증/인가**: JWT 기반 회원가입, 로그인, 소셜 로그인(Google, Naver, Kakao) 처리
- **프로필 관리**: 사용자 프로필 정보, 프로필 이미지, 여행 스타일 등 관리
- **상호작용**: 다른 사용자 팔로우/언팔로우, 사용자 검색 기능

```yaml
# 주요 기능
- JWT 토큰 생성 및 검증
- OAuth2 소셜 로그인 연동
- 사용자 정보 CRUD
- Feign Client를 통한 타 서비스와의 통신
```

### 2. 🛠️ Admin Service
**관리자 기능 및 시스템 운영**
- **관리자 대시보드**: 전체 사용자, 결제, 콘텐츠 현황 모니터링
- **사용자 관리**: 사용자 제재(활동 정지), 권한 변경 등
- **콘텐츠 관리**: 부적절한 게시물 및 댓글 숨김(블라인드) 처리

```yaml
# 주요 기능
- 관리자용 API 엔드포인트 제공
- 사용자 및 콘텐츠 상태 변경
- 통계 데이터 조회
```

### 3. 💳 Payment Service
**결제 및 프리미엄 구독 관리**
- **정기 구독 결제**: Portone(아임포트) 연동을 통한 프리미엄 서비스 정기 결제
- **상태 관리**: 사용자의 구독 상태(활성, 비활성, 해지) 실시간 관리
- **자동화**: 스케줄러를 통한 월간 자동 결제 및 만료 처리
- **웹훅 연동**: Portone 웹훅을 통한 결제 상태 동기화

```yaml
# 주요 기능
- Portone API 연동 (빌링키 발급, 정기결제)
- Spring Scheduler를 이용한 자동 결제
- 웹훅 수신 및 처리
```

## 🔐 핵심 기술적 도전과제

### 1. Portone 정기 결제 플로우
**문제점**: 사용자의 최초 결제와 2회차 이후의 자동 결제를 안정적으로 처리하고 상태를 동기화해야 함.
**해결방안**: `결제 준비` -> `최초 결제(빌링키 발급)` -> `웹훅 수신` -> `자동 결제 스케줄링`으로 이어지는 상태 관리 플로우 구축

```java
// PaymentService.java - processWebhook
// 1. 웹훅으로 받은 imp_uid, merchant_uid, status 검증
// 2. 결제 상태(PAID, CANCELLED)에 따라 DB 업데이트
// 3. 성공 시, User Service에 Feign Client로 알려 프리미엄 상태 변경
paymentMapper.updatePaymentStatus(payment);
if ("PAID".equals(status)) {
    userServiceClient.updateUserToPremium(payment.getUserId(), customerUid);
}
```

### 2. 스케줄러를 이용한 자동 결제
**문제점**: 매월 구독 만료일이 다가오는 사용자를 대상으로 정확한 시점에 자동 결제를 실행해야 함.
**해결방안**: Spring Scheduler(`@Scheduled`)를 사용하여 매일 특정 시각에 만료 예정인 사용자를 조회하고, Portone API를 통해 자동 결제 요청

```java
// PaymentScheduler.java
@Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시에 실행
public void processScheduledPayments() {
    // 1. 24시간 내에 구독 만료 예정인 활성 사용자 목록 조회
    List<User> users = paymentMapper.findUsersWithExpiringSubscriptions();
    // 2. 각 사용자에 대해 Portone 자동 결제 API 호출
    for (User user : users) {
        paymentService.processAutoPayment(user);
    }
}
```

## 🛠️ 기술 스택

**Core Framework**
- Java 17 + Spring Boot 3.4.6
- Spring Cloud 2024.0.1
- MyBatis, MySQL

**External APIs & Libraries**
- **Portone (아임포트)**: 결제 API 연동
- **Spring Security + JWT**: 사용자 인증/인가
- **Feign Client**: 마이크로서비스 간 통신
- **Lombok**: Boilerplate 코드 제거

**DevOps & Cloud**
- Docker Containerization
- AWS EKS (Kubernetes)
- Jenkins CI/CD Pipeline

## 🚀 배포 및 실행

### 로컬 개발 환경

**실행 순서 (중요!)**
1. **Config Server, Eureka Server** 실행 (from `infra-platform`)
2. **User, Admin, Payment Service** 실행
   ```bash
   # 각 서비스 디렉토리로 이동하여 아래 명령어 실행
   # 예: user-service
   cd user-service
   ./mvnw spring-boot:run
   ```
3. **모든 비즈니스 서비스** 실행
4. **API Gateway** 실행 (from `infra-platform`)

### AWS 클라우드 배포

**CI/CD 파이프라인**
1. **개발자**: GitHub에 코드 푸시
2. **Jenkins**: 자동 빌드 트리거 (`Jenkinsfile_user-admin-repo.groovy`)
3. **Docker**: 이미지 빌드 및 ECR 푸시
4. **EKS**: 쿠버네티스 자동 배포

```groovy
// Jenkinsfile_user-admin-repo.groovy (예시)
pipeline {
    agent any
    stages {
        stage('Build & Push User Service') {
            steps {
                dir('user-service') {
                    sh 'docker build -t $ECR_REGISTRY/user-service:latest .'
                    sh 'docker push $ECR_REGISTRY/user-service:latest'
                }
            }
        }
        // ... Admin, Payment 서비스도 동일하게 진행
        stage('Deploy to EKS') {
            steps {
                sh 'kubectl apply -f k8s/'
            }
        }
    }
}
```

## 📊 모니터링 및 관리

### 서비스 상태 확인
- **Eureka Dashboard**: `http://localhost:8761`
- **Service Health Check**:
  - `http://localhost:8081/actuator/health` (User Service)
  - `http://localhost:8082/actuator/health` (Admin Service)
  - `http://localhost:8086/actuator/health` (Payment Service)

### 주요 포트 정보
| 서비스 | 포트 | 설명 |
|---|---|---|
| User Service | 8081 | 사용자 인증 및 정보 관리 |
| Admin Service | 8082 | 관리자 기능 및 운영 |
| Payment Service | 8086 | 결제 및 구독 관리 |

## 🔧 주요 설정 파일

### application.properties (User Service)
```properties
# application.properties
spring.application.name=user-service
server.port=8081

# JWT Secret Key
jwt.secret= ...

# Google OAuth2 Client
spring.security.oauth2.client.registration.google.client-id= ...
spring.security.oauth2.client.registration.google.client-secret= ...
```

### application.properties (Payment Service)
```properties
# application.properties
spring.application.name=payment-service
server.port=8086

# Portone API Keys
portone.api-key= ...
portone.api-secret= ...

# Jackson Timezone Setting
spring.jackson.time-zone=UTC
```

## 📁 프로젝트 Repository 구조

### 🏗️ **Infrastructure & Core Services**
| Repository | 포함 서비스 | 담당자 |
|---|---|---|
| **config** | 설정 관리 | 팀장 이설영 |
| **infra-platform** | config-server, eureka-server, api-gateway | 팀장 이설영 |

### 💼 **Business Services**
| Repository | 포함 서비스 | 담당자 |
|---|---|---|
| **user-admin-repo** | user-service, admin-service, payment-service | 박나혜, 장준환 |
| **chat-service** | chat-service, notification | 이설영 |
| **qc-home-repo** | quest-service, com-service | 권민정 |
| **feed-mypage-repo** | feed-service, mypage-service | 남승현 |

---

**⚡ 빠른 시작 가이드**
```bash
# 1. 인프라 서비스 실행 (config, eureka)
# 2. user-admin-repo의 서비스들 실행
cd user-service && ./mvnw spring-boot:run &
cd ../admin-service && ./mvnw spring-boot:run &
cd ../payment-service && ./mvnw spring-boot:run &
```
