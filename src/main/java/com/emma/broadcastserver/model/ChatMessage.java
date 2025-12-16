package com.emma.broadcastserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder(alphabetic = true)
public class ChatMessage {
    private String type;
    @JsonProperty
    private String content;
    private String sender;

    public ChatMessage(){}

    public ChatMessage(String type, String sender, String content) {
        this.type = type;
        this.content = content;
        this.sender = sender;
    }
}
