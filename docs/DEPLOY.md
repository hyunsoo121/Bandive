# 배포 (AWS, EC2 Spot)

`main` 브랜치가 실제 운영 브랜치. `develop → main` PR이 머지되면 GitHub Actions(`.github/workflows/deploy.yml`)가
이미지를 빌드해 ECR에 올리고, SSM으로 EC2 인스턴스에서 `docker compose pull && up -d` 를 실행한다.

## 아키텍처

```
Route53(A레코드) → Elastic IP → EC2 Spot(persistent, interrupt=stop, t3.micro, us-east-1)
                                    └─ docker compose (모두 restart: unless-stopped)
                                        ├─ web(Caddy)   — TLS 자동발급 + SPA 정적서빙 + /api 등 backend 프록시
                                        ├─ backend      — Spring Boot
                                        ├─ postgres     — EBS 루트 볼륨에 데이터
                                        └─ redis
```

- Spot 인스턴스가 회수되면 **정지만** 되고(터미네이트 아님) 루트 EBS는 그대로 남는다. 용량이 생기면 AWS가 자동으로
  다시 켜주고, Docker가 `restart: unless-stopped` 로 컨테이너를 알아서 복구한다 — 별도 부팅 스크립트 불필요.
- 서버에는 `docker-compose.prod.yml` + `.env` 딱 두 파일만 있으면 된다. 코드/인프라 변경은 전부 이미지 빌드로
  흘러가고, 서버는 그 이미지를 pull만 한다.
- SSH 포트(22)는 아예 열지 않는다. 서버 접속·배포 모두 SSM(Session Manager / RunCommand)으로.

## 한 번만 하는 설정

### 1. ECR 리포지토리

```bash
aws ecr create-repository --repository-name bandive-backend  --region us-east-1
aws ecr create-repository --repository-name bandive-frontend --region us-east-1
```

### 2. GitHub Actions용 IAM 역할 (OIDC, 액세스키 없음)

OIDC 프로바이더가 계정에 없으면 먼저 생성 (IAM → ID 공급자 → 공급자 추가 → OpenID Connect,
URL `https://token.actions.githubusercontent.com`, 대상 `sts.amazonaws.com`). 이미 있으면 스킵.

신뢰 정책 (`<ACCOUNT_ID>`, `<GH_OWNER>/<GH_REPO>` 치환):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": ["sts:AssumeRoleWithWebIdentity", "sts:TagSession"],
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike": { "token.actions.githubusercontent.com:sub": "repo:<GH_OWNER>/<GH_REPO>:ref:refs/heads/main" }
    }
  }]
}
```

(`sts:TagSession` 빠뜨리면 `Not authorized to perform sts:AssumeRoleWithWebIdentity` 로 실패한다 —
`aws-actions/configure-aws-credentials` 가 기본으로 세션 태그를 붙여서 assume하기 때문에 이것도 같이 허용해야 함.)

권한 정책 (`<ACCOUNT_ID>`, `<INSTANCE_ID>` 치환 — 인스턴스는 3번에서 만든 뒤 채워도 됨):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "EcrAuth", "Effect": "Allow", "Action": "ecr:GetAuthorizationToken", "Resource": "*" },
    {
      "Sid": "EcrPush", "Effect": "Allow",
      "Action": ["ecr:BatchCheckLayerAvailability", "ecr:PutImage", "ecr:InitiateLayerUpload", "ecr:UploadLayerPart", "ecr:CompleteLayerUpload"],
      "Resource": [
        "arn:aws:ecr:us-east-1:<ACCOUNT_ID>:repository/bandive-backend",
        "arn:aws:ecr:us-east-1:<ACCOUNT_ID>:repository/bandive-frontend"
      ]
    },
    {
      "Sid": "SsmSend", "Effect": "Allow", "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:us-east-1:<ACCOUNT_ID>:instance/<INSTANCE_ID>",
        "arn:aws:ssm:us-east-1::document/AWS-RunShellScript"
      ]
    },
    { "Sid": "SsmResult", "Effect": "Allow", "Action": ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"], "Resource": "*" }
  ]
}
```

(`GetCommandInvocation`은 리소스 레벨 제한을 지원하지 않아 `*` — AWS 사양.)

역할 이름 예: `bandive-github-deploy`. ARN을 GitHub secret `AWS_DEPLOY_ROLE_ARN` 에 저장.

### 3. EC2 인스턴스 역할 (SSM + ECR pull)

새 역할(신뢰 대상: `ec2.amazonaws.com`)에 다음을 붙인다:
- 관리형 정책 `AmazonSSMManagedInstanceCore`
- 관리형 정책 `AmazonEC2ContainerRegistryReadOnly` (또는 pull 전용 인라인 정책)

### 4. EC2 Spot 인스턴스 launch

