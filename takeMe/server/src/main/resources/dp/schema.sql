CREATE TABLE admin
(
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL COMMENT '密码',
    real_name       VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login_time DATETIME     NULL COMMENT '最后登录时间',
    INDEX idx_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='管理员表';

CREATE TABLE user
(
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    real_name       VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    phone           VARCHAR(20)  NOT NULL UNIQUE COMMENT '手机号',
    password        VARCHAR(100) NULL COMMENT '密码',
    avatar          VARCHAR(255) NULL COMMENT '头像',
    gender          TINYINT      NULL COMMENT '性别 0-男 1-女',
    age             INT          NULL COMMENT '年龄',
    address         VARCHAR(255) NULL COMMENT '居住地址',
    emergency_name  VARCHAR(50)  NULL COMMENT '紧急联系人姓名',
    emergency_phone VARCHAR(20)  NULL COMMENT '紧急联系人电话',
    status          TINYINT  DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login_time DATETIME     NULL COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='老人用户表';

CREATE TABLE volunteer
(
    id                  INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    real_name           VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    username            VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    phone               VARCHAR(20)  NOT NULL UNIQUE COMMENT '手机号',
    password            VARCHAR(100) NULL COMMENT '密码',
    avatar              VARCHAR(255) NULL COMMENT '头像',
    gender              TINYINT      NULL COMMENT '性别 0-男 1-女',
    age                 INT          NULL COMMENT '年龄',
    address             VARCHAR(255) NULL COMMENT '居住地址',
    service_days        TINYINT      NULL COMMENT '服务日期 0-周天 1-周一...6-周六',
    service_type        TINYINT      NULL COMMENT '服务类型 0-代购 1-助洁 2-助餐 3-助医',
    work_status         TINYINT  DEFAULT 0 COMMENT '工作状态 0-休息中 1-待命中 2-服务中',
    total_service_hours INT      DEFAULT 0 COMMENT '累计服务时长',
    emergency_name      VARCHAR(50)  NULL COMMENT '紧急联系人姓名',
    emergency_phone     VARCHAR(20)  NULL COMMENT '紧急联系人电话',
    status              TINYINT  DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login_time     DATETIME     NULL COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='志愿者表';

DROP TABLE IF EXISTS address;
CREATE TABLE address
(
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id     INT          NOT NULL COMMENT '所属用户ID',
    address     VARCHAR(255) NOT NULL COMMENT '地址详情',
    is_default  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认地址 0=否 1=是',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='地址信息表';

DROP TABLE IF EXISTS volunteer_leave;
CREATE TABLE volunteer_leave
(
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    volunteer_id INT          NOT NULL COMMENT '关联志愿者ID',
    type         TINYINT(1)   NOT NULL COMMENT '0=事假 1=病假',
    start_time   DATETIME     NOT NULL COMMENT '开始时间',
    end_time     DATETIME     NOT NULL COMMENT '结束时间',
    reason       VARCHAR(255) NOT NULL COMMENT '请假原因',
    status       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0=待审核 1=已通过 2=已拒绝',
    create_time  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    INDEX idx_volunteer_id (volunteer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='请假信息表';

DROP TABLE IF EXISTS message;
CREATE TABLE message
(
    id                   INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    receiver_id          INT          NOT NULL COMMENT '接收者ID（老人/志愿者ID）',
    receiver_type        TINYINT(1)   NOT NULL COMMENT '0=老人用户 1=志愿者',
    type                 TINYINT(1)   NOT NULL COMMENT '0=系统消息 1=订单消息 2=服务提醒',
    title                VARCHAR(100) NOT NULL COMMENT '消息标题',
    content              TEXT         NOT NULL COMMENT '消息内容',
    is_read              TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读 0=未读 1=已读',
    related_order_id     INT          NULL COMMENT '关联订单ID',
    related_user_id      INT          NULL COMMENT '关联老人ID',
    related_volunteer_id INT          NULL COMMENT '关联志愿者ID',
    related_url          VARCHAR(255) NULL COMMENT '跳转链接',
    create_time          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_receiver (receiver_id, receiver_type),
    INDEX idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统消息表';

DROP TABLE IF EXISTS service_package;
CREATE TABLE service_package
(
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '服务套餐ID',
    name        VARCHAR(100) NOT NULL COMMENT '套餐名：如“助洁2小时”',
    type        TINYINT      NOT NULL COMMENT '服务类型：0助餐 1助洁 2助医 3代购 4陪伴',
    price       INT          NOT NULL COMMENT '价格',
    description VARCHAR(500) NOT NULL COMMENT '服务介绍',
    image       VARCHAR(255) NULL COMMENT '服务图片',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type (type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务套餐表';

DROP TABLE IF EXISTS cart;
CREATE TABLE cart
(
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车ID',
    user_id     INT NOT NULL COMMENT '所属老人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='购物车表';

DROP TABLE IF EXISTS cart_item;
CREATE TABLE cart_item
(
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车项ID',
    cart_id       INT          NOT NULL COMMENT '所属购物车ID',
    product_id    INT          NOT NULL COMMENT '关联服务套餐ID',
    product_name  VARCHAR(100) NOT NULL COMMENT '服务名称快照',
    product_price INT          NOT NULL COMMENT '服务价格快照',
    quantity      INT          NOT NULL DEFAULT 1 COMMENT '数量',
    selected      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否勾选 0否 1是',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_cart_id (cart_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='购物车项表';

DROP TABLE IF EXISTS `order`;
CREATE TABLE `order`
(
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单主键',
    order_no      VARCHAR(50)  NOT NULL COMMENT '订单编号',
    user_id       INT          NOT NULL COMMENT '下单老人ID',
    volunteer_id  INT          NULL COMMENT '接单志愿者ID',
    total_price   INT          NOT NULL COMMENT '订单总金额',
    service_date  VARCHAR(50)  NOT NULL COMMENT '服务日期',
    service_time  VARCHAR(50)  NOT NULL COMMENT '服务时间',
    address       VARCHAR(255) NOT NULL COMMENT '服务地址',
    remark        VARCHAR(255) NULL COMMENT '备注',
    status        TINYINT      NOT NULL COMMENT '0待接单 1已接单 2服务中 3待确认 4已完成 5已取消',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    complete_time DATETIME     NULL COMMENT '完成时间',
    is_reviewed   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否评价 0否 1是',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_volunteer_id (volunteer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单主表';

DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item
(
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单项ID',
    order_id      INT          NOT NULL COMMENT '所属订单ID',
    product_id    INT          NOT NULL COMMENT '服务套餐ID',
    product_name  VARCHAR(100) NOT NULL COMMENT '服务名称',
    product_price INT          NOT NULL COMMENT '购买时价格',
    quantity      INT          NOT NULL COMMENT '数量',
    item_price    INT          NOT NULL COMMENT '小计金额',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单项表';

DROP TABLE IF EXISTS review;
CREATE TABLE review
(
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    order_id     INT          NOT NULL COMMENT '订单ID',
    user_id      INT          NOT NULL COMMENT '评价老人ID',
    volunteer_id INT          NOT NULL COMMENT '被评价志愿者ID',
    rating       INT          NOT NULL COMMENT '评分1-5星',
    comment      VARCHAR(500) NULL COMMENT '评价内容',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    INDEX idx_order_id (order_id),
    INDEX idx_volunteer_id (volunteer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务评价表';

DROP TABLE IF EXISTS volunteer_points_record;
CREATE TABLE volunteer_points_record
(
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    volunteer_id INT          NOT NULL COMMENT '志愿者ID',
    order_id     INT          NOT NULL COMMENT '关联订单ID',
    points       INT          NOT NULL COMMENT '本次变动积分',
    type         TINYINT      NOT NULL COMMENT '0=奖励 1=扣除',
    description  VARCHAR(255) NOT NULL COMMENT '记录说明',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_volunteer_id (volunteer_id),
    INDEX idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='志愿者积分记录表';