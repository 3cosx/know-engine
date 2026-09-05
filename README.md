# know-engine

个人知识库工程，基于 JDK 21 和 Spring Boot 4，提供用户权限、文档版本、MinerU 解析、Qwen 向量化及 Elasticsearch 关键词/向量索引能力。

## 技术栈

- JDK 21、Spring Boot 4.1.1、Maven
- MyBatis-Plus 3.5.17、MySQL 8.4
- Redis、Sa-Token、Fastjson2
- Apache Tika、MinIO、MinerU
- LangChain4j 1.20.0、Qwen Embedding
- Elasticsearch 8.18.6

## 领域模块结构

业务代码按领域内聚，不在项目根包下单独暴露通用的 `controller`、`service`、`dto`、`entity` 或 `mapper` 目录：

```text
src/main/java/com/cosx/knowengine/
├── common/                 # 统一响应和 BaseEntity 等共享模型
├── config/                 # MyBatis-Plus、Redis 等跨领域配置
├── exception/              # 全局异常与业务异常
├── user/                   # 用户、登录、权限和用户上下文
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── mapper/
│   ├── security/
│   └── service/
├── document/               # 文档上传、版本、分段和检索处理
└── chat/                   # 会话和消息领域
    ├── entity/
    └── enums/
```

聊天领域当前只完成实体和枚举设计；后续增加接口时，Controller、Service、DTO 和 Mapper 继续放在 `chat` 内部，不再创建根级业务目录。

文档业务只保留一个统一入口和一个流程中转层，避免按每个步骤拆出大量 Service：

```text
document/
├── controller/             # 文档 HTTP 接口
├── service/
│   └── DocumentService     # 上传、版本、查询、分段操作的统一入口
├── processing/
│   └── DocumentProcessor   # MinerU、切分、向量化、版本切换和清理中转
├── persistence/
│   └── DocumentPersistenceGateway  # 集中的短事务与数据库状态迁移
├── parser/                 # MinerU 与解析结果适配器
├── storage/                # MinIO 对象存储适配器
└── vector/
    └── DocumentVectorizationService # Qwen 与 Elasticsearch 向量能力
```

`DocumentProcessor` 参考 MinerU 文件处理流程，异步执行预签名、任务提交和轮询、ZIP 安全解压、Markdown 提取、分段和向量写入。默认按 `500` 字符切分并保留 `50` 字符重叠，优先在段落或换行处断开。

## 本地启动

确认 `java -version` 和 Maven 使用 JDK 21。Docker Compose 会启动 MySQL、Redis、MinIO 和 Elasticsearch：

```bash
docker compose up -d
mvn spring-boot:run
```

默认地址：

| 服务 | 地址 |
| --- | --- |
| 应用 API | `http://localhost:8080/api` |
| 文档管理页 | `http://localhost:8080/api/documents.html` |
| MinIO API / Console | `http://localhost:9000` / `http://localhost:9001` |
| Elasticsearch | `http://localhost:9200` |

首次启动后，通过 `POST /api/v1/auth/bootstrap` 创建第一个管理员。除登录和首次初始化外，请求使用 `Authorization: Bearer <token>` 传递 Sa-Token。

## 环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/know_engine?...` | MySQL 地址 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | MySQL 凭据 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 地址 |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO API 地址 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | `minioadmin` / `minioadmin` | 本地 MinIO 凭据 |
| `MINIO_BUCKET` | `know-engine` | 文档桶 |
| `MINERU_BASE_URL` | `https://mineru.net` | MinerU 服务地址 |
| `MINERU_TOKEN` | 空 | MinerU Token，必须自行配置 |
| `QWEN_ENABLED` | `false` | 是否启用向量模型 |
| `QWEN_API_KEY` | 空 | DashScope API Key |
| `QWEN_EMBEDDING_MODEL` | `text-embedding-v4` | Qwen 向量模型 |
| `QWEN_EMBEDDING_DIMENSIONS` | `1024` | 向量维度，需与 ES 索引一致 |
| `ES_ENDPOINT` | `http://localhost:9200` | Elasticsearch 地址 |
| `DOCUMENT_SEGMENT_SIZE` / `DOCUMENT_SEGMENT_OVERLAP` | `500` / `50` | 文档切分参数 |

不要把 `MINERU_TOKEN` 或 `QWEN_API_KEY` 写入仓库。需要执行完整解析和向量化时，至少设置：

```powershell
$env:MINERU_TOKEN = "your-mineru-token"
$env:QWEN_ENABLED = "true"
$env:QWEN_API_KEY = "your-dashscope-api-key"
mvn spring-boot:run
```

## 文档接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/knowledge-documents/upload` | 上传首版文档，默认版本 `1.0.0` |
| `POST` | `/api/v1/knowledge-documents/{documentId}/versions` | 上传新版本，需传旧 `baseVersion` |
| `GET` | `/api/v1/knowledge-documents` | 查询当前用户的文档列表 |
| `GET` | `/api/v1/knowledge-documents/{documentId}` | 查询当前版本详情 |
| `GET` | `/api/v1/knowledge-documents/{documentId}/processing-status` | 查询异步处理状态 |
| `GET` | `/api/v1/knowledge-documents/{documentId}/segments` | 分页查询当前版本分段 |
| `PUT` | `/api/v1/knowledge-documents/{documentId}/segments/{chunkId}` | 编辑分段并创建 PATCH 版本 |
| `POST` | `/api/v1/knowledge-documents/{documentId}/segments/{chunkId}/vectorize` | 幂等触发单分段向量化 |

上传接口先通过 Tika 校验文件、计算 SHA-256，并按当前用户去重。新版本完成向量化后才切换为当前版本，再清理旧版本向量；处理失败时旧版本仍可用。雪花 ID 在响应中按字符串返回。

## 数据库与验证

实体表结构维护在 `src/main/resources/db/schema.sql`，H2 测试结构维护在 `src/test/resources/schema-test.sql`，实体调整时两处必须同步。

```bash
mvn -DforkCount=0 test
```

完整流程需要可访问的 MinIO、MinerU、Qwen 和 Elasticsearch；单元与 H2 集成测试不调用真实外部服务。