콘솔 "인스턴스 시작":
- AMI: **Amazon Linux 2023 (x86_64)**
- 인스턴스 유형: **t3.micro** (RAM 1GB — 버벅이거나 OOM 나면 t3.small로 갈아타기)
- 키 페어: 없어도 됨 (SSH 안 씀, SSM으로 접속)
- 네트워크: 기본 VPC, 퍼블릭 서브넷
- 보안 그룹: 인바운드 **80, 443** 만 (0.0.0.0/0). 22는 열지 않음
- 스토리지: 루트 볼륨 gp3 20GB, **"종료 시 삭제" 체크 해제**
- IAM 인스턴스 프로필: 3번에서 만든 역할
- 고급 세부 정보 → 사용자 데이터:

```bash
#!/bin/bash
dnf update -y
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

mkdir -p /opt/bandive

# t3.micro 는 RAM 1GB 뿐 — 넷(Caddy/backend/postgres/redis) 다 올리기엔 빠듯해서
# 스왑 2GB 를 안전장치로 걸어둔다 (OOM killer 가 컨테이너 죽이는 것보단 느려도 낫다)
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

- 고급 세부 정보 → 구매 옵션: **스팟 인스턴스 요청** 체크 → 요청 유형 **지속적(Persistent)**,
  인터럽트 시 동작 **중지(Stop)**, 최대 가격은 **비워둠**(온디맨드 상한 사용)

시작 후: **탄력적 IP** 하나 할당해서 이 인스턴스에 연결. 인스턴스 ID를 GitHub secret
`AWS_EC2_INSTANCE_ID` 에 저장. 권한 정책의 `<INSTANCE_ID>` 도 이걸로 채워 넣기(2번으로 돌아가서).

### 5. 서버에 최초 파일 배치 + 첫 기동

Session Manager로 접속 (콘솔의 "연결" → Session Manager, 또는 `aws ssm start-session --target <instance-id>`):

```bash
sudo su -
mkdir -p /opt/bandive && cd /opt/bandive
# 로컬에서 이 두 파일 내용을 복사해 붙여넣거나, S3/scp 등으로 옮긴다
#  - docker-compose.prod.yml  (레포 루트의 파일 그대로)
#  - .env                     (.env.prod.example 참고해서 실제 값 채운 것)

aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin "$(grep ECR_REGISTRY .env | cut -d= -f2)"

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

### 6. DNS + 카카오

- 도메인 A 레코드 → 위에서 만든 Elastic IP
- 카카오 디벨로퍼스 → 내 애플리케이션 → 카카오 로그인 → Redirect URI에
  `https://<도메인>/login/oauth2/code/kakao` 추가 (기존 localhost 것은 유지)

### 7. GitHub repo secrets

| 이름 | 값 |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | 2번에서 만든 역할 ARN |
| `AWS_EC2_INSTANCE_ID` | 4번 인스턴스 ID |

여기까지 하면 이제부터는 `develop → main` 머지 = 배포.

## 평상시 배포 흐름

1. `feature/N` → `develop` 로 평소처럼 PR/머지 (배포 안 됨, CI만 돎)
2. 준비되면 `develop → main` PR → 머지
3. `deploy.yml` 이 백엔드/프론트 이미지를 빌드해 ECR에 `:latest` + `:<커밋sha>` 로 push
4. SSM으로 서버에서 `docker compose pull && up -d` 실행, 결과를 Actions 로그에서 확인 가능

## 롤백

이전 커밋 sha 이미지가 ECR에 남아있으니, 서버에서:

```bash
cd /opt/bandive
sed -i 's/:latest/:<이전 sha>/' docker-compose.prod.yml   # 또는 IMAGE_TAG 를 compose 에 도입해도 됨
docker compose -f docker-compose.prod.yml up -d
```

## 백업

- **EBS 스냅샷**: AWS Backup 또는 DLM(Data Lifecycle Manager)으로 루트 볼륨 매일 스냅샷 — 몇 백원/월.
- **논리 백업**: 컨테이너 안에서 `pg_dump` 를 cron으로 돌려 S3에 별도 저장 (블록 스냅샷만 믿지 않기).
  ```bash
  docker compose -f /opt/bandive/docker-compose.prod.yml exec -T postgres \
    pg_dump -U bandive bandive | gzip > backup-$(date +%F).sql.gz
  ```

## 남은 일 (배포 전 권장)

- **S3 StorageService**: 로고/배너 업로드가 지금은 로컬 디스크(`bandive-uploads` 볼륨)에 저장됨.
  EBS에 묶여있어 인스턴스 볼륨을 통째로 교체하면 유실됨 — `StorageService` 의 S3 구현체로 교체 권장
  (`backend/CLAUDE.md` Phase 7 항목 참고). 없어도 배포는 되지만 내구성이 EBS 하나에 의존하게 됨.
