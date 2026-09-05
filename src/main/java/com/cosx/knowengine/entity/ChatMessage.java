package com.cosx.knowengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.cosx.knowengine.common.enums.MessageType;

public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;


    private String conversationId;

    private String question;

    private String convertedQuestion;

    private String documentIds;

    private String sectionIds;


    private MessageType status;

    private String userId;

}
