# User-Admin-Repo Repository

**Core Business Services for User Management and Monetization**
: *User Service, Admin Service, Payment Service*

이 레포지토리는 **Jigubangbang✈** 프로젝트의 핵심 비즈니스 로직인 사용자, 관리자, 결제 도메인을 담당하는 마이크로서비스들을 관리합니다.


## 🎯 서비스 개요

### 👤 User Service
**사용자 도메인 총괄**
- **인증/인가**: JWT 기반 회원가입, 로그인, 소셜 로그인(Google, Naver, Kakao)
- **회원 정보 관리**: 회원 정보 수정(이름, 닉네임, 전화번호), 비밀번호 변경, 이메일 변경(인증코드 검증)
- **계정 상태 관리**: 정지/탈퇴 처리 및 복구, 회원 탈퇴 이력 기록
- **이메일 서비스 연동**: 인증코드 발송 및 확인, 임시 비밀번호 전송

```yaml
# 주요 기능
- 회원가입, 로그인 (JWT 기반), 소셜 로그인(Google, Naver, Kakao)
- Access Token 갱신 (Refresh Token 검증)
- 사용자 정보 조회 및 수정
- 비밀번호/이메일 변경 (이메일 인증 포함)
- 회원 탈퇴 처리 및 이력 저장
- 타 서비스와의 Feign 연동
```

### 🛠️ Admin Service
**관리자 기능 및 시스템 운영**
- **관리자 대시보드**: 전체 사용자, 결제, 콘텐츠 현황 모니터링 및 통계 데이터 제공
- **사용자 관리**: 사용자 활동 상태 변경, 회원 탈퇴 이력 관리
- **콘텐츠 관리**: 게시글, 댓글, 그룹 등 부적절한 콘텐츠 블라인드(숨김) 처리 및 해제
- **신고 관리**: 사용자 신고 내역 조회, 신고 승인 및 기각 처리
- **1:1 문의 관리**: 사용자 문의 내역 조회, 답변 등록 및 알림 전송

```yaml
# 주요 기능
- 관리자용 REST API 제공
- 사용자 상태 변경 및 권한 조정
- 게시글, 댓글, 그룹 등 블라인드 처리 및 해제
- 신고 내역 관리 (승인/기각/철회)
- 문의 답변 작성 및 알림 발송
- 통계 및 대시보드 데이터 제공
```

### 💳 Payment Service
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


## 🌐 API 엔드포인트

### 1. User Service API

#### AuthController (`/auth`)

| HTTP 메서드 | 경로                      | 설명                 | 인증 필요 여부 | 요청 DTO            | 응답 DTO            |
|-------------|---------------------------|----------------------|----------------|---------------------|---------------------|
| POST        | /auth/login               | 로그인               | X              | LoginRequestDto      | LoginResponseDto     |
| POST        | /auth/register            | 회원가입             | X              | RegisterRequestDto   | String              |
| GET         | /auth/check-id/{id}       | 아이디 중복 확인     | X              | -                   | Boolean             |
| GET         | /auth/check-email/{email} | 이메일 중복 확인     | X              | -                   | Boolean             |
| POST        | /auth/email/send          | 인증코드 이메일 발송 | X              | EmailDto            | String              |
| POST        | /auth/email/verify        | 인증코드 검증        | X              | EmailDto            | String              |
| POST        | /auth/{provider}          | 소셜 로그인          | X              | SocialRequestDto     | LoginResponseDto     |
| POST        | /auth/refresh-token       | AccessToken 재발급   | X              | Header(RefreshToken) | LoginResponseDto     |
| POST        | /auth/find-id             | 아이디 찾기          | X              | FindIdRequestDto     | ResponseEntity       |
| POST        | /auth/find-password       | 비밀번호 찾기        | X              | FindPwdRequestDto    | ResponseEntity       |

---

#### UserController (`/user`)

