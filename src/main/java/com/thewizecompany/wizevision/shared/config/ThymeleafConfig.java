package com.thewizecompany.wizevision.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/*
 * THYMELEAF CONFIGURATION
 *
 * Configures Thymeleaf to process templates as STRING input
 * rather than loading from files.
 *
 * This is different from the default Spring Boot setup where
 * Thymeleaf loads templates from /templates/*.html files.
 *
 * We store templates in the database as strings.
 * StringTemplateResolver processes these strings directly.
 *
 * TemplateMode.HTML means Thymeleaf treats the
 * template content as HTML and processes
 * th:text, ${}, etc. expressions.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine =
                new SpringTemplateEngine();

        StringTemplateResolver resolver =
                new StringTemplateResolver();

        resolver.setTemplateMode(TemplateMode.HTML);

        /*
         * Cache is disabled for string templates.
         * Since templates are stored in DB and can be
         * updated at any time, caching would serve
         * stale content after an update.
         */
        resolver.setCacheable(false);

        engine.setTemplateResolver(resolver);
        return engine;
    }
}