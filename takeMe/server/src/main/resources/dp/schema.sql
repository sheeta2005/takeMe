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


-- =============================================
-- 1. 管理员账号（1个）
-- =============================================
INSERT INTO admin (username, password, real_name, create_time, last_login_time)
VALUES
    ('admin', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', 'sheeta1998', '2025-01-01 09:00:00', '2026-05-29 15:30:00');

-- =============================================
-- 2. 志愿者账号（6个，包含3种状态）
-- 状态说明：0=待审核 1=正常 2=禁用
-- 服务类型：1=助老陪护 2=医疗协助 3=生活照料 4=心理疏导 5=文化娱乐
-- 工作状态：0=空闲 1=服务中 2=休息
-- =============================================
INSERT INTO volunteer (status, create_time, last_login_time, real_name, username, phone, password, avatar, gender, age, address, service_days, service_type, work_status, total_service_hours, emergency_name, emergency_phone)
VALUES
-- 正常状态志愿者（4个）
(1, '2025-03-15 10:20:00', '2026-05-28 18:45:00', '李浩然', 'lihaoran', '13800138001', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 24, '西安市长安区长安街123号', 86, 1, 0, 342, '李妈妈', '13800138002'),
(1, '2025-04-20 14:30:00', '2026-05-27 20:10:00', '张一诺', 'zhangyinuo', '13800138003', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 22, '西安市雁塔区雁塔路456号', 62, 2, 1, 248, '张爸爸', '13800138004'),
(1, '2025-05-10 09:15:00', '2026-05-29 10:05:00', '王宇轩', 'wangyuxuan', '13800138005', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 26, '西安市未央区未央路789号', 124, 3, 0, 496, '王姐姐', '13800138006'),
(1, '2025-06-05 16:40:00', '2026-05-26 14:20:00', '刘若汐', 'liuruoxi', '13800138007', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 23, '西安市碑林区碑林路101号', 47, 4, 2, 188, '刘哥哥', '13800138008'),

-- 待审核志愿者（1个）
(0, '2026-05-25 11:30:00', NULL, '陈沐阳', 'chenmuyang', '13800138009', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 21, '西安市高新区高新路202号', 0, 5, 0, 0, '陈妈妈', '13800138010'),

-- 禁用志愿者（1个）
(2, '2025-02-10 08:50:00', '2026-03-15 09:30:00', '赵梓涵', 'zhaozihan', '13800138011', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 25, '西安市灞桥区灞桥路303号', 32, 1, 0, 128, '赵爸爸', '13800138012');

-- =============================================
-- 3. 普通用户（老人）账号（10个，年龄60-88岁）
-- 状态说明：0=禁用 1=正常
-- 性别说明：0=男 1=女
-- =============================================
INSERT INTO user (real_name, username, phone, password, avatar, gender, age, emergency_name, emergency_phone, status, create_time, last_login_time)
VALUES
-- 男性老人（5个）
('张大爷', 'zhangdaye', '13900139001', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 72, '张小明', '13900139002', 1, '2025-02-01 10:00:00', '2026-05-28 16:30:00'),
('李爷爷', 'liyeye', '13900139003', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 81, '李小红', '13900139004', 1, '2025-02-15 14:20:00', '2026-05-29 09:15:00'),
('王伯伯', 'wangbobo', '13900139005', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 65, '王大伟', '13900139006', 1, '2025-03-05 09:30:00', '2026-05-27 18:40:00'),
('刘爷爷', 'liuyeye', '13900139007', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 88, '刘建国', '13900139008', 1, '2025-03-20 11:10:00', '2026-05-26 14:50:00'),
('陈大爷', 'chendaye', '13900139009', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 0, 68, '陈小丽', '13900139010', 0, '2025-04-10 15:40:00', '2026-04-10 10:20:00'),

-- 女性老人（5个）
('张奶奶', 'zhangnainai', '13900139011', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 75, '张小花', '13900139012', 1, '2025-04-25 08:50:00', '2026-05-29 11:30:00'),
('李阿姨', 'liayi', '13900139013', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 62, '李大明', '13900139014', 1, '2025-05-15 13:20:00', '2026-05-28 17:10:00'),
('王奶奶', 'wangnainai', '13900139015', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 78, '王小红', '13900139016', 1, '2025-06-01 10:40:00', '2026-05-27 19:25:00'),
('刘阿姨', 'liuayi', '13900139017', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 67, '刘小华', '13900139018', 1, '2025-06-20 16:10:00', '2026-05-26 15:40:00'),
('陈奶奶', 'chennainai', '13900139019', '$2a$12$U7xQ9zK3vB2nM5pR8sT7uY4wE6rT2yU8iO0pA5sD9fG3hJ7kL1', NULL, 1, 83, '陈大伟', '13900139020', 1, '2025-07-05 09:00:00', '2026-05-25 12:15:00');





INSERT INTO service_package (name, type, price, description, image, status, create_time) VALUES
-- ====================== 0=代购服务 ======================
('生活用品代购', 0, 10, '帮您购买日常用品、粮油米面，送货上门', '', 1, NOW()),
('药品代购', 0, 15, '帮您去药店购买常用药品，凭处方购买处方药', '', 1, NOW()),
('生鲜代购', 0, 20, '帮您购买新鲜蔬菜、水果、肉类，保证新鲜', '', 1, NOW()),

-- ====================== 1=助洁服务 ======================
('日常保洁2小时', 1, 30, '打扫房间、擦桌子、拖地、整理桌面', '', 1, NOW()),
('深度保洁3小时', 1, 50, '包含厨房油污清洁、卫生间消毒、玻璃擦拭', '', 1, NOW()),
('衣物清洗', 1, 25, '帮您清洗、晾晒衣物，可熨烫', '', 1, NOW()),
('家电清洁', 1, 40, '空调、冰箱、洗衣机等家电表面清洁', '', 1, NOW()),

-- ====================== 2=助餐服务 ======================
('营养套餐A', 2, 15, '一荤两素一汤，适合日常饮食', '', 1, NOW()),
('软食易消化餐', 2, 20, '软烂易咀嚼，适合牙口不佳的老人', '', 1, NOW()),
('糖尿病专用餐', 2, 25, '低糖低盐，适合糖尿病患者', '', 1, NOW()),
('高血压专用餐', 2, 25, '低油低盐，适合高血压患者', '', 1, NOW()),

-- ====================== 3=助医服务 ======================
('陪同就诊服务', 3, 40, '陪您去医院就诊、排队、取药、缴费', '', 1, NOW()),
('代取药品服务', 3, 20, '帮您去医院取药并送上门', '', 1, NOW()),
('体检陪同服务', 3, 50, '全程陪同体检，帮您拿报告、讲解结果', '', 1, NOW()),

-- ====================== 4=陪伴服务 ======================
('聊天陪伴', 4, 25, '陪您聊天、解闷，听您讲过去的故事', '', 1, NOW()),
('散步陪伴', 4, 30, '陪您出门散步、逛公园、晒太阳', '', 1, NOW());

-- 插入订单数据（用户ID=2，表名order加反引号解决关键字问题）
INSERT INTO `order` (order_no, user_id, volunteer_id, total_price, service_date, service_time, address, remark, status, create_time, complete_time, is_reviewed)
VALUES
-- 订单1：生活用品代购 + 药品代购（代购服务）
('ORD20260601001', 2, NULL, 25, '2026-06-02', '09:00-11:00', '北京市朝阳区测试小区1号楼101', '测试订单-代购服务', 0, NOW(), NULL, 0),
-- 订单2：日常保洁2小时（助洁服务）
('ORD20260601002', 2, NULL, 30, '2026-06-03', '14:00-16:00', '北京市朝阳区测试小区1号楼101', '测试订单-助洁服务', 1, NOW(), NULL, 0),
-- 订单3：营养套餐A + 糖尿病专用餐（助餐服务）
('ORD20260601003', 2, NULL, 40, '2026-06-04', '11:30-12:30', '北京市朝阳区测试小区1号楼101', '测试订单-助餐服务', 3, NOW(), NULL, 0);

-- 插入订单明细数据（对应上面3个订单，关联service_package）
INSERT INTO order_item (order_id, product_id, product_name, product_price, quantity, item_price, create_time)
VALUES
-- 订单1 明细
(1, 1, '生活用品代购', 10, 1, 10, NOW()),
(1, 2, '药品代购', 15, 1, 15, NOW()),
-- 订单2 明细
(2, 4, '日常保洁2小时', 30, 1, 30, NOW()),
-- 订单3 明细
(3, 7, '营养套餐A', 15, 1, 15, NOW()),
(3, 9, '糖尿病专用餐', 25, 1, 25, NOW());