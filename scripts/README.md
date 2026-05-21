# 쿠팡 수동 매핑 도구

자동 매핑(`/admin/coupang/retry`)으로 잡히지 않은 상품들을 사람이 직접 검토·매핑하는 도구.
- **`manual_map.py`** — 터미널 CLI. 팀원이 로컬에서 실행
- **백엔드 API 3개** — `/admin/coupang/manual/*` (이미 서버에 추가됨)

## 동작 그림

```
[로컬 PC]                                [AWS]
  manual_map.py                            (운영 EC2 와는 무관)
       │
       │  HTTP localhost:8080
       ▼
  Spring Boot (로컬 ./gradlew bootRun)
       │
       ├── DB: SSH 터널 → 운영 RDS
       └── 쿠팡 파트너스 API (분당 50회 제한)
```

쿠팡 호출은 **로컬 서버에서** 일어나고, 결과는 **운영 RDS** 의 `coupang_links` 테이블에 즉시 반영됨.
운영 EC2 서버는 이 작업과 무관 — 그쪽 트래픽에 영향 없음.

---

## 1. 사전 준비 (한 번만)

### 1-1. Java 17 + Gradle (이미 있으면 패스)
```bash
java -version    # 17 이상이어야 함
```

### 1-2. Python 3 + requests (Mac 은 보통 기본 설치)
```bash
python3 --version
pip3 install requests
```

### 1-3. 쿠팡 API 키
`src/main/resources/application.yml` (또는 `application-local.yml`) 의
`coupang.access-key`, `coupang.secret-key` 가 채워져 있어야 함.
(운영 키 그대로 써도 됨. 분당 50회 제한은 글로벌 throttle 로 자동 보호됨.)

---

## 2. DB 접속 — SSH 터널링

운영 RDS 는 보통 외부 접속이 막혀있어요. EC2 를 거쳐서 터널 뚫습니다.

### 2-1. 별도 터미널에서 터널 켜기 (작업 내내 켜둠)
```bash
ssh -i ~/.ssh/your-key.pem \
    -L 5432:<RDS-endpoint>:5432 \
    ubuntu@<EC2-public-IP>
```
- `<RDS-endpoint>` 예: `nutriuniv-prod.xxxxx.ap-northeast-2.rds.amazonaws.com`
- `<EC2-public-IP>` 예: `13.124.xx.xx`
- 이 터미널은 **그대로 두고** 다른 작업 진행

### 2-2. 로컬 `application-local.properties` DB 설정
`src/main/resources/application-local.properties` (gitignore 되어 있음) 에서 DB 호스트를 `localhost:5432` 로:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<DB이름>
spring.datasource.username=<RDS 사용자>
spring.datasource.password=<RDS 비밀번호>
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.connection-init-sql=CREATE EXTENSION IF NOT EXISTS vector
```

> ⚠️ `application-local.properties` 는 이미 `.gitignore` 되어 있어 커밋되지 않습니다. 안심하고 운영 RDS 비밀번호 넣어도 됨.

### 2-3. OAuth 키 더미 (없으면 빈 생성 실패)
같은 파일에 OAuth 관련 모든 키가 있어야 부팅 됩니다. `application-local.properties.example` 보고 누락된 게 있으면 채우세요. 사용 안 할 거면 더미 값으로:

```properties
naver.client-id=dummy
naver.client-secret=dummy
naver.redirect-uri=http://localhost:8080/auth/oauth
```
(kakao, google 도 마찬가지 — 본인이 안 쓸 거면 더미)

---

## 3. 서버 띄우기

```bash
./gradlew bootRun
```

기동 로그 끝에 `Started NutriUnivApplication` 가 보이면 OK.
다음 명령으로 동작 확인:
```bash
curl "http://localhost:8080/admin/coupang/manual/queue?status=FAILED" | head -c 200
```
`{"data":{"totalCount": ...}}` 비슷한 응답 오면 정상.

---

## 4. CLI 실행

### 처음부터
```bash
cd scripts
python3 manual_map.py
# → "시작 productId 입력 (엔터 = 처음부터):" 프롬프트
# → 엔터 또는 시작 ID 입력
```

### 특정 ID 부터 재개
```bash
python3 manual_map.py --start-from 6800
```

### 팀원 분담 (ID 범위 나누기)
```bash
# 팀원 A
python3 manual_map.py --start-from 1     --to-id 10000