| HTTP 메서드 | 경로                          | 설명                   | 인증 필요 여부 | 요청 DTO             | 응답 DTO             |
|-------------|-------------------------------|------------------------|----------------|----------------------|----------------------|
| GET         | /user/me                      | 내 정보 조회           | O              | -                    | UserDto              |
| PUT         | /user/me                      | 내 정보 수정           | O              | UpdateUserDto        | String               |
| PUT         | /user/password                | 비밀번호 변경          | O              | ChangePwdDto         | String               |
| POST        | /user/email/change-request    | 이메일 변경 요청       | O              | String (email)       | String               |
| PUT         | /user/email/change-confirm    | 이메일 변경 확인       | O              | ChangeEmailDto       | String               |
| DELETE      | /user/me                      | 회원 탈퇴              | O              | WithdrawalRequestDto  | 204 No Content       |

---

#### UserResponseController (`/user/internal`)

| HTTP 메서드 | 경로                              | 설명                      | 인증 필요 여부 | 요청 DTO                      | 응답 DTO              |
|-------------|-----------------------------------|---------------------------|----------------|-------------------------------|-----------------------|
| GET         | /user/internal/{userId}            | 내부 서비스 사용자 정보 조회 | X (내부통신)    | -                             | UserResponseDto        |
| PUT         | /user/internal/premium/{userId}   | 내부 서비스 프리미엄 상태 변경 | X (내부통신)    | UserPremiumUpdateRequestDto   | 200 OK                 |

---

#### InquiryController (`/user/inquiry`)

| HTTP 메서드 | 경로                | 설명                   | 인증 필요 여부 | 요청 DTO           | 응답 DTO             |
|-------------|---------------------|------------------------|----------------|--------------------|----------------------|
| POST        | /user/inquiry       | 1:1 문의 생성 (멀티파트) | O              | CreateInquiryDto   | Map<String, Integer>  |
| GET         | /user/inquiry       | 내 문의 리스트 조회     | O              | -                  | List<InquiryDto>      |
| GET         | /user/inquiry/{id}  | 문의 상세 조회          | O              | -                  | InquiryDto            |
| PUT         | /user/inquiry/{id}  | 문의 수정 (멀티파트)    | O              | CreateInquiryDto   | String                |
| DELETE      | /user/inquiry/{id}  | 문의 삭제               | O              | -                  | 204 No Content        |

---

#### ReportController (`/user/reports`)

| HTTP 메서드 | 경로                | 설명                   | 인증 필요 여부 | 요청 DTO           | 응답 DTO             |
|-------------|---------------------|------------------------|----------------|--------------------|----------------------|
| POST        | /user/reports       | 신고 등록               | O              | CreateReportDto    | String                |

---

### 2. Admin Service API

#### AdminController (`/admin`)

| HTTP 메서드 | 경로                            | 설명                      | 인증 필요 여부   | 요청 DTO           | 응답 DTO             |
|-------------|---------------------------------|---------------------------|------------------|--------------------|----------------------|
| GET         | /admin/users                    | 사용자 목록 조회           | O (관리자 권한)  | -                  | List<AdminUserDto>    |
| PUT         | /admin/users/{userId}/status    | 사용자 상태 변경 (정지 등) | O (관리자 권한)  | ChangeStatusDto    | String               |
| GET         | /admin/posts                    | 게시글 목록 조회           | O (관리자 권한)  | 필터 파라미터       | List<AdminPostDto>    |
| PUT         | /admin/posts/{postId}/blind     | 게시글 블라인드 처리       | O (관리자 권한)  | -                  | String               |
| PUT         | /admin/posts/{postId}/unblind   | 게시글 블라인드 해제       | O (관리자 권한)  | -                  | String               |
| GET         | /admin/comments                 | 댓글 목록 조회             | O (관리자 권한)  | 필터 파라미터       | List<AdminCommentDto> |
| PUT         | /admin/comments/{commentId}/blind   | 댓글 블라인드 처리    | O (관리자 권한)  | -                  | String               |
| PUT         | /admin/comments/{commentId}/unblind | 댓글 블라인드 해제    | O (관리자 권한)  | -                  | String               |
| GET         | /admin/groups                   | 그룹 목록 조회             | O (관리자 권한)  | 필터 파라미터       | List<AdminGroupDto>   |
| PUT         | /admin/groups/{groupId}/blind   | 그룹 블라인드 처리         | O (관리자 권한)  | -                  | String               |
| PUT         | /admin/groups/{groupId}/unblind | 그룹 블라인드 해제         | O (관리자 권한)  | -                  | String               |
| GET         | /admin/reports                  | 신고 목록 조회             | O (관리자 권한)  | -                  | List<AdminReportDto>  |
| POST        | /admin/reports/{reportId}/blind | 신고 승인 및 블라인드 처리 | O (관리자 권한)  | -                  | String               |
| POST        | /admin/reports/{reportId}/keep  | 신고 기각 처리             | O (관리자 권한)  | -                  | String               |
| POST        | /admin/reports/{reportId}/cancel| 신고 승인 철회            | O (관리자 권한)  | -                  | String               |
| GET         | /admin/inquiries                | 1:1 문의 목록 조회         | O (관리자 권한)  | -                  | List<AdminInquiryDto> |
| GET         | /admin/inquiries/{id}           | 문의 상세 조회             | O (관리자 권한)  | -                  | AdminInquiryDto       |
| PUT         | /admin/inquiries/{id}/reply     | 문의 답변 등록             | O (관리자 권한)  | 답변 내용 DTO       | String               |

