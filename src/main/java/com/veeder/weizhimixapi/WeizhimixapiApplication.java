package com.veeder.weizhimixapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WeizhimixapiApplication {


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("path_route", r -> r.path("/vrapi/**")
                        .uri("http://localhost:8080"))
                .route("host_route", r -> r.host("*.myhost.org")
                        .uri("http://httpbin.org"))
                .route("rewrite_route", r -> r.host("*.rewrite.org")
                        .filters(f -> f.rewritePath("/foo/(?<segment>.*)",
                                "/${segment}"))
                        .uri("http://httpbin.org"))
                .route("websocket_route", r -> r.path("/echo")
                        .uri("ws://localhost:8080"))
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(WeizhimixapiApplication.class, args);
    }

}