# 팀원 B
python3 manual_map.py --start-from 10001 --to-id 20000
```

### 매핑된 거 검수 (잘못 매핑된 거 잡기)
```bash
python3 manual_map.py --status LINKED --start-from 1
# → 매핑된 상품들이 차례로 표시됨
# → 매핑이 맞으면 엔터 (그냥 통과)
# → 잘못됐고 새 정답이 있으면 번호 선택해서 덮어쓰기
# → 잘못됐고 정답 없으면 s (SKIP)
```

### 여러 상태 동시
```bash
python3 manual_map.py --status FAILED UNLINKED
python3 manual_map.py --status ALL   # 전체
```

---

## 5. CLI 사용법

상품 한 건이 표시될 때:

```
──────────────────────────────────────────────────────────
[123/3000]  productId=6748  name=꼬기다 스틱 닭가슴살 숯불데리야끼맛
  현재 상태: FAILED

  검색 키워드: "꼬기다 스틱 닭가슴살 숯불데리야끼맛"
   1. 꼬기다 스틱 닭가슴살 5종 혼합 x 2개 세트   ₩29,900 [로켓]
   2. [랭킹닭컴] 맛있닭 소스 통 닭가슴살 5종...   ₩45,000
   ...
  20. ...

선택 [번호/엔터/s/r/h/q]:
```

| 키 | 의미 |
|---|---|
| **숫자** (1~20) | 해당 후보로 매핑 (LINKED). 기존 매핑이 있으면 덮어쓰기 |
| **엔터** | 변경 없이 다음 상품 — 이미 매핑 OK 거나 일단 보류 |
| **s** | 매칭 포기. **영구 SKIPPED** — 다음 작업에서 다시 안 나옴 |
| **r 새키워드** | 키워드 바꿔서 같은 상품 재검색 (예: `r 꼬기다`) |
| **h** | 도움말 다시 표시 |
| **q** | 종료. 마지막 처리 productId 출력 → 다음 실행 시 이어서 가능 |

---

## 6. 동시 작업 시 안전성

- 쿠팡 분당 50회 제한은 **API 키 단위**라 팀원과 합쳐서 카운트됨
- 그래도 안전한 이유: 로컬 서버의 `CoupangApiClient` 에 **글로벌 throttle** 이 들어가 있음
  - 누가 어디서 호출하든 마지막 쿠팡 호출로부터 3초 이내면 자동 wait
  - 단 **각자 로컬 서버가 다르면** 글로벌 락이 분리됨
  - → **여러 명이 작업해도 OK**, 단 모두 같은 RDS / 같은 쿠팡 키 쓰는 상황만 주의
- 사람이 후보 보고 입력하는 시간(평균 10초)이 호출 간격(3초)보다 길어 실제로는 거의 안 겹침

> 🚨 **주의**: 운영 EC2 서버에서 retry / bulksync 같은 게 동시에 돌면 안 됨.
> 작업 시간만큼 그쪽 자동화는 꺼두세요.

---

## 7. 진행 상황 확인 (DB)

작업 도중 또는 끝나고 DB 에서 직접:

```sql
SELECT link_status, COUNT(*) FROM coupang_links GROUP BY link_status;
```

| link_status | 의미 |
|---|---|
| LINKED   | 자동/수동으로 쿠팡 상품 매핑 완료 |
| FAILED   | 자동 매핑 실패. 수동 매핑 대상 |
| UNLINKED | 아직 매핑 시도 안 함 |
| SKIPPED  | 수동으로 매칭 포기. 광고 노출에서 제외 (LINKED 만 노출하므로) |

---

## 8. 트러블슈팅

**Q. 서버가 안 떠요**
- `./gradlew bootRun` 로그에 `Connection refused` 가 보이면 → SSH 터널이 안 켜져 있음. 2-1 단계 확인
- 포트 충돌이면 → `lsof -i :8080` 으로 점유 프로세스 확인

**Q. CLI 가 `Connection refused`**
- 서버가 띄워져 있는지: `curl http://localhost:8080/admin/coupang/manual/queue?status=FAILED`
- 다른 포트면 `--server http://localhost:포트번호`

**Q. 검색 결과가 매번 너무 느림 (3초+α)**
- 정상입니다. 쿠팡 분당 50회 제한 때문에 호출 간 3초 sleep 들어감. 다른 사람이 동시 작업 중이면 더 길어질 수 있어요.

**Q. 실수로 엉뚱한 번호 눌렀어요**
- 같은 productId 를 다시 검색하면 됨. 새 번호 누르면 자동 덮어쓰기.
- 또는 `--status LINKED --start-from <그productId>` 로 검수 모드에서 다시 잡기.

**Q. 진짜 쿠팡에 없는 상품인데 어떻게 표시?**
- `s` (SKIPPED) 처리. 다음 큐에서 영구 제외됨.
