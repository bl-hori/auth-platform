# Project Context

## Purpose
認可基盤プラットフォーム (Authorization Platform) の構築。Permit.io風の統合認可基盤を提供し、RBAC/ABAC/ReBACをサポートする柔軟な認可システムを実現する。

### Goals
- 企業向けの統合認可基盤の構築
- ゼロレイテンシー（<10ms p95）でスケーラブルなアーキテクチャの実現
- ノーコード/ローコードでのポリシー管理
- 24/7の高可用性（99.99%）

## Tech Stack

### Backend
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.2+
- **Microservices**: Spring Cloud 2023.0+
- **Policy Engine**: Open Policy Agent (OPA) 0.60+ / Cedar 3.0+
- **API Gateway**: Spring Cloud Gateway 4.1+

### Frontend
- **Framework**: Next.js 15+ (App Router)
- **Language**: TypeScript 5.3+
- **UI Library**: React 18+
- **Styling**: Tailwind CSS 3.4+

### Data & Messaging
- **Database**: PostgreSQL 15+ (main), TimescaleDB 2.13+ (audit logs)
- **Cache**: Redis 7.2+
- **Message Broker**: Apache Kafka 3.6+
- **Graph DB**: Neo4j 5.15+ (ReBAC - optional)

### Infrastructure
- **Container Runtime**: Docker 24+
- **Orchestration**: Kubernetes 1.28+
- **Service Mesh**: Istio 1.20+ (optional)
- **CI/CD**: GitHub Actions + ArgoCD

## Project Conventions

### Code Style
- **Java**: Google Java Style Guide, enforced by Checkstyle
- **TypeScript**: Prettier + ESLint (Airbnb config)
- **Line Length**: 120 characters maximum
- **Documentation**: Comprehensive Javadoc/JSDoc for all public APIs

### Architecture Patterns
- **Pattern**: Microservices Architecture + Event-Driven
- **Design**: Control Plane / Data Plane separation
- **Data**: Database per Service pattern
- **Consistency**: Event-driven eventual consistency
- **Cache**: Multi-layer caching (L1: in-memory, L2: Redis)
- **API**: RESTful + gRPC for service-to-service communication

### Testing Strategy
- **Pyramid**: 50% Unit, 25% Component, 20% Integration, 5% E2E
- **Coverage**: 80%+ code coverage, 95%+ critical path coverage
- **Tools**: JUnit 5, Jest, Playwright, JMeter/Gatling
- **Security**: SAST (SonarQube), DAST (OWASP ZAP), SCA (Snyk)

### Git Workflow
- **Branching**: Git Flow (main, develop, feature/*, hotfix/*)
- **Commits**: Conventional Commits format
- **PR**: Required reviews, all tests must pass
- **Protection**: Main branch protected, no direct commits

### Naming Conventions
- **Capabilities**: kebab-case, verb-noun (e.g., `policy-management`, `user-authentication`)
- **Changes**: kebab-case, verb-led (e.g., `add-rbac-support`, `update-api-auth`)
- **Classes**: PascalCase (Java/TypeScript)
- **Methods**: camelCase
- **Constants**: UPPER_SNAKE_CASE

## Domain Context

### Authorization Models
- **RBAC** (Role-Based Access Control): ロールに基づくアクセス制御
- **ABAC** (Attribute-Based Access Control): 属性に基づくアクセス制御
- **ReBAC** (Relationship-Based Access Control): 関係性に基づくアクセス制御（Google Zanzibar風）

### Key Concepts
- **Policy**: 認可ルールの定義（Rego/Cedar言語で記述）
- **PDP** (Policy Decision Point): ポリシー評価エンジン
- **PEP** (Policy Enforcement Point): ポリシー実施点（アプリケーション側）
- **PAP** (Policy Administration Point): ポリシー管理点（管理画面）
- **Control Plane**: ポリシー管理・設定を行う制御層
- **Data Plane**: 認可判定を高速実行するデータ層

### Performance Requirements
- Authorization decision latency: **< 10ms (p95)**
- Throughput: **> 100,000 requests/sec**
- Policy update propagation: **< 1 second**
- Uptime: **99.99%** (< 52.56 min downtime/year)

## Important Constraints

### Technical Constraints
- Must support multi-tenancy (organization isolation)
- Must integrate with external IdPs (Keycloak, Auth0, etc.) via OIDC/SCIM
- Must support Policy-as-Code (Git integration)
- Backward compatibility for policy engine (OPA/Cedar)

### Business Constraints
- Phased rollout required (MVP → Advanced features)
- Zero-downtime deployments mandatory
- Support gradual migration from legacy systems

### Regulatory Constraints
- GDPR compliance (EU)
- Personal Information Protection Law compliance (Japan)
- SOC2 Type II certification required
- ISO 27001 compliance

### Security Constraints
- All communication must use TLS 1.3+
- mTLS for service-to-service communication
- Data encryption at rest (AES-256-GCM)
- Audit logs must be tamper-proof
- No secrets in code or configuration files

## External Dependencies

### Identity Providers
- **Keycloak**: Primary IdP for authentication
- **SCIM 2.0**: User/group synchronization protocol

### Policy Engines
- **Open Policy Agent (OPA)**: Primary policy engine (Rego language)
- **Cedar**: Alternative policy engine (Cedar language)
- **OPAL** (Open Policy Administration Layer): Policy distribution layer

### Monitoring & Observability
- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards
- **OpenTelemetry**: Distributed tracing
- **SIEM**: Security event forwarding (Syslog/HTTP)

### Development Tools
- **GitHub Actions**: CI pipeline
- **ArgoCD**: GitOps-based CD
- **SonarQube**: Code quality and security analysis
- **Trivy/Snyk**: Container and dependency scanning

## Simplicity-First Principles
- Default to straightforward implementations (< 100 lines for new features)
- Single-file implementations until proven insufficient
- Avoid frameworks without clear justification
- Choose boring, proven patterns over bleeding-edge
- Only add complexity when justified by data (performance, scale requirements)
