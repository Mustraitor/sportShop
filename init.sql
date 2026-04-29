CREATE DATABASE IF NOT EXISTS sport_shop DEFAULT CHARSET utf8mb4;
USE sport_shop;
SET NAMES utf8mb4;
ALTER DATABASE sport_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- =====================================================
-- RuoYi 系统表
-- =====================================================

DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
  config_id INT NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  config_name VARCHAR(100) DEFAULT '' COMMENT '参数名称',
  config_key VARCHAR(100) DEFAULT '' COMMENT '参数键名',
  config_value VARCHAR(500) DEFAULT '' COMMENT '参数键值',
  config_type CHAR(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500),
  PRIMARY KEY (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  menu_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  menu_name VARCHAR(50) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  order_num INT DEFAULT 0,
  path VARCHAR(200) DEFAULT '',
  component VARCHAR(255),
  is_frame INT DEFAULT 1,
  menu_type CHAR(1),
  visible CHAR(1) DEFAULT '0',
  perms VARCHAR(100),
  icon VARCHAR(100) DEFAULT '#',
  create_by VARCHAR(64),
  create_time DATETIME,
  update_by VARCHAR(64),
  update_time DATETIME,
  remark VARCHAR(500),
  PRIMARY KEY (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  role_id BIGINT NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(30) NOT NULL,
  role_key VARCHAR(100) NOT NULL,
  role_sort INT NOT NULL,
  data_scope CHAR(1) DEFAULT '1',
  status CHAR(1) NOT NULL,
  del_flag CHAR(1) DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME,
  update_by VARCHAR(64),
  update_time DATETIME,
  remark VARCHAR(500),
  PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  user_name VARCHAR(30) NOT NULL,
  nick_name VARCHAR(30) NOT NULL,
  user_type VARCHAR(2) DEFAULT '00',
  email VARCHAR(50),
  phonenumber VARCHAR(11),
  sex CHAR(1) DEFAULT '0',
  avatar VARCHAR(500),
  password VARCHAR(100),
  status CHAR(1) DEFAULT '0',
  del_flag CHAR(1) DEFAULT '0',
  login_ip VARCHAR(50),
  login_date DATETIME,
  create_by VARCHAR(64),
  create_time DATETIME,
  update_by VARCHAR(64),
  update_time DATETIME,
  remark VARCHAR(500),
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
  FOREIGN KEY (role_id) REFERENCES sys_role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id),
  FOREIGN KEY (role_id) REFERENCES sys_role(role_id),
  FOREIGN KEY (menu_id) REFERENCES sys_menu(menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `sys_config` VALUES ('1', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES ('2', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '初始化密码 123456');
INSERT INTO `sys_config` VALUES ('3', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '深色主题theme-dark，浅色主题theme-light');

INSERT INTO `sys_menu` VALUES ('1', '系统管理', '0', '1', 'system', null, '1', 'M', '0', '', 'system', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '系统管理目录');
INSERT INTO `sys_menu` VALUES ('100', '用户管理', '1', '1', 'user', 'system/user/index', '1', 'C', '0', 'system:user:list', 'user', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '用户管理菜单');
INSERT INTO `sys_menu` VALUES ('101', '角色管理', '1', '2', 'role', 'system/role/index', '1', 'C', '0', 'system:role:list', 'peoples', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '角色管理菜单');
INSERT INTO `sys_menu` VALUES ('102', '菜单管理', '1', '3', 'menu', 'system/menu/index', '1', 'C', '0', 'system:menu:list', 'tree-table', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '菜单管理菜单');


INSERT INTO `sys_role` VALUES ('1', '管理员', 'admin', '1', '1', '0', '0', 'admin', '2018-03-16 11:33:00', 'ry', '2018-03-16 11:33:00', '管理员');
INSERT INTO `sys_role` VALUES ('2', '普通角色', 'common', '2', '2', '0', '0', 'admin', '2018-03-16 11:33:00', 'system', '2020-04-02 07:58:06', '普通角色');
INSERT INTO `sys_role` VALUES ('100', '测试', 'test', '0', '1', '0', '0', 'admin', '2020-03-14 15:37:17', 'system', '2020-04-02 07:58:00', null);

INSERT INTO `sys_role_menu` VALUES ('1', '1');
INSERT INTO `sys_role_menu` VALUES ('1', '100');
INSERT INTO `sys_role_menu` VALUES ('1', '101');
INSERT INTO `sys_role_menu` VALUES ('1', '102');

INSERT INTO `sys_user` VALUES ('1', 'admin', 'alex', '00', 'alex@163.com', '15888888888', '1', 'https://wx.qlogo.cn/mmopen/vi_32/1yzTKJKIfurhDI29RqibEicNOoH0WiaCuKb6jWppVu4uzWovO0d1ICAwuW4rB4zfUxVvGHfNuxXLHu44t3yBkgbicQ/132', '$2a$10$boXFAiZ4OdtZiT2owx.xx.F848I4rh4JCQQDvgAaEwiktcFRh8Ile', '0', '0', '127.0.0.1', '2018-03-16 11:33:00', 'admin', '2018-03-16 11:33:00', 'ry', '2020-03-15 17:33:47', '管理员');
INSERT INTO `sys_user` VALUES ('2', '元春', '元春', '00', 'yr@qq.com', '15666666666', '1', 'https://wx.qlogo.cn/mmopen/vi_32/1yzTKJKIfurhDI29RqibEicNOoH0WiaCuKb6jWppVu4uzWovO0d1ICAwuW4rB4zfUxVvGHfNuxXLHu44t3yBkgbicQ/132', '$2a$10$LP25Pp2GITfV3QX6v.MmT.jV7vi.1rnQQJeeab3/AIyJn2.kPFiOq', '0', '0', '127.0.0.1', '2018-03-16 11:33:00', 'admin', '2018-03-16 11:33:00', 'admin', '2020-03-16 22:03:17', '测试员');

INSERT INTO `sys_user_role` VALUES ('1', '1');
INSERT INTO `sys_user_role` VALUES ('2', '2');
-- =====================================================
-- 电商业务表（核心模块）
-- =====================================================

-- 收货地址
DROP TABLE IF EXISTS address;
CREATE TABLE address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(50),
  phone VARCHAR(20),
  province VARCHAR(50),
  city VARCHAR(50),
  district VARCHAR(50),
  detail VARCHAR(255),
  is_default TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类
DROP TABLE IF EXISTS category;
CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50),
  parent_id BIGINT DEFAULT 0,
  sort INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品
DROP TABLE IF EXISTS product;
CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT,
  name VARCHAR(100),
  description TEXT,
  main_image VARCHAR(255),
  price DECIMAL(10,2),
  status TINYINT DEFAULT 1,
  stock INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SKU
DROP TABLE IF EXISTS product_sku;
CREATE TABLE product_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT,
  sku_name VARCHAR(100),
  price DECIMAL(10,2),
  stock INT DEFAULT 0,
  sku_code VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品图片
DROP TABLE IF EXISTS product_image;
CREATE TABLE product_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT,
  url VARCHAR(255),
  sort INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 购物车
DROP TABLE IF EXISTS cart;
CREATE TABLE cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  product_id BIGINT,
  sku_id BIGINT,
  quantity INT DEFAULT 1,
  checked TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
  FOREIGN KEY (product_id) REFERENCES product(id),
  FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  address_id BIGINT,
  total_amount DECIMAL(10,2),
  pay_amount DECIMAL(10,2),
  status TINYINT DEFAULT 0,
  payment_type TINYINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
  FOREIGN KEY (address_id) REFERENCES address(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单详情（快照设计）
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT,
  product_id BIGINT,
  sku_id BIGINT,
  product_name VARCHAR(100),
  sku_name VARCHAR(100),
  price DECIMAL(10,2),
  quantity INT,
  total_price DECIMAL(10,2),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 支付
DROP TABLE IF EXISTS payment;
CREATE TABLE payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT,
  user_id BIGINT,
  amount DECIMAL(10,2),
  payment_method TINYINT,
  trade_no VARCHAR(100),
  status TINYINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (user_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评价
DROP TABLE IF EXISTS review;
CREATE TABLE review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  product_id BIGINT,
  rating INT,
  content TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
  FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 物流
DROP TABLE IF EXISTS order_shipping;
CREATE TABLE order_shipping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT,
  courier_company VARCHAR(50),
  tracking_no VARCHAR(100),
  status VARCHAR(20),
  shipped_at DATETIME,
  delivered_at DATETIME,
  FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 秒杀/活动
DROP TABLE IF EXISTS promotion;
CREATE TABLE promotion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT,
  sku_id BIGINT,
  promo_price DECIMAL(10,2),
  stock INT,
  start_time DATETIME,
  end_time DATETIME,
  FOREIGN KEY (product_id) REFERENCES product(id),
  FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
