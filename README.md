# PickView

구독 없이 원하는 영상만 구매하는 크리에이터 마켓플레이스 데모입니다. React Native / Expo 클라이언트가 웹·iOS·Android 코드를 공유하고, Java / Spring Boot API가 계정·구매·시청 권한을 관리합니다. 결제·환불·정산·소셜 로그인은 시연용이며 실제 금전 거래는 없습니다.

## 실행

필요 도구: JDK 21 이상, Maven 3.9, Node.js 22 이상, pnpm, FFmpeg와 FFprobe. 검증 환경은 Windows, JDK 25, Node.js 24입니다. `java`, `mvn`, `node`, `pnpm`, `ffmpeg`, `ffprobe`를 PATH에 등록합니다. FFmpeg는 H.264 인코더 `libx264`를 포함해야 합니다.

프로젝트 루트에서 터미널 두 개를 사용합니다.

```powershell
# 터미널 1: H2 파일 DB와 비공개 로컬 미디어를 사용하는 API
./scripts/start-api.ps1
```

```powershell
# 터미널 2: 웹 및 모바일 개발 서버
cd frontend
pnpm install --frozen-lockfile
pnpm web
```

웹은 http://localhost:8081, API는 http://localhost:8080/api/health 입니다. 종료는 각 터미널에서 Ctrl+C입니다. API 실행 중에는 Windows가 JAR를 잠그므로 서버를 종료한 뒤 다시 빌드합니다.

도구를 별도 경로에 설치했다면 다음 환경변수를 사용합니다.

```powershell
$env:FFMPEG_PATH='C:/tools/ffmpeg.exe'
$env:FFPROBE_PATH='C:/tools/ffprobe.exe'
./scripts/start-api.ps1 -Maven 'C:/tools/apache-maven/bin/mvn.cmd'
```

`-SkipBuild`는 이미 빌드된 JAR를 실행합니다. 샘플 영상은 사진·모션 그래픽·직접 생성한 배경음으로 구성한 48초 오리지널 영상 6편과 별도 9초 미리보기입니다. `scripts/prepare-media.mjs`가 번들 영상을 로컬 저장소에 복사합니다. 실제 촬영 강의가 아닌 데모용 콘텐츠이며, 기존 구매 권한과 사용자 업로드는 보존됩니다. 데이터는 `backend/.local/`에 남으므로 재시작해도 계정과 구매 내역이 유지됩니다.

## 데모 계정

공통 비밀번호: `PickView-demo-2026!`. 로컬 시연 전용으로 공개된 값입니다.

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

시연 API 테스트는 임의 QA 계정을 추가하고 테스트 상품은 판매 중단합니다. 반복 실행한 로컬 DB에 QA 후기·문의가 남는 것은 정상입니다.

## IntelliJ Ultimate

프로젝트 루트를 열고 `backend/pom.xml`을 Maven 프로젝트로 연결합니다. Project SDK를 JDK 21 이상으로 설정하고 Maven 동기화를 실행합니다. `.run/PickView API.run.xml`의 Spring Boot 실행 구성을 사용할 수 있습니다. API의 작업 디렉터리는 `backend`, 활성 프로필은 `local`입니다. FFmpeg/FFprobe가 PATH에 없다면 실행 구성의 환경변수에 경로를 지정합니다.

`frontend/package.json`의 `web`, `typecheck`, `lint` 스크립트는 IntelliJ에서 실행할 수 있습니다. 프런트엔드와 백엔드는 하나의 창에서 관리하지만 별도 프로세스입니다. iOS 네이티브 로컬 빌드는 macOS/Xcode가 필요합니다.

## PostgreSQL / MinIO 구성

`compose.yml`에 PostgreSQL과 MinIO가 정의되어 있습니다. Docker가 있는 환경에서 `docker compose up -d` 후 `local` 프로필 없이 서버를 실행하면 PostgreSQL을 사용합니다. 환경변수는 `backend/src/main/resources/application.yml`에 정의되어 있습니다.

