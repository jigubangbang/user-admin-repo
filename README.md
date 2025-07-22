# User-Admin-Repo Repository

**Core Business Services for User Management and Monetization**
: *User Service, Admin Service, Payment Service*

이 레포지토리는 **Jigubangbang✈** 프로젝트의 핵심 비즈니스 로직인 사용자, 관리자, 결제 도메인을 담당하는 마이크로서비스들을 관리합니다.

## 🎯 서비스 구성

### 1. 👤 User Service
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

### 1. Spring Security 기반 JWT 인증 및 인가
**문제점**: 마이크로서비스 환경에서 사용자 인증 및 권한 부여를 효율적이고 안전하게 처리해야 함.
**해결방안**: Spring Security와 JWT(JSON Web Token)를 활용하여 Stateless한 인증 시스템 구축. API Gateway에서 1차 인증/인가를 수행하고, User Service에서 상세 권한 검증 및 토큰 발급/갱신을 담당.

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

### 2. Refresh Token을 이용한 Access Token 갱신
**문제점**: Access Token의 짧은 유효 기간으로 인한 잦은 재로그인 필요성과 보안 취약점.
**해결방안**: Refresh Token을 도입하여 Access Token 만료 시 사용자 재로그인 없이 새로운 Access Token을 발급. Refresh Token은 긴 유효 기간을 가지며, 탈취 시 재사용 방지 및 강제 만료 처리 로직 구현.

```java
// AuthService.java - refreshAccessToken method
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

### 3. 소셜 로그인 (OAuth2) 연동
**문제점**: 다양한 소셜 플랫폼(Google, Naver, Kakao)을 통한 간편 로그인 기능을 제공해야 함.
**해결방안**: Spring Security OAuth2 Client를 활용하여 각 소셜 플랫폼의 인증 흐름을 통합하고, 사용자 정보를 서비스에 맞게 매핑하여 JWT 토큰 발급.

```java
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

### 4. Portone 정기 결제 플로우
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

### 5. 스케줄러를 이용한 자동 결제
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
