package org.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisCartItem implements Serializable {
    private Integer quantity; // 数量
    private Integer checked;  // 勾选状态：0未选中，1选中
}