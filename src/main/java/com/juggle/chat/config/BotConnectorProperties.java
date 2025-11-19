package com.juggle.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "bot.connector")
@Getter
@Setter
public class BotConnectorProperties {
    /**
     * Base domain of the bot connector service, e.g. https://bots.example.com.
     */
    private String domain;
}