---

### 3. Payment Service API

(필요시 Payment Service API도 추가하세요)


## 💡 주요 구현 사항

### 1. User Service
#### Spring Security 기반 JWT 인증 및 인가
**문제점**: 마이크로서비스 환경에서 분산된 사용자 인증 및 권한 부여를 효율적이고 안전하게 처리해야 함.<br>
**해결방안**: Spring Security와 JWT(JSON Web Token)를 활용하여 Stateless한 인증 시스템 구축. API Gateway에서 1차 인증·인가를 수행하고, User Service에서 세부 권한 검증과 토큰 발급·갱신을 담당.

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**",  "/public/**", "/", "/health-check", "/actuator/**", "/user/internal/**").permitAll()
            .anyRequest().authenticated()  
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

#### Refresh Token을 이용한 Access Token 갱신
**문제점**: Access Token의 짧은 유효 시간으로 인한 잦은 재로그인과 보안 취약점 존재.<br>
**해결방안**: 긴 유효 시간의 Refresh Token을 도입하여 Access Token 만료 시 사용자 재로그인 없이 토큰 갱신 가능. Refresh Token 탈취 방지를 위해 재사용 제한 및 강제 만료 처리 로직 구현.

```java
// AuthService.java
public LoginResponseDto refreshAccessToken(String tokenHeader) {
    String token = tokenHeader.replace("Bearer ", "");
    if (!jwtTokenProvider.validateToken(token) || !"refresh".equals(jwtTokenProvider.getTokenType(token))) {
        throw new IllegalArgumentException("유효하지 않은 RefreshToken입니다.");
    }
    String userId = jwtTokenProvider.getUserIdFromToken(token);
    UserDto user = userMapper.findUserById(userId);
    if (user == null) {
        throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
    }
    String newAccessToken = jwtTokenProvider.generateAccessToken(user);
    return LoginResponseDto.of(newAccessToken, token, user); 
}
```

#### 소셜 로그인 (OAuth2) 연동
**문제점**: 다양한 소셜 플랫폼(Google, Naver, Kakao)을 통한 간편 로그인 기능을 제공해야 함.<br>
**해결방안**: Spring Security OAuth2 Client를 활용하여 각 소셜 플랫폼의 인증 흐름을 통합 및 사용자 정보를 서비스에 맞게 매핑, JWT 토큰 발급으로 일관된 인증 서비스 제공.

```java
// AuthController.java
@PostMapping("/{provider}")
public ResponseEntity<?> socialLogin(
        @PathVariable String provider,
        @RequestBody SocialRequestDto request) {
    try {
        LoginResponseDto response = authService.socialLogin(request.getCode(), provider);
        return ResponseEntity.ok(response);
    } catch (UserStatusException e) {
        return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
    }
}
```
---

### 2. Amdin Service
#### 콘텐츠 블라인드 처리 및 사용자 알림 전송
**문제점** 부적절한 게시글, 댓글, 그룹 콘텐츠에 대한 효율적인 관리 및 사용자 경고 전달이 필요함.<br>
**해결방안** 관리자가 콘텐츠를 블라인드 처리할 경우, 해당 유저의 블라인드 카운트를 증가시키고 알림 서비스(FeignClient)를 통해 자동으로 알림 전송. 신고 승인 처리 시에도 동일한 로직 자동 적용.

```java
// AdminReportService.java
blindCountMapper.increaseBlindCount(report.getTargetUserId());
BlindNotificationRequestDto notification = BlindNotificationRequestDto.builder()
    .userId(report.getTargetUserId())
    .message("콘텐츠가 블라인드 처리되었습니다.\n자세한 사항은 1:1 문의를 통해 확인해 주세요.")
    .relatedUrl("/user/inquiry")
    .senderId(null)
    .build();
notificationServiceClient.createBlindNotification(notification);
```

#### 문의 답변 등록 및 알림 전송
**문제점** 사용자의 문의에 대한 답변이 등록되었을 때 이를 실시간으로 사용자에게 전달할 방법이 없음.<br>
**해결방안** 관리자가 답변 등록 시, 알림 서비스(FeignClient)를 통해 자동으로 알림 전송. 첨부파일이 JSON 문자열로 저장된 경우에도 파싱 처리하여 상세 조회 시 제공.

```java
// AdminInquiryService.java
public void replyToInquiry(int inquiryId, String adminId, String reply) {
    int updated = adminInquiryMapper.updateInquiryReply(inquiryId, adminId, reply);
    if (updated == 0) {
        throw new IllegalArgumentException("문의 답변 등록에 실패했습니다.");
    }
    AdminInquiryDto inquiry = adminInquiryMapper.getInquiryById(inquiryId);
    InquiryNotificationRequestDto notification = InquiryNotificationRequestDto.builder()
        .userId(inquiry.getUserId())
        .message("문의하신 내용에 대한 답변이 등록되었습니다.")
        .relatedUrl("/user/inquiry/" + inquiryId)
        .senderId(adminId)
        .build();
    notificationServiceClient.createInquiryAnsweredNotification(notification);
}
```
---

### 3.Payment Service
#### Portone 정기 결제 플로우
**문제점**: 사용자의 최초 결제와 2회차 이후의 자동 결제를 안정적으로 처리하고 상태를 동기화해야 함.<br>
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

#### 스케줄러를 이용한 자동 결제
**문제점**: 매월 구독 만료일이 다가오는 사용자를 대상으로 정확한 시점에 자동 결제를 실행해야 함.<br>
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
---


## 🛠️ 기술 스택

**Core Framework**
- Java 17 + Spring Boot 3.4.6
- Spring Cloud 2024.0.1
- MyBatis, MySQL

**External APIs & Libraries**
- **Spring Security + JWT**: 사용자 인증/인가
- **Portone (아임포트)**: 결제 API 연동
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


## 🔧 주요 설정 정보

### application.properties (User Service)
```properties
# application.properties
spring.application.name=user-service
server.port=8081

# JWT Secret Key
jwt.secret= ...
jwt.access-token-validity= ...
jwt.refresh-token-validity= ...

# OAuth2 Client
oauth.kakao.client-id= ...
oauth.kakao.redirect-uri=http://localhost:5173/oauth/kakao/callback

oauth.naver.client-id= ...
oauth.naver.client-secret= ...
oauth.naver.redirect-uri=http://localhost:5173/oauth/naver/callback

oauth.google.client-id= ...
oauth.google.client-secret= ...
oauth.google.redirect-uri=http://localhost:5173/oauth/google/callback

# Gmail SMTP 
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username= ...
spring.mail.password= ...
spring.mail.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

### application.properties (Admin Service)
```properties
# application.properties
spring.application.name=admin-service
server.port=8082
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
