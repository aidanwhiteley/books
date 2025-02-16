package com.aidanwhiteley.books.controller.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;

// Taken from https://stackoverflow.com/questions/48732894/spring-mvc-and-thymeleaf-resource-versioning
@Configuration
public class TemplateEngineConfig {
    @Autowired
    public void configureTemplateEngine(SpringTemplateEngine engine,
                                        ResourceUrlProvider urlProvider) {
        engine.setLinkBuilder(new VersioningLinkBuilder(urlProvider));
    }
}

class VersioningLinkBuilder extends StandardLinkBuilder {
    private final ResourceUrlProvider urlProvider;

    VersioningLinkBuilder(ResourceUrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    @Override
    public String processLink(IExpressionContext context, String link) {
        String lookedUpLink = urlProvider.getForLookupPath(link);
        if (lookedUpLink != null) {
            return super.processLink(context, lookedUpLink);
        } else {
            return super.processLink(context, link);
        }
    }
}
