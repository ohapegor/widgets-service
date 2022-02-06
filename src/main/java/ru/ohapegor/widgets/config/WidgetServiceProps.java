package ru.ohapegor.widgets.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "widgets")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WidgetServiceProps {
    @Positive
    private int readTimeoutMs;
    @Positive
    private int writeTimeoutMs;
    @NotBlank
    private String repository;
}
