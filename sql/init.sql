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
    `user_id`     VARCHAR(64)  NOT NULL COMMENT '用户ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 模拟用户
INSERT INTO `user_info` (`user_id`, `username`, `password`)
VALUES ('user123', '张三', '123456');