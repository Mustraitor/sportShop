USE sport_shop;

-- ==========================================
-- 1. 补充测试买家用户 (接续你原有的 sys_user)
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
-- 3. 商品分类表 (category) - 支持两级/三级分类
-- ==========================================
INSERT INTO category (id, name, parent_id, sort, created_at) VALUES 
(1, '运动鞋靴', 0, 1, NOW()),
(2, '跑步鞋', 1, 1, NOW()),
(3, '篮球鞋', 1, 2, NOW()),
(4, '运动服饰', 0, 2, NOW()),
(5, '速干T恤', 4, 1, NOW()),
(6, '运动短裤', 4, 2, NOW()),
(7, '运动装备', 0, 3, NOW()),
(8, '篮球/足球', 7, 1, NOW()),
(9, '瑜伽器械', 7, 2, NOW());

-- ==========================================
-- 4. 商品表 (product)
-- ==========================================
INSERT INTO product (id, category_id, name, description, main_image, price, stock, status, created_at) VALUES 
(1, 2, 'Nike Air Zoom Pegasus 39 登月跑步鞋', '透气缓震，适合日常慢跑及中长跑训练。', 'https://dummyimage.com/400x400/000/fff&text=Nike+Pegasus39', 699.00, 500, 1, NOW()),
(2, 3, 'Adidas Trae Young 1 吹杨一代篮球鞋', '极度防滑，专为后卫打造的实战利器。', 'https://dummyimage.com/400x400/000/fff&text=Adidas+Trae1', 899.00, 200, 1, NOW()),
(3, 5, 'Under Armour 安德玛男士速干运动T恤', 'HeatGear面料，排汗透气，高弹力紧身。', 'https://dummyimage.com/400x400/000/fff&text=UA+T-Shirt', 199.00, 1000, 1, NOW()),
(4, 8, 'Spalding 斯伯丁7号标准比赛篮球', '优质PU吸湿材质，室内外通用，手感极佳。', 'https://dummyimage.com/400x400/000/fff&text=Spalding+Ball', 159.00, 350, 1, NOW()),
(5, 9, 'Lululemon 露露乐蒙 The Reversible 瑜伽垫', '5mm加厚双面防滑，天然橡胶环保材质。', 'https://dummyimage.com/400x400/000/fff&text=Lululemon+Mat', 450.00, 120, 1, NOW());

-- ==========================================
-- 5. 商品SKU表 (product_sku)
-- ==========================================
INSERT INTO product_sku (id, product_id, sku_name, price, stock, sku_code, created_at) VALUES 
(1, 1, '黑白基础色-42码', 699.00, 100, 'NK-AP39-BLK-42', NOW()),
(2, 1, '黑白基础色-43码', 699.00, 80, 'NK-AP39-BLK-43', NOW()),
(3, 1, '荧光绿限定款-42码', 749.00, 50, 'NK-AP39-GRN-42', NOW()),
(4, 2, '冰雪蓝-42.5码', 899.00, 50, 'AD-TY1-BLU-425', NOW()),
(5, 3, '藏青色-L码', 199.00, 500, 'UA-TS-NAV-L', NOW()),
(6, 3, '藏青色-XL码', 199.00, 500, 'UA-TS-NAV-XL', NOW()),
(7, 4, '标准7号-棕色(送气筒)', 159.00, 350, 'SP-BALL-7-BRN', NOW()),
(8, 5, '莫兰迪粉-5mm', 450.00, 60, 'LU-MAT-PNK-5', NOW()),
(9, 5, '星空黑-5mm', 450.00, 60, 'LU-MAT-BLK-5', NOW());

-- ==========================================
-- 6. 商品图片轮播表 (product_image)
-- ==========================================
INSERT INTO product_image (product_id, url, sort, created_at) VALUES 
(1, 'https://dummyimage.com/800x800/000/fff&text=Nike+Pic1', 1, NOW()),
(1, 'https://dummyimage.com/800x800/000/fff&text=Nike+Pic2', 2, NOW()),
(1, 'https://dummyimage.com/800x800/000/fff&text=Nike+Pic3', 3, NOW()),
(4, 'https://dummyimage.com/800x800/000/fff&text=Spalding+Detail', 1, NOW());

-- ==========================================
-- 7. 购物车 (cart)
-- ==========================================
INSERT INTO cart (id, user_id, product_id, sku_id, quantity, checked, created_at) VALUES 
(1, 3, 5, 8, 1, 1, NOW()),         -- 张三购物车里有1个瑜伽垫
(2, 4, 3, 5, 2, 0, NOW());         -- 李四购物车里有2件T恤（未勾选）

-- ==========================================
-- 8. 订单主表 (orders)
-- 状态说明：0待支付, 1待发货, 2已发货, 3已完成, 4已取消
-- 支付说明：1微信, 2支付宝, 3银行卡
-- ==========================================
INSERT INTO orders (id, user_id, address_id, total_amount, pay_amount, status, payment_type, created_at) VALUES 
(1, 3, 1, 858.00, 858.00, 3, 2, '2023-10-01 10:00:00'), -- 张三：已完成订单（买了鞋和篮球）
(2, 4, 3, 899.00, 899.00, 0, NULL, NOW()),              -- 李四：待支付订单（买了篮球鞋）
(3, 3, 2, 199.00, 199.00, 2, 1, '2023-10-15 14:30:00'); -- 张三：已发货订单（买了T恤）

-- ==========================================
-- 9. 订单详情快照表 (order_item)
-- ==========================================
INSERT INTO order_item (id, order_id, product_id, sku_id, product_name, sku_name, price, quantity, total_price, created_at) VALUES 
-- 订单1的商品明细
(1, 1, 1, 1, 'Nike Air Zoom Pegasus 39 登月跑步鞋', '黑白基础色-42码', 699.00, 1, 699.00, '2023-10-01 10:00:00'),
(2, 1, 4, 7, 'Spalding 斯伯丁7号标准比赛篮球', '标准7号-棕色(送气筒)', 159.00, 1, 159.00, '2023-10-01 10:00:00'),
-- 订单2的商品明细
(3, 2, 2, 4, 'Adidas Trae Young 1 吹杨一代篮球鞋', '冰雪蓝-42.5码', 899.00, 1, 899.00, NOW()),
-- 订单3的商品明细
(4, 3, 3, 5, 'Under Armour 安德玛男士速干运动T恤', '藏青色-L码', 199.00, 1, 199.00, '2023-10-15 14:30:00');

-- ==========================================
-- 10. 支付记录 (payment)
-- ==========================================
INSERT INTO payment (id, order_id, user_id, amount, payment_method, trade_no, status, created_at) VALUES 
(1, 1, 3, 858.00, 2, 'ALIPAY202310018888999901', 1, '2023-10-01 10:05:00'),
(2, 3, 3, 199.00, 1, 'WXPAY202310156666777702', 1, '2023-10-15 14:32:00');

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
(1, 3, 1, 5, '鞋子非常轻便，踩屎感很强，今天去操场跑了10公里，膝盖一点都不疼，正品好评！', '2023-10-05 20:15:00'),
(2, 3, 4, 4, '手感不错，吸湿材质名不虚传，但是刚买回来稍微有点滑，需要磨合一下。', '2023-10-06 10:00:00');

-- ==========================================
-- 13. 促销/秒杀活动 (promotion)
-- ==========================================
INSERT INTO promotion (id, product_id, sku_id, promo_price, stock, start_time, end_time) VALUES 
(1, 3, 5, 99.00, 100, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY)), -- 正在进行：UA T恤半价秒杀
(2, 2, 4, 699.00, 20, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY)); -- 即将开始：篮球鞋特价