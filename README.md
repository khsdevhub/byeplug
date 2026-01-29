# ByePlug  
## 위치 기반 스마트 멀티탭 제어 안드로이드 애플리케이션

**ByePlug**은 oneM2M 표준 기반 IoT 플랫폼을 활용하여  
스마트 멀티탭을 원격으로 제어하고,  
사용자의 위치(GPS)를 인식하여 **집을 벗어났을 경우 자동으로 전원을 차단**하는 안드로이드 애플리케이션이다.

본 프로젝트는 실제 사용 환경을 고려하여  
**명령 신뢰성, 상태 일관성, 유지보수성**을 중심으로 설계되었다.

---

## 1. 주요 기능

### 1.1 스마트 멀티탭 원격 제어
- 멀티탭 1대당 **4구 고정 콘센트 구조**
- 콘센트별 개별 ON / OFF 제어
- 사용자 조작 시 서버로 즉시 제어 명령 전송

### 1.2 oneM2M 기반 통신 구조
- IoTcoss oneM2M 플랫폼 사용
- `AE / CNT / CIN` 리소스 구조 준수
- Retrofit 기반 REST 통신
- JSON 형식의 제어(Control) 및 상태(Status) 데이터 처리

### 1.3 상태 동기화 (STATUS Polling)
- 아두이노 디바이스가 업로드한 STATUS를 주기적으로 조회
- 사용자 조작 직후에는 **고속 폴링**, 평상시에는 **저속 폴링** 적용
- 서버 부하와 사용자 체감 반응성을 동시에 고려한 폴링 정책

### 1.4 GPS 기반 자동 전원 차단 (Geo-fencing)
- 사용자가 현재 위치를 “집(Home)”으로 등록
- 설정된 반경을 벗어날 경우(EXIT 이벤트):
  - 선택된 콘센트만 자동으로 OFF
  - 앱이 종료된 상태에서도 동작
- BroadcastReceiver + WorkManager 기반 백그라운드 처리

### 1.5 콘센트별 자동 차단 설정
- 각 콘센트는 **길게 누르기(Long Press)** 동작으로 자동 차단 대상 설정
- 비트마스크(mask) 방식으로 설정 상태 저장
- 추가 버튼 없이 직관적인 UX 제공

---

## 2. 설계 핵심 원칙

### 2.1 전체 상태 스냅샷 기반 제어

본 프로젝트에서는 개별 콘센트 제어 시에도  
항상 **4구 전체 상태를 하나의 CONTROL 명령으로 전송**한다.

#### 적용 이유
- oneM2M `la(latest)` 조회 특성상  
  다수의 부분 명령은 처리 순서 및 누락 문제가 발생할 수 있음
- 전체 상태 스냅샷 방식은 다음과 같은 장점을 가짐:
  - 명령 유실 방지
  - 아두이노 제어 로직 단순화
  - 상태 기반(idempotent) 제어 가능
  - 유지보수성 향상

---

## 3. oneM2M 데이터 포맷

### 3.1 CONTROL (App → Server → Arduino)

```json
{
  "cmd": "SET_OUTLETS",
  "outlets": {
    "1": true,
    "2": false,
    "3": true,
    "4": false
  },
  "ts": 1730000000000
}
```
### 3.2 STATUS (Arduino → Server → App)
```json
{
  "outlets": {
    "1": true,
    "2": false,
    "3": true,
    "4": false
  },
  "ts": 1730000000000
}

```
---
## 4. 디바이스 구성 및 동작
### 4.1 하드웨어 구성

MCU: Arduino R4 WiFi

릴레이 모듈: 4채널 5V 릴레이 모듈

전원

Arduino: USB 5V

릴레이: 외부 5V 어댑터 (GND 공통)

부하

220V 가정용 콘센트 4구

활선만 릴레이로 차단

### 4.2 릴레이 제어 방식

콘센트별 독립 릴레이 제어

기본 상태: OFF (Normally Open 사용)

릴레이 제어 핀은 소프트웨어에서 ON/OFF 추상화 처리

릴레이 모듈이 LOW 트리거인 경우를 고려한 설정 옵션 제공

### 4.3 통신 안정성 설계

Arduino R4 WiFi의 TLS 리소스 한계를 고려

STATUS POST와 CONTROL GET은 동일 SSL 소켓을 공유하지 않음

CONTROL 폴링 전에는 STATUS 연결을 명시적으로 종료

---
## 5. GPS 자동 차단 동작 흐름

사용자가 집 위치를 등록

앱이 해당 좌표를 기준으로 지오펜스 등록

사용자가 설정 반경을 벗어남 (EXIT)

```GeofenceBroadcastReceiver``` 수신

```AutoOffWorker``` 실행

자동 차단 대상으로 설정된 콘센트를 OFF로 설정한
CONTROL 스냅샷 전송

디바이스는 최신 CONTROL만 적용

---
## 6. 권한 요구 사항
```xml
android.permission.ACCESS_COARSE_LOCATION
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_BACKGROUND_LOCATION
android.permission.INTERNET
```

---
## 7. 테스트 항목

콘센트 개별 ON / OFF 제어 정상 동작 여부

CONTROL / STATUS CIN 서버 업로드 확인

자동 차단 대상 콘센트 설정 저장 여부

집 반경 이탈 시 자동 OFF 동작 여부

앱 종료 상태에서도 자동 차단 동작 여부

---
## 8. 개발 환경

Android Studio

Java

Retrofit2

Google Play Services Location

WorkManager

Arduino IDE

oneM2M (IoTcoss 플랫폼)
