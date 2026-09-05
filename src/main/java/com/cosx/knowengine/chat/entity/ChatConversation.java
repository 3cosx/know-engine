package com.cosx.knowengine.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cosx.knowengine.chat.enums.ConversationStatus;
import com.cosx.knowengine.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("chat_conversation")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatConversation extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String conversationTitle;

    private ConversationStatus status;

    private Long userId;

}
