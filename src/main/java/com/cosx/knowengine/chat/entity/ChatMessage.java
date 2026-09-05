package com.cosx.knowengine.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cosx.knowengine.chat.enums.MessageType;
import com.cosx.knowengine.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String question;

    private String convertedQuestion;

    private String documentIds;

    private String sectionIds;

    private MessageType status;

    private Long userId;

}
