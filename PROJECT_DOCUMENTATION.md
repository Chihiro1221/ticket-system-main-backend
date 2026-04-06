# 票务系统后端服务 - 项目说明文档

## 📋 项目概述

### 基本信息
- **项目名称**: ticket-system-main-backend
- **项目版本**: 0.0.1-SNAPSHOT
- **开发语言**: Java 21
- **框架**: Spring Boot 3.5.14-SNAPSHOT
- **构建工具**: Maven

### 项目描述
这是一个基于分布式架构的票务系统后端服务，采用了 **Dapr (Distributed Application Runtime)** 作为分布式应用运行时，通过 **Actor 模型** 实现高并发场景下的库存管理和订单处理。系统解决了传统票务系统中的超卖、库存一致性和订单超时处理等核心问题。

---

## 🏗️ 技术架构

### 核心技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.14-SNAPSHOT | 应用框架 |
| Java | 21 | 编程语言 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| Dapr SDK | 1.13.1 | 分布式应用运行时 |
| MySQL | - | 关系型数据库 |
| Lombok | - | 代码简化 |
| Spring Web | - | Web 服务 |

### 架构特点

#### 1. Actor 模型
- 使用 Dapr Actor 实现并发控制
- 每个 Ticket 对应一个 Actor 实例
- 通过 Actor 内部状态管理库存，避免并发冲突

#### 2. 事件驱动架构
- 使用 Dapr Pub/Sub 进行异步消息传递
- 订单创建后发送到消息队列
- 消费者异步处理库存扣减和订单创建

#### 3. 定时任务机制
- 使用 Dapr Reminder 实现订单超时处理
- 默认 10 分钟未支付自动取消订单
- 支持动态取消定时器

#### 4. 数据一致性保障
- 数据库乐观锁（version 字段）
- 唯一索引防止重复下单
- 事务保证库存扣减和订单创建的原子性

---

## 📊 数据库设计

### 1. 库存表 (ticket_stock)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 车次或场次ID（主键） |
| name | VARCHAR(128) | 名称 |
| stock | INT | 当前库存数量 |
| version | INT | 乐观锁版本号 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

**初始数据**:
```sql
INSERT INTO ticket_stock (id, name, stock, version)
VALUES ('Concert-JayChou-2026', 'JayChou演唱会', 100, 0);
```

### 2. 订单表 (order_info)

| 字段 | 类型 | 说明 |
|------|------|------|
| order_id | VARCHAR(64) | 订单ID（主键） |
| user_id | VARCHAR(64) | 用户ID |
| ticket_id | VARCHAR(64) | 车次或场次ID |
| status | TINYINT | 状态：0-待支付, 1-已支付, 3-已取消 |
| create_time | DATETIME | 下单时间 |
| update_time | DATETIME | 更新时间 |

**关键约束**:
```sql
UNIQUE KEY uk_user_ticket (user_id, ticket_id)
```
防止同一用户对同一场次多次下单。

### 3. 用户表 (user_info)

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | VARCHAR(64) | 用户ID（主键） |
| username | VARCHAR(64) | 用户名 |
| password | VARCHAR(128) | 密码 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 🔧 核心模块

### 1. Actor 模块

#### TicketActor 接口
```java
@ActorType(name = "TicketActor")
public interface TicketActor extends ActorFactory {
    // 扣减库存方法
    Mono<Boolean> deductTicket(int count);
    
    // 支付成功回调
    Mono<Void> confirmPayment(String orderId);
    
    // 查询当前剩余库存
    Mono<Integer> getRestCount();
}
```

#### TicketActorImpl 实现
- **库存扣减逻辑**:
  1. 先从 Actor 状态存储获取库存
  2. 如果不存在则从数据库加载
  3. 检查库存是否充足
  4. 扣减库存并更新状态
  5. 注册 10 分钟超时 Reminder
  6. 发送订单创建事件到消息队列

- **支付确认逻辑**:
  1. 更新订单状态为已支付
  2. 取消超时 Reminder
  3. 处理失败情况（触发退款逻辑）

- **超时处理逻辑**:
  1. 检查订单状态（仅处理待支付订单）
  2. 更新订单状态为已取消
  3. 回滚库存到数据库
  4. 更新 Actor 内存状态

### 2. 服务模块

#### TicketStockService
库存管理服务，继承自 `IService<TicketStock>`，提供基础的 CRUD 操作。

#### OrderInfoService
订单管理服务，额外提供订单取消和库存回滚功能：
```java
boolean cancelOrderAndRestoreStock(String orderId);
```

**实现逻辑**:
1. 将订单状态从 0（待支付）改为 3（已取消）
2. 对应的库存数量加 1
3. 使用事务保证操作的原子性

### 3. 事件模块

#### OrderCreatedEvent
订单创建事件，包含以下字段：
- `orderId`: 订单ID
- `userId`: 用户ID
- `ticketId`: 场次ID
- `stock_count`: 购票数量
- `timestamp`: 时间戳

### 4. 消息消费模块

#### OrderEventConsumer
订单事件消费者，监听 `order_topic` 主题。

**处理流程**:
1. 接收订单创建事件
2. 扣减对应票的库存（带条件更新）
3. 创建订单记录
4. 利用唯一索引防止重复处理
5. 使用 `@Transactional` 保证数据一致性

**异常处理**:
- 库存不足或扣减失败：抛出异常，事务回滚
- 重复订单：捕获 `DuplicateKeyException`，自动忽略

### 5. Dapr 集成模块

