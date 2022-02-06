package ru.ohapegor.widgets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.ohapegor.widgets.config.WidgetServiceProps;

@SpringBootApplication
@EnableTransactionManagement
@EnableConfigurationProperties(WidgetServiceProps.class)
public class WidgetsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WidgetsServiceApplication.class, args);
    }

}
