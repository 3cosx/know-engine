# know-engine

个人知识库后端基础工程，基于 JDK 21、Spring Boot 4、MyBatis-Plus、Redis 和 Fastjson2。

## 技术栈

- JDK 21
- Spring Boot 4.1.1
- MyBatis-Plus 3.5.17
- MySQL 8.4
- Redis 7.4
- Fastjson2 2.0.65
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

创建示例：

```bash
curl -X POST http://localhost:8080/api/v1/knowledge-documents \
  -H "Content-Type: application/json" \
  -d '{"title":"JDK 21","content":"Virtual threads","tags":"java,jdk"}'
```

## 验证

测试使用内存 H2，不依赖本地 MySQL 和 Redis：

```bash
mvn test
```
