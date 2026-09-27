# PickView

**구독 없이, 보고 싶은 영상만.**

PickView는 창작자가 영상을 등록하고 시청자가 원하는 영상의 시청 권한을 한 편씩 또는 패키지로 구매하는 크리에이터 마켓플레이스 데모입니다.

보고 싶은 영상은 몇 편인데 채널 구독을 계속 유지하기는 부담스럽다는 친구의 이야기에서 시작했습니다. 편별 판매 아이디어를 실제 제품으로 옮기면서 구매 조건, 판매자 등록, 검수, 시청 권한, 환불과 정산이 어떻게 연결돼야 하는지 확인하기 위해 만들었습니다.

블로그의 PickView 제작기에서 다룬 제품과 코드가 이 저장소에 담겨 있습니다. 글이 선택의 이유를 설명한다면, 이 README는 그 선택을 직접 실행하고 구현을 찾아보기 위한 안내입니다. 현재는 **로컬에서 실행하는 데모**이며 실제 결제·송금이나 상용 서비스 운영을 제공하지 않습니다. 구매 의향과 창작자 참여 등 사업성 검증은 별도의 과제로 남아 있습니다.

[로컬 실행](#로컬-실행) · [데모 계정](#데모-계정) · [시연 순서](#시연-순서) · [글에서 코드로](#글에서-코드로) · [검증과 남은 범위](#검증과-남은-범위)

## 어떤 경험을 구현했나

판매자 신청 → 상품 등록 → 콘텐츠 검수 → 탐색과 미리보기 → 모의 구매 → 라이브러리와 본편 시청 → 환불 요청·처리까지 하나의 API와 데이터를 공유합니다.

| 사용자 | 할 수 있는 일 |
| --- | --- |
| 구매자 | 검색·분류·정렬, 미리보기, 장바구니와 모의 결제, 라이브러리, 이어보기·배속 조절, 후기·찜·팔로우, 문의·환불 요청 |
| 판매자 | 판매자 신청, 단품·패키지 등록, 파일 업로드, 검수 상태 확인, 판매 중단, 판매 실적·정산 예정액 확인 |
| 운영자 | 판매자·콘텐츠 심사, 신고·문의·환불 처리, 콘텐츠 제공 중단, 모의 정산. 업무에 따라 역할과 서버 접근 권한을 구분 |

화면은 한국어·영어를 지원하며 웹의 데스크톱·태블릿·모바일 너비에 대응합니다. Expo / React Native 클라이언트는 웹·iOS·Android에서 화면 코드를 공유합니다.

## 구매에 붙는 약속

- **구매 대상은 플랫폼 안에서의 시청 권한입니다.** 파일 다운로드는 제공하지 않습니다. 이용 기간은 구매 시점부터 7일·30일·90일 또는 기간 제한 없음이며, 기간 제한 없음은 별도 만료일을 두지 않는다는 뜻입니다.
- **주문 기록과 현재 시청 권한을 분리합니다.** 구매 당시의 상품명·금액·기간을 남기고, 만료와 환불은 시청 가능 여부에 반영합니다.
- **유효한 권한이 있는 영상의 중복 구매를 막습니다.** 단품과 패키지가 겹치는 경우도 검사합니다. 이미 가진 영상의 가격을 빼는 부분 할인은 구현하지 않았습니다.
- **판매 중단과 제공 중단은 다릅니다.** 판매자의 신규 판매 중단은 기존 구매자의 유효한 권한을 유지합니다. 운영자의 제공 중단은 본편 접근도 제한합니다.
- **환불 요청과 환불 완료를 구분합니다.** 운영자가 승인하면 해당 주문 항목의 권한을 회수합니다. 원래 결제 금액과 환불 금액은 주문 내역에 구분해 표시합니다.
- **미리보기와 본편의 접근 범위를 나눕니다.** 별도 미리보기 파일을 제공하며 본편은 파일 요청 시에도 서버가 권한을 확인합니다. 이미 전달된 영상의 복제를 제한하는 DRM은 포함하지 않습니다.

가격·기간·수수료 등은 현재 데모의 정책입니다. 상세 범위는 [제품 명세](docs/specification.md)에서 확인할 수 있습니다.

## 실제 구현과 모의 처리

| 구분 | 범위 |
| --- | --- |
| 실제로 동작 | 이메일·비밀번호 인증, 상품·주문 저장, 파일 검사와 미리보기 생성, 계정별 시청 권한, 이어보기 저장, 운영 역할 검사 |
| 모의 처리 | 결제 성공·실패·취소, 환불과 정산의 금전 이동, 소셜 로그인. 모의 결과에 따른 내부 주문·권한·정산 기록은 실제로 변경 |
| 제외 | 실제 결제사·은행·소셜 인증 연동, 이메일·푸시 발송, DRM, 오프라인 다운로드, 여러 화질의 적응형 스트리밍 |

샘플 콘텐츠는 사진·모션 그래픽·생성한 배경음으로 구성한 48초 데모 영상 6편과 별도의 9초 미리보기입니다. 실제 촬영 강의가 아니며, 블로그에 등장하는 추가 상품·거래는 시연 중 생성한 데이터라 초기 실행 화면과 다를 수 있습니다.

판매자 업로드는 H.264 영상의 MP4, 최대 1080p·100MB·10분을 대상으로 합니다. 오디오 트랙이 있다면 AAC여야 합니다. 미리보기는 전체 길이의 20% 이내이면서 최대 60초입니다.

## 기술 구성

| 영역 | 구성 |
| --- | --- |
| 클라이언트 | TypeScript, React Native, Expo, Expo Video, 반응형 웹 |
| 공통 API | Java, Spring Boot, Spring Security, JPA, Flyway |
| 기본 로컬 실행 | H2 파일 DB, 비공개 로컬 미디어 저장소, FFmpeg / FFprobe |
| 별도 구성 | PostgreSQL / MinIO — 설정 제공, 해당 조합의 실행 검증은 남아 있음 |
| 품질 검사 | TypeScript, ESLint, Prettier, Checkstyle, 클라이언트 회귀 테스트, Java 통합 테스트, GitHub Actions |

## 로컬 실행

처음 실행할 때는 Docker 없이 H2와 로컬 파일 저장소로 전체 데모를 살펴볼 수 있습니다. 아래 명령은 **Windows PowerShell** 기준입니다.

필요 도구: JDK 21 이상, Maven 3.9, Node.js 24, pnpm 11.19.0, FFmpeg와 FFprobe. GitHub Actions는 JDK 21 / Node.js 24를 사용하며 로컬에서는 Windows / JDK 25 / Node.js 24로 검증했습니다. `java`, `mvn`, `node`, `pnpm`, `ffmpeg`, `ffprobe`를 PATH에 등록합니다. FFmpeg는 H.264 인코더 `libx264`를 포함해야 합니다.

```powershell
git clone https://github.com/potterLim/pickview.git
cd pickview
pnpm --dir frontend install --frozen-lockfile
```

프로젝트 루트에서 터미널 두 개를 사용합니다.

```powershell
# 터미널 1: H2 파일 DB와 비공개 로컬 미디어를 사용하는 API
./scripts/start-api.ps1
```

```powershell
# 터미널 2: 웹 및 모바일 개발 서버
cd frontend
pnpm web
```

웹은 http://localhost:8081, API는 http://localhost:8080/api/health 입니다. 종료는 각 터미널에서 Ctrl+C입니다. API 실행 중에는 Windows가 JAR를 잠그므로 서버를 종료한 뒤 다시 빌드합니다.

도구를 별도 경로에 설치했다면 다음 환경변수를 사용합니다.

```powershell
$env:FFMPEG_PATH='C:/tools/ffmpeg.exe'
$env:FFPROBE_PATH='C:/tools/ffprobe.exe'
./scripts/start-api.ps1 -Maven 'C:/tools/apache-maven/bin/mvn.cmd'
```

`-SkipBuild`는 이미 빌드된 JAR를 실행합니다. 실행 스크립트가 `scripts/prepare-media.mjs`를 호출해 샘플 미디어를 준비합니다. 데이터는 `backend/.local/`에 남으므로 재시작해도 계정과 구매 내역이 유지됩니다. 이 폴더와 개인 환경변수 파일은 Git에 포함하지 않습니다.

PowerShell 스크립트를 사용하지 않을 때는 프로젝트 루트에서 다음 순서로 같은 로컬 API를 실행할 수 있습니다.

```sh
mvn -f backend/pom.xml package -DskipTests
node scripts/prepare-media.mjs
cd backend
java -jar target/pickview-api-0.1.0.jar --spring.profiles.active=local
```

초기 빌드는 테스트를 생략해 서버를 준비합니다. 변경사항 검증에는 아래의 `mvn verify`와 `pnpm check`를 사용합니다.

## 데모 계정

공통 비밀번호: `PickView-demo-2026!`. 로컬 시연 전용으로 공개된 값입니다.

기본 계정과 모의 인증을 포함한 구성입니다. 로컬 데모를 그대로 공개 서버에 운영하는 용도로 사용하지 않습니다.

| 이메일 | 역할 |
| --- | --- |
| buyer@pickview.demo | 구매자 |
| seller@pickview.demo | 승인된 판매자 |
| admin@pickview.demo | 전체 운영 |
| content@pickview.demo | 콘텐츠·판매자 심사 |
| support@pickview.demo | 문의·환불 |
| finance@pickview.demo | 정산 |

로그인 화면에서 시연 계정을 선택할 수 있습니다. 직접 이메일 계정을 만들어 구매자→판매자 신청→운영자 승인 흐름도 확인할 수 있습니다.

## 시연 순서

1. 구매자로 영상 미리보기 → 장바구니 → 모의 결제 → 라이브러리 → 본편 재생을 진행합니다. 결제 실패·취소도 선택할 수 있습니다.
2. 다른 브라우저에서 같은 계정으로 로그인하여 구매 영상과 이어보기 위치를 확인합니다.
3. 판매자로 스튜디오에서 제목·설명·태그·가격·기간·썸네일·MP4를 등록합니다. 업로드는 검수 대기로 전환됩니다.
4. 콘텐츠 담당자로 운영 관리에서 업로드 영상을 확인하고 승인합니다. 구매자 탐색에 나타납니다.
5. 구매자가 주문 내역에서 환불을 요청하고 고객지원 담당자가 처리합니다. 승인하면 기존 재생 링크도 차단됩니다.
6. 판매 중단은 기존 구매자의 권한을 유지합니다. 운영자의 제공 중단은 기존 구매자 재생도 차단합니다.

서로 다른 역할은 별도의 브라우저 프로필이나 시크릿 창으로 열면 계정을 바꾸지 않고 흐름을 따라갈 수 있습니다.

## IntelliJ Ultimate

프로젝트 루트를 열고 `backend/pom.xml`을 Maven 프로젝트로 연결합니다. Project SDK를 JDK 21 이상으로 설정하고 Maven 동기화를 실행합니다. `.run/PickView API.run.xml`의 Spring Boot 실행 구성을 사용할 수 있습니다. API의 작업 디렉터리는 `backend`, 활성 프로필은 `local`입니다. FFmpeg/FFprobe가 PATH에 없다면 실행 구성의 환경변수에 경로를 지정합니다.

처음 실행하기 전 프로젝트 루트에서 `node scripts/prepare-media.mjs`로 샘플 미디어를 준비합니다. `.run`은 팀에서 공유하는 IntelliJ 실행 설정입니다.

`frontend/package.json`의 `web`, `typecheck`, `lint` 스크립트는 IntelliJ에서 실행할 수 있습니다. 프런트엔드와 백엔드는 하나의 창에서 관리하지만 별도 프로세스입니다. iOS 네이티브 로컬 빌드는 macOS/Xcode가 필요합니다.

## PostgreSQL / MinIO 구성

`compose.yml`에 PostgreSQL과 MinIO가 정의되어 있습니다. Docker가 있는 환경에서 `docker compose up -d` 후 `local` 프로필 없이 서버를 실행하면 PostgreSQL을 사용합니다. 환경변수는 `backend/src/main/resources/application.yml`에 정의되어 있습니다.

`STORAGE_MODE=s3`로 설정하면 영상·썸네일 업로드가 MinIO의 비공개 버킷에 저장됩니다. 기본값은 비공개 로컬 저장소입니다. S3 모드에서 기본 샘플 영상을 사용하려면 `content/media/`의 `sample-video-*.mp4` 12개 파일을 `pickview` 버킷에 같은 이름으로 업로드하거나 판매자 화면에서 실제 샘플 영상을 새로 등록합니다. 기존 로컬 업로드가 자동으로 S3로 이동하지는 않습니다.

PostgreSQL/MinIO 런타임 검증은 아직 수행하지 않았습니다. 이 구성은 검증된 배포 안내가 아니라 별도 연동을 위한 출발점입니다.

## 모바일

필요하면 `frontend/.env.example`을 `frontend/.env`로 복사해 주소를 지정합니다. 실제 기기에서는 `EXPO_PUBLIC_API_URL`을 개발 PC의 LAN 주소 또는 테스트 API의 HTTPS 주소로 설정합니다. `localhost`는 각 기기 자신입니다. Android 에뮬레이터의 기본 개발 API 주소는 `http://10.0.2.2:8080`입니다. `.env.example`의 `localhost` 값을 그대로 복사하면 이 기본값을 덮어쓰므로 실행 대상에 맞게 바꿉니다. API URL에는 `/api`를 붙이지 않습니다. 웹 공유 링크는 `EXPO_PUBLIC_WEB_URL`로 설정합니다. 주소를 변경한 뒤 개발 서버를 재시작합니다.

```powershell
cd frontend
pnpm start
# Android SDK/에뮬레이터가 있을 때
pnpm android
# macOS/Xcode 시뮬레이터가 있을 때
pnpm ios
```

`eas.json`에 Android 내부 배포 APK와 iOS 시뮬레이터 빌드 프로필이 있습니다. EAS 빌드 실행·계정 연결·서명·스토어 제출은 수행하지 않았습니다. 릴리스 네이티브 앱은 HTTPS API를 사용해야 합니다. 로컬 HTTP 주소 허용 설정은 개발 빌드에서 별도로 검증해야 합니다.

## 검증과 남은 범위

| 환경·영역 | 확인 상태 |
| --- | --- |
| 로컬 웹 / H2 / 비공개 파일 저장소 | 구매·재생·판매자 업로드·검수·환불 흐름 확인. 데스크톱·태블릿·모바일 너비 점검 |
| Android | 에뮬레이터의 Expo Go에서 실행·시연 확인. 실제 기기와 서명된 릴리스 빌드는 별도 검증 필요 |
| iOS | 공용 코드와 빌드 설정 제공. 시뮬레이터·실제 기기 실행 검증은 남아 있음 |
| PostgreSQL / MinIO | 설정 제공. 실행 검증은 남아 있음 |
| 서비스 운영 | 실결제·외부 인증·송금·스토어 배포·부하·다중 서버 운영 검증은 범위 밖 |

재생 티켓은 서버 프로세스 메모리에 보관하며 일부 조회는 데모 규모를 전제로 합니다. 운영 규모를 늘리려면 상태 공유, 조회 방식, 영상 전송 비용과 부하를 별도로 검토해야 합니다. 테스트 통과는 모든 기기·접근성·운영 환경의 검증을 뜻하지 않습니다.

### 정적 검사와 테스트

프로젝트 루트에서 실행합니다.

```powershell
mvn -f backend/pom.xml verify
pnpm --dir frontend check
pnpm --dir frontend export:web
```

`pnpm check`는 TypeScript, 타입 기반 ESLint·React Hook 검사, 클라이언트 회귀 테스트를 실행합니다. Maven `verify`는 Java 명명·구조 규칙 검사와 통합 테스트를 포함합니다. [GitHub Actions](.github/workflows/verify.yml)는 푸시와 PR에서 이 검사와 웹 빌드를 실행합니다. 자동 배포는 하지 않습니다.

### 실행 중인 서버의 흐름 검증

API 테스트는 실행 중인 로컬 서버와 기본 시연 계정을 사용합니다. 임의 QA 계정과 거래·후기·문의가 추가되므로 시연 데이터와 분리한 환경에서 실행하는 것이 좋습니다.

```powershell
$env:TEST_VIDEO="$PWD/backend/.local/media/demo.mp4"
$env:TEST_THUMBNAIL="$PWD/frontend/assets/pottery.png"
node scripts/api-smoke.mjs
```

별도 API를 사용할 경우 테스트용 `API_URL`은 `/api`까지 포함해 지정합니다. `TEST_VIDEO`와 `TEST_THUMBNAIL`을 지정하면 업로드 검사도 수행합니다. 운영 데이터에는 실행하지 않습니다.

브라우저 회귀 검증은 [기본 흐름](scripts/browser-smoke.cjs)과 [결제·부분 환불·전체 환불 표시](scripts/order-payment-smoke.cjs) 스크립트로 나뉩니다. 별도로 설치한 Playwright의 모듈 경로를 `PLAYWRIGHT_MODULE`에, 사용할 Chrome 실행 파일을 `CHROME_PATH`에 지정합니다. 웹과 API 서버를 먼저 실행해야 하며, 이 검증은 현재 기본 CI에 포함하지 않습니다. 생성된 스크린샷은 기본적으로 임시 디렉터리에 저장됩니다.

## 글에서 코드로

제작기의 주제에 맞춰 구현을 찾아볼 수 있습니다.

| 글의 주제 | 코드의 시작점 |
| --- | --- |
| 0~2편: 아이디어, 구매 조건, 데모 범위 | [제품 명세](docs/specification.md), [상품 가격](backend/src/main/java/com/pickview/domain/ProductPrice.java), [이용 기간](backend/src/main/java/com/pickview/domain/EAccessTerm.java) |
| 3편: 구매 전 판단 | [탐색 화면](frontend/src/screens/DiscoverScreen.tsx), [상세와 미리보기](frontend/src/screens/DetailScreen.tsx) |
| 4편: 구매 이후 경험 | [주문·라이브러리 화면](frontend/src/screens/PurchaseScreens.tsx), [거래와 권한 처리](backend/src/main/java/com/pickview/commerce/CommerceService.java) |
| 5편: 판매자 경험 | [판매자 스튜디오](frontend/src/screens/StudioScreen.tsx), [상품 관리](backend/src/main/java/com/pickview/catalog/CatalogService.java) |
| 6편: 검수와 운영 책임 | [운영 화면](frontend/src/screens/AdminScreen.tsx), [미디어 접근](backend/src/main/java/com/pickview/media/MediaService.java) |
| 7편: 환불과 정산 | [운영 처리](backend/src/main/java/com/pickview/operations/OperationsService.java), [통합 테스트](backend/src/test/java/com/pickview/MarketplaceIntegrationTest.java) |
| 8편: 규칙과 실패 처리 | [원화 금액](backend/src/main/java/com/pickview/domain/WonAmount.java), [API 응답 검증](frontend/src/core/contracts.ts), [클라이언트 회귀 테스트](frontend/tests/core.test.cjs) |
| 9편: 다음 검증 과제 | 이 README의 [검증과 남은 범위](#검증과-남은-범위) |

내부에서는 식별자, 상품 가격, 원화 금액, 이용 기간과 심사·결제 상태를 타입으로 구분합니다. 외부 입력은 검증 후 강타입으로 전달하고 저장소 매핑에서 필요한 기본형으로 변환합니다. 표시용 문자열, 프레임워크 시그니처와 저장소 필드는 의미 없는 래퍼로 감싸지 않습니다.

## 구성과 개발 원칙

- `backend`: Spring Security, JPA, Flyway, 서버 측 구매·시청 권한, 미디어 처리.
- `frontend`: 반응형 웹 및 네이티브 공용 화면, Expo Video, 서버 API 클라이언트.
- `scripts`: 미디어 생성, 로컬 실행, API 회귀 검증.
- `docs`: 제품 제작 명세.
- `.github/workflows`: GitHub 자동 검증 설정.
- `.run`: IntelliJ 공유 실행 설정.
- `.idea/codeStyles`: IntelliJ 공유 서식 설정.

함수는 하나의 논리적 책임을 갖습니다. 파일은 책임·의존성·변경 이유가 나뉠 때 분리하며 길다는 이유만으로 나누지 않습니다. 커밋은 영문 `type: summary` 형식으로 관리합니다.

단순한 표현식은 한 줄을 기본으로 하며, 글자 수만으로 줄바꿈을 강제하지 않습니다. 복잡한 조건·인수 목록·스트림 처리 단계·JSX 계층은 읽기 쉬운 논리적 경계에서 나눕니다. IntelliJ 프로젝트 서식은 기존 줄바꿈을 보존하며, Prettier의 폭 기반 서식 검사는 사용하지 않습니다. 개인 IDE의 저장 시 Prettier 실행 설정이 켜져 있다면 이 프로젝트에서는 해제합니다.
