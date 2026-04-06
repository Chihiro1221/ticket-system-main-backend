CREATE TABLE `ticket_info`
(
    `ticket_id`        VARCHAR(64)     NOT NULL COMMENT '票据ID',
    `name`             VARCHAR(128)    NOT NULL COMMENT '票据名称',
    `description`      VARCHAR(512)             DEFAULT NULL COMMENT '票据描述',
    `price`            DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '票价',
    `status`           TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0-下架, 1-上架',
    `event_time`       DATETIME                 DEFAULT NULL COMMENT '场次时间',
    `sale_start_time`  DATETIME                 DEFAULT NULL COMMENT '开售时间',
    `sale_end_time`    DATETIME                 DEFAULT NULL COMMENT '截止售卖时间',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ticket_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='票据基础信息表';

INSERT INTO `ticket_info` (`ticket_id`, `name`, `description`, `price`, `status`, `event_time`, `sale_start_time`, `sale_end_time`)
VALUES ('Concert-JayChou-2026', 'JayChou演唱会', '周杰伦 2026 巡回演唱会示例场次', 680.00, 1,
        '2026-07-01 19:30:00', '2026-04-10 10:00:00', '2026-07-01 19:30:00');

CREATE TABLE `ticket_stock`
(
    `id`          VARCHAR(64)  NOT NULL COMMENT '车次或场次ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '名称',
    `stock`       INT          NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存表';

-- 初始化一条数据，比如 JayChou 的演唱会，库存 100 张
INSERT INTO `ticket_stock` (`id`, `name`, `stock`, `version`)
VALUES ('Concert-JayChou-2026', 'JayChou演唱会', 100, 0);

CREATE TABLE `order_info`
(
    `order_id`    VARCHAR(64) NOT NULL COMMENT '订单ID',
    `user_id`     VARCHAR(64) NOT NULL COMMENT '用户ID',
    `ticket_id`   VARCHAR(64) NOT NULL COMMENT '车次或场次ID',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    -- 关键索引：通过联合唯一索引防止同一个用户对同一个场次下多次订单
    UNIQUE KEY `uk_user_ticket` (`user_id`, `ticket_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

CREATE TABLE `user_info`
(
    `user_id`            VARCHAR(64)   NOT NULL COMMENT '用户ID',
    `username`           VARCHAR(64)   NOT NULL COMMENT '账号名',
    `password`           VARCHAR(255)  NOT NULL COMMENT '密码（加密后）',
    `role`               VARCHAR(16)   NOT NULL DEFAULT 'user' COMMENT '角色：admin/user/ban',
    `nickname`           VARCHAR(64)            DEFAULT NULL COMMENT '昵称',
    `avatar`             VARCHAR(255)           DEFAULT NULL COMMENT '头像',
    `email`              VARCHAR(128)           DEFAULT NULL COMMENT '邮箱',
    `phone`              VARCHAR(32)            DEFAULT NULL COMMENT '手机号',
    `ticket_preferences` VARCHAR(1024)          DEFAULT NULL COMMENT '票务偏好信息，预留扩展',
    `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 模拟用户
INSERT INTO `user_info` (`user_id`, `username`, `password`, `role`, `nickname`, `email`, `phone`, `ticket_preferences`)
VALUES ('user123', 'zhangsan', '$2a$10$7TqK5fV5N8QCC5B7A0tL8OkVhLrG0C0Hjv5mP7L4YyL5E6YgL2P8G', 'user', '张三',
        'zhangsan@example.com', '13800138000', '偏好周末晚场、优先前排');
