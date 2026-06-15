package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.server.entity.AiChatMessage;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {}
