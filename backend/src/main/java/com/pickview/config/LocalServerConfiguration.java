package com.pickview.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalServerConfiguration {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> configureLocalConnector() {
        // NIO2 avoids the JDK 25 Windows Unix-domain selector loopback failure.
        return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
    }
}