`STORAGE_MODE=s3`로 설정하면 영상·썸네일 업로드가 MinIO의 비공개 버킷에 저장됩니다. 기본값은 비공개 로컬 저장소입니다. S3 모드에서 기본 샘플 영상을 사용하려면 `content/media/`의 `sample-video-*.mp4` 12개 파일을 `pickview` 버킷에 같은 이름으로 업로드하거나 판매자 화면에서 실제 샘플 영상을 새로 등록합니다. 기존 로컬 업로드가 자동으로 S3로 이동하지는 않습니다.

실행한 검증은 H2/로컬 저장소를 사용했습니다. PostgreSQL/MinIO 런타임 검증은 아직 수행하지 않았습니다. 실제 배포 전에는 PostgreSQL/MinIO 연동과 네이티브 기기 동작을 별도로 검증해야 합니다.

## 모바일

`frontend/.env.example`을 `.env`로 복사합니다. 실제 기기에서는 `EXPO_PUBLIC_API_URL`을 개발 PC의 LAN 주소 또는 테스트 API의 HTTPS 주소로 설정합니다. `localhost`는 각 기기 자신입니다. Android 에뮬레이터의 기본 개발 API 주소는 `10.0.2.2:8080`입니다. 웹 공유 링크는 `EXPO_PUBLIC_WEB_URL`로 설정합니다.

```powershell
cd frontend
pnpm start
# Android SDK/에뮬레이터가 있을 때
pnpm android
# macOS/Xcode 시뮬레이터가 있을 때
pnpm ios
```

`eas.json`에 Android 내부 배포 APK와 iOS 시뮬레이터 빌드 프로필이 있습니다. EAS 빌드 실행·계정 연결·서명·스토어 제출은 수행하지 않았습니다. 릴리스 네이티브 앱은 HTTPS API를 사용해야 합니다. 로컬 HTTP 주소 허용 설정은 개발 빌드에서 별도로 검증해야 합니다.

## 검증 명령

```powershell
mvn -f backend/pom.xml verify
cd frontend
pnpm check
pnpm exec expo export --platform all
cd ..
$env:TEST_VIDEO="$PWD/backend/.local/media/demo.mp4"
$env:TEST_THUMBNAIL="$PWD/frontend/assets/pottery.png"
node scripts/api-smoke.mjs
```

API 테스트는 실행 중인 로컬 서버와 기본 시연 계정을 사용합니다. 별도 API를 사용할 경우 `API_URL`을 `/api`까지 포함해 지정합니다. 운영 데이터에 실행하지 않습니다.

`pnpm check`는 TypeScript, 타입 기반 ESLint·React Hook 검사, 클라이언트 회귀 테스트, Java/TypeScript/스크립트 서식 검사를 실행합니다. Maven `verify`는 Java 명명·구조 규칙 검사와 통합 테스트를 포함합니다. 같은 검사를 GitHub Actions에서도 실행합니다.

API의 기존 JSON 표현은 유지하며 내부에서는 식별자, 상품 가격, 원화 금액, 이용 기간과 심사·결제 상태를 구분합니다. 외부 입력은 검증 후 강타입으로 전달하고 저장소 매핑에서 필요한 기본형으로 변환합니다. 표시용 문자열, 프레임워크 시그니처와 저장소 필드는 의미 없는 래퍼로 감싸지 않습니다.

## 구성과 개발 원칙

- `backend`: Spring Security, JPA, Flyway, 서버 측 구매·시청 권한, 미디어 처리.
- `frontend`: 반응형 웹 및 네이티브 공용 화면, Expo Video, 서버 API 클라이언트.
- `scripts`: 미디어 생성, 로컬 실행, API 회귀 검증.
- `docs`: 제품 제작 명세.

함수는 하나의 논리적 책임을 갖습니다. 파일은 책임·의존성·변경 이유가 나뉠 때 분리하며 길다는 이유만으로 나누지 않습니다. 커밋은 영문 `type: summary` 형식으로 관리합니다.
