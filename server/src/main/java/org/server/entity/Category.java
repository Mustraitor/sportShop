package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO) // 设置主键为数据库自增
    private Long id;

    private String name;

    private Long parentId;

    private Integer sort;

    @TableLogic
    private Integer isDeleted; // 0=未删除，1=已删除

    private LocalDateTime createdAt;
}