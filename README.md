# know-engine

个人知识库后端基础工程，基于 JDK 21、Spring Boot 4、MyBatis-Plus、Redis 和 Fastjson2。

## 技术栈

- JDK 21
- Spring Boot 4.1.1
- MyBatis-Plus 3.5.17
- MySQL 8.4
- Redis 7.4
- Fastjson2 2.0.65
- Sa-Token 1.46.0
- Maven 3.9+

## 目录结构

```text
src/main/java/com/cosx/knowengine
├── common       # 统一响应、分页对象
├── config       # MyBatis-Plus、Redis 配置
├── controller   # HTTP 接口
├── dto          # 请求和响应模型
├── entity       # 数据库实体
├── exception    # 业务异常与全局异常处理
├── mapper       # MyBatis-Plus Mapper
├── security     # Sa-Token 权限提供器、ThreadLocal 用户上下文
└── service      # 业务接口与实现
```

## 本地启动

环境要求：`java -version` 为 21，Maven 运行时也建议使用 JDK 21。

```bash
docker compose up -d
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080/api`。数据库和 Redis 可通过以下环境变量覆盖：

| 环境变量 | 默认值 |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/know_engine?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `root` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `REDIS_PASSWORD` | 空 |
| `SERVER_PORT` | `8080` |

## 示例接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/knowledge-documents` | 新建知识条目 |
| `GET` | `/api/v1/knowledge-documents/{id}` | 查询详情 |
| `GET` | `/api/v1/knowledge-documents?page=1&size=20&keyword=java` | 分页搜索 |
| `PUT` | `/api/v1/knowledge-documents/{id}` | 更新条目，需要携带当前 `version` |
| `DELETE` | `/api/v1/knowledge-documents/{id}` | 逻辑删除条目 |

除登录和首次初始化外，接口通过 `Authorization: Bearer <token>` 传递 Sa-Token。

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/bootstrap` | 公开，仅首次可用 | 创建第一个 ADMIN 并登录 |
| `POST` | `/api/v1/auth/login` | 公开 | 用户登录 |
| `POST` | `/api/v1/auth/logout` | 登录 | 退出登录 |
| `GET` | `/api/v1/auth/me` | 登录 | 当前用户信息 |
| `GET/POST/PUT/DELETE` | `/api/v1/users/**` | `ADMIN` | 用户和授权管理 |
| `GET/POST` | `/api/v1/permissions` | `ADMIN` | 权限管理及分页 |
| `GET/POST/PUT/DELETE` | `/api/v1/knowledge-documents/**` | `NORMAL` | 知识条目管理 |

`ADMIN` 自动拥有 `NORMAL` 能力。首次启动后先初始化管理员：

```bash
curl -X POST http://localhost:8080/api/v1/auth/bootstrap \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_01","password":"ChangeMe123!","nickname":"管理员"}'
```

该接口在用户表已有数据后会永久拒绝再次初始化。

创建示例：

```bash
curl -X POST http://localhost:8080/api/v1/knowledge-documents \
  -H "Content-Type: application/json" \
  -d '{"documentId":10001,"documentName":"JDK 21","content":"Virtual threads","documentUser":20001,"documentType":"markdown"}'
```

## 验证

测试使用内存 H2，不依赖本地 MySQL 和 Redis：

```bash
mvn test
```
