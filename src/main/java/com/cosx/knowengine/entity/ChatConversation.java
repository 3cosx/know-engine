package com.cosx.knowengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cosx.knowengine.common.enums.ConversationStatus;
import lombok.Data;

@TableName("chat_conversation")
@Data
public class ChatConversation extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private String conversationTitle;

    private ConversationStatus status;

    private String userId;

}
