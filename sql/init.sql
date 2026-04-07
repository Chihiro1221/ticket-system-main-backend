CREATE TABLE `ticket_info`
(
    `ticket_id`        VARCHAR(64)     NOT NULL COMMENT '票据ID',
    `name`             VARCHAR(128)    NOT NULL COMMENT '票据名称',
    `description`      VARCHAR(512)             DEFAULT NULL COMMENT '票据描述',
    `venue`            VARCHAR(128)             DEFAULT NULL COMMENT '场馆/地点',
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

INSERT INTO `ticket_info` (`ticket_id`, `name`, `description`, `venue`, `price`, `status`, `event_time`, `sale_start_time`, `sale_end_time`)
VALUES ('Concert-JayChou-2026-Shanghai', '周杰伦 2026 嘉年华世界巡演', '上海站内场与看台混合开售，热门场次，适合演示高并发抢票。', '上海梅赛德斯-奔驰文化中心', 1280.00, 1, '2026-07-01 19:30:00', '2026-04-10 10:00:00', '2026-07-01 19:30:00'),
       ('Concert-Mayday-2026-Beijing', '五月天 诺亚方舟限定场', '北京站加场，适合展示库存快速变化与支付占位倒计时。', '北京国家体育场', 980.00, 1, '2026-06-18 19:30:00', '2026-04-12 11:00:00', '2026-06-18 19:30:00'),
       ('Concert-Eason-2026-Guangzhou', '陈奕迅 FEAR and DREAMS 返场', '华南区域高热度场次，适合展示剩余库存预警。', '广州宝能观致文化中心', 1080.00, 1, '2026-05-22 20:00:00', '2026-04-14 12:00:00', '2026-05-22 20:00:00'),
       ('Concert-TFBOYS-2026-Shenzhen', 'TFBOYS 十三周年特别演出', '青少年粉丝集中抢票场景，适合模拟秒杀流量峰值。', '深圳湾体育中心春茧体育场', 880.00, 1, '2026-08-06 19:30:00', '2026-04-20 10:00:00', '2026-08-06 19:30:00'),
       ('Concert-Hebe-2026-Chengdu', '田馥甄 If Plus 成都站', '偏文艺风格场次，用于丰富项目数据层次。', '成都东安湖体育公园多功能馆', 780.00, 1, '2026-05-30 19:30:00', '2026-04-16 10:30:00', '2026-05-30 19:30:00'),
       ('Concert-Jolin-2026-Wuhan', '蔡依林 Ugly Beauty 武汉站', '中部热门演出场次，适合展示不同价位库存变化。', '武汉光谷国际网球中心', 1180.00, 1, '2026-06-12 19:30:00', '2026-04-18 10:00:00', '2026-06-12 19:30:00'),
       ('Concert-Hins-2026-Nanjing', '张敬轩 Reverie 南京站', '适合展示中高热度、库存逐步下降的演示效果。', '南京青奥体育公园体育馆', 680.00, 1, '2026-06-05 19:30:00', '2026-04-15 14:00:00', '2026-06-05 19:30:00'),
       ('Concert-Amei-2026-Xian', '张惠妹 ASMR 西安站', '西北区域大型演唱会，展示多城市票务大厅效果。', '西安奥体中心体育馆', 980.00, 1, '2026-07-18 19:30:00', '2026-04-22 10:00:00', '2026-07-18 19:30:00'),
       ('Festival-Strawberry-2026-Hangzhou', '草莓音乐节 2026 杭州站', '音乐节类票务，适合体现不同演出类型。', '杭州大运河杭钢公园', 520.00, 1, '2026-05-16 14:00:00', '2026-04-11 10:00:00', '2026-05-16 20:00:00'),
       ('Drama-Phantom-2026-Suzhou', '音乐剧《剧院魅影》苏州特别场', '非演唱会类票务数据，增强项目多样性。', '苏州文化艺术中心大剧院', 880.00, 1, '2026-05-09 19:30:00', '2026-04-09 10:00:00', '2026-05-09 19:30:00');

CREATE TABLE `ticket_stock`
(
    `id`          VARCHAR(64)  NOT NULL COMMENT '车次或场次ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '名称',
    `total_stock` INT          NOT NULL DEFAULT 0 COMMENT '总库存数量',
    `stock`       INT          NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存表';

-- 初始化一条数据，比如 JayChou 的演唱会，库存 100 张
INSERT INTO `ticket_stock` (`id`, `name`, `total_stock`, `stock`, `version`)
VALUES ('Concert-JayChou-2026-Shanghai', '周杰伦 2026 嘉年华世界巡演', 20000, 16842, 0),
       ('Concert-Mayday-2026-Beijing', '五月天 诺亚方舟限定场', 18000, 9320, 0),
       ('Concert-Eason-2026-Guangzhou', '陈奕迅 FEAR and DREAMS 返场', 15000, 6241, 0),
       ('Concert-TFBOYS-2026-Shenzhen', 'TFBOYS 十三周年特别演出', 30000, 1880, 0),
       ('Concert-Hebe-2026-Chengdu', '田馥甄 If Plus 成都站', 12000, 7320, 0),
       ('Concert-Jolin-2026-Wuhan', '蔡依林 Ugly Beauty 武汉站', 16000, 4820, 0),
       ('Concert-Hins-2026-Nanjing', '张敬轩 Reverie 南京站', 9000, 5112, 0),
       ('Concert-Amei-2026-Xian', '张惠妹 ASMR 西安站', 14000, 8450, 0),
       ('Festival-Strawberry-2026-Hangzhou', '草莓音乐节 2026 杭州站', 22000, 13980, 0),
       ('Drama-Phantom-2026-Suzhou', '音乐剧《剧院魅影》苏州特别场', 6000, 2740, 0);

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
        'zhangsan@example.com', '13800138000', '偏好周末晚场、优先前排'),
       ('admin001', 'admin', '$2a$10$AOjSx.I2F6UTYBeovvvCgOP6kLXuPWFeOo6vzm3iidzGHSIQkkhF.', 'admin', '系统管理员',
        'admin@ticketing.com', '13900139000', '负责票务运营、活动上架与库存管理');