#### DaprClientHolder
Dapr 客户端单例持有者，提供全局访问点。

---

## 🔄 业务流程

### 1. 订单创建流程

```
用户下单
   ↓
调用 TicketActor.deductTicket()
   ↓
Actor 检查库存（内存状态）
   ↓
库存充足：扣减库存
   ↓
注册 10 分钟超时 Reminder
   ↓
发送 OrderCreatedEvent 到消息队列
   ↓
OrderEventConsumer 消费消息
   ↓
扣减数据库库存（带条件更新）
   ↓
创建订单记录
   ↓
返回成功
```

### 2. 支付确认流程

```
用户完成支付
   ↓
调用 TicketActor.confirmPayment(orderId)
   ↓
更新订单状态为已支付
   ↓
取消超时 Reminder
   ↓
返回成功
```

### 3. 订单超时处理流程

```
10 分钟超时触发
   ↓
TicketActor.receiveReminder() 被调用
   ↓
handleOrderTimeout() 处理
   ↓
检查订单状态（必须为待支付）
   ↓
更新订单状态为已取消
   ↓
回滚库存到数据库
   ↓
更新 Actor 内存状态
   ↓
完成
```

---

## 🚀 部署配置

### 数据库配置 (application.yml)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ticket_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC
    username: root
    password: youzhi..
server:
  port: 8080
```

### Dapr 配置要求

需要配置以下 Dapr 组件：
1. **State Store**: 用于存储 Actor 状态（建议使用 Redis）
2. **Pub/Sub**: 用于消息传递（名称为 `pubsub`）

---

## 🎯 核心特性

### 1. 高并发支持
- Actor 模型天然支持并发控制
- 每个票种独立 Actor，避免全局锁
- 内存状态缓存，减少数据库压力

### 2. 数据一致性
- 数据库乐观锁防止并发更新冲突
- 唯一索引防止重复下单
- 事务保证库存和订单的一致性

### 3. 容错机制
- 订单超时自动取消
- 库存自动回滚
- 重复消息幂等处理

### 4. 可扩展性
- 基于 Dapr 的微服务架构
- 易于水平扩展
- 支持分布式部署

---

## 📝 使用说明

### 启动应用

```bash
# 使用 Maven Wrapper
./mvnw spring-boot:run

# 或使用 Maven
mvn spring-boot:run
```

### 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 调用 Actor 示例

```bash
# 扣减库存
curl -X POST http://localhost:3500/v1.0/actors/TicketActor/Concert-JayChou-2026/method/deductTicket \
  -H "Content-Type: application/json" \
  -d '1'

# 查询剩余库存
curl -X POST http://localhost:3500/v1.0/actors/TicketActor/Concert-JayChou-2026/method/getRestCount \
  -H "Content-Type: application/json"

# 确认支付
curl -X POST http://localhost:3500/v1.0/actors/TicketActor/Concert-JayChou-2026/method/confirmPayment \
  -H "Content-Type: application/json" \
  -d '"order_id_here"'
```

---

## ⚠️ 注意事项

1. **Dapr 依赖**: 应用必须运行在 Dapr 环境中
2. **数据库依赖**: 需要预先创建 MySQL 数据库并执行初始化脚本
3. **组件配置**: 需要配置 Dapr 的 State Store 和 Pub/Sub 组件
4. **超时设置**: 当前订单超时时间硬编码为 10 分钟，可根据需要调整

---

## 🔍 项目结构

```
ticket-system-main-backend/
├── src/main/java/com/haonan/ticketsystemmainbackend/
│   ├── TicketSystemMainBackendApplication.java    # 启动类
│   ├── actor/                                      # Actor 模块
│   │   ├── TicketActor.java                        # Actor 接口
│   │   └── TicketActorImpl.java                    # Actor 实现
│   ├── controller/                                 # 控制器
│   │   └── HelloController.java                    # 示例控制器
│   ├── dapr/                                       # Dapr 集成
│   │   ├── DaprClientHolder.java                   # Dapr 客户端持有者
│   │   └── consumer/                               # 消息消费者
│   │       └── OrderEventConsumer.java             # 订单事件消费者
│   ├── domain/                                     # 领域模型
│   │   ├── OrderCreatedEvent.java                  # 订单创建事件
│   │   ├── OrderInfo.java                          # 订单实体
│   │   └── TicketStock.java                        # 库存实体
│   ├── mapper/                                     # 数据访问层
│   │   ├── OrderInfoMapper.java                    # 订单 Mapper
│   │   └── TicketStockMapper.java                  # 库存 Mapper
│   └── service/                                    # 服务层
│       ├── OrderInfoService.java                   # 订单服务接口
│       ├── TicketStockService.java                 # 库存服务接口
│       ├── impl/
│       │   ├── OrderInfoServiceImpl.java           # 订单服务实现
│       │   └── TicketStockServiceImpl.java         # 库存服务实现
├── src/main/resources/
│   └── application.yml                             # 应用配置
├── sql/
│   └── init.sql                                    # 数据库初始化脚本
├── pom.xml                                         # Maven 配置
└── HELP.md                                         # Spring Boot 帮助文档
```

---

## 📚 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Dapr 官方文档](https://dapr.io/docs/)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Java 21 文档](https://docs.oracle.com/en/java/javase/21/)

---

## 👥 维护信息

- **开发者**: heart
- **创建日期**: 2026-04-02
- **最后更新**: 2026-04-04

---

**文档版本**: 1.0
**生成时间**: 2026-04-04
