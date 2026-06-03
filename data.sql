USE sport_shop;
-- ==========================================
-- 1. 补充测试买家用户
-- ==========================================
INSERT INTO sys_user (user_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, create_by, create_time) VALUES 
(3, 'zhangsan', '张三(买家)', '01', 'zhangsan@qq.com', '13800138000', '1', '$2a$10$boXFAiZ4OdtZiT2owx.xx.F848I4rh4JCQQDvgAaEwiktcFRh8Ile', '0', 'admin', NOW()),
(4, 'lisi', '李四(买家)', '01', 'lisi@163.com', '13900139000', '0', '$2a$10$boXFAiZ4OdtZiT2owx.xx.F848I4rh4JCQQDvgAaEwiktcFRh8Ile', '0', 'admin', NOW());

-- ==========================================
-- 2. 收货地址假数据 (address)
-- ==========================================
INSERT INTO address (id, user_id, name, phone, province, city, district, detail, is_default, created_at) VALUES 
(1, 3, '张三', '13800138000', '北京市', '北京市', '朝阳区', '望京SOHO T1 1101室', 1, NOW()),
(2, 3, '张三(公司)', '13800138000', '北京市', '北京市', '海淀区', '中关村软件园 8号楼', 0, NOW()),
(3, 4, '李四', '13900139000', '上海市', '上海市', '浦东新区', '张江高科技园区 88号', 1, NOW());

-- ==========================================
-- 3. 分类数据 (category)
-- ==========================================
INSERT INTO category (id, name, parent_id, sort, created_at) VALUES 
(1, '运动鞋靴', 0, 1, NOW()),
(2, '跑步鞋', 1, 1, NOW()),
(3, '篮球鞋', 1, 2, NOW());

-- ==========================================
-- 4. 商品主表 (product)
-- ==========================================
INSERT INTO product (id, category_id, name, description, main_image, price, stock, status, created_at) VALUES 
(10001, 2, 'Nike Air Zoom Pegasus 39 登月跑步鞋', '透气缓震，适合日常慢跑及中长跑训练。', '10001_main.png', 699.00, 500, 1, NOW()),
(10002, 3, 'Adidas Trae Young 1 吹杨一代篮球鞋', '极度防滑，专为后卫打造的实战利器。', '10002_main.png', 899.00, 200, 1, NOW());

-- ==========================================
-- 5. 商品库存明细表 (product_sku) 
-- ==========================================
INSERT INTO product_sku (product_id, sku_name, price, stock, sku_code, created_at, is_deleted) VALUES 
(10001, '黑白基础色-42码', 699.00, 100, 'NK-AP39-BLK-42', NOW(), 0),
(10001, '黑白基础色-43码', 699.00, 80, 'NK-AP39-BLK-43', NOW(), 0),
(10002, '冰雪蓝-42.5码', 899.00, 50, 'ADI-TY1-BLU-425', NOW(), 0);

-- ==========================================
-- 6. 商品图片副表 (product_image)
-- ==========================================
INSERT INTO product_image (product_id, url, sort, created_at) VALUES 
(10001, '10001_1.png', 1, NOW()),
(10001, '10001_2.png', 1, NOW());

-- ==========================================
-- 7. 购物车 (cart)
-- ==========================================
INSERT INTO cart (user_id, product_id, sku_id, quantity, checked, created_at) VALUES 
(3, 10001, 1, 1, 1, NOW()),         
(4, 10001, 1, 2, 0, NOW());         

-- ==========================================
-- 8. 订单主表 (orders)
-- ==========================================
INSERT INTO orders (id, user_id, address_id, total_amount, pay_amount, status, payment_type, created_at) VALUES 
(1, 3, 1, 699.00, 699.00, 3, 2, '2023-10-01 10:00:00'), 
(2, 4, 3, 899.00, 899.00, 0, NULL, NOW()),               
(3, 3, 2, 699.00, 699.00, 2, 1, '2023-10-15 14:30:00'); 

-- ==========================================
-- 9. 订单详情快照表 (order_item)
-- ==========================================
INSERT INTO order_item (id, order_id, product_id, sku_id, product_name, sku_name, price, quantity, total_price, created_at) VALUES 
(1, 1, 10001, 1, 'Nike Air Zoom Pegasus 39 登月跑步鞋', '黑白基础色-42码', 699.00, 1, 699.00, '2023-10-01 10:00:00'),
(3, 2, 10002, 3, 'Adidas Trae Young 1 吹杨一代篮球鞋', '冰雪蓝-42.5码', 899.00, 1, 899.00, NOW()),
(4, 3, 10001, 2, 'Nike Air Zoom Pegasus 39 登月跑步鞋', '黑白基础色-43码', 699.00, 1, 699.00, '2023-10-15 14:30:00');

-- ==========================================
-- 10. 支付记录 (payment)
-- ==========================================
INSERT INTO payment (id, order_id, user_id, amount, payment_method, trade_no, status, created_at) VALUES 
(1, 1, 3, 699.00, 2, 'ALIPAY202310018888999901', 1, '2023-10-01 10:05:00'),
(2, 3, 3, 699.00, 1, 'WXPAY202310156666777702', 1, '2023-10-15 14:32:00');

-- ==========================================
-- 11. 物流表 (order_shipping)
-- ==========================================
INSERT INTO order_shipping (id, order_id, courier_company, tracking_no, status, shipped_at, delivered_at) VALUES 
(1, 1, '顺丰速运', 'SF1234567890123', '已签收', '2023-10-01 15:00:00', '2023-10-03 11:30:00'),
(2, 3, '京东物流', 'JD0000098765432', '运输中', '2023-10-16 09:00:00', NULL);

-- ==========================================
-- 12. 评价表 (review)
-- ==========================================
INSERT INTO review (id, user_id, product_id, rating, content, created_at) VALUES 
(1, 3, 10001, 5, '鞋子非常轻便，踩屎感很强，今天去操场跑了10公里，膝盖一点都不疼，正品好评！', '2023-10-05 20:15:00'),
(2, 3, 10002, 4, '手感不错，吸湿材质名不虚传，但是刚买回来稍微有点滑，需要磨合一下。', '2023-10-06 10:00:00');

-- ==========================================
-- 13. 促销/秒杀活动 (promotion)
-- ==========================================
INSERT INTO promotion (id, product_id, sku_id, promo_price, stock, start_time, end_time) VALUES 
(1, 10001, 1, 599.00, 100, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY)), 
(2, 10002, 3, 799.00, 20, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY));