package com.veeder.weizhimixapi.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;

@Component
@Slf4j
public class UserFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI URIPath = request.getURI();
        String path = request.getPath().value();
        String method = request.getMethodValue();
        HttpHeaders header = request.getHeaders();
        log.info("");
        log.info("***********************************请求信息**********************************");
        log.info("请求request信息：URI = {}, path = {}，method = {}，header = {}。", URIPath, path, method, header);
        if ("POST".equals(method)) {
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        try {
                            String bodyString = new String(bytes, "utf-8");
                            log.info("请求参数：" + bodyString);
                            exchange.getAttributes().put("POST_BODY", bodyString);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        DataBufferUtils.release(dataBuffer);
                        Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                            DataBuffer buffer = exchange.getResponse().bufferFactory()
                                    .wrap(bytes);
                            return Mono.just(buffer);
                        });

                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return cachedFlux;
                            }
                        };
                        log.info("****************************************************************************\n");
                        ServerHttpRequest newRequest  = request.mutate()
                                .path(request.getPath() + "/demo").build();

                        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest)
                                .build();
                        return ServerRequest.create(mutatedExchange, HandlerStrategies.withDefaults().messageReaders())
                                .bodyToMono(String.class)
                                .doOnNext(objectValue -> {
                                    //在此处,将body中的params值获取到,并存放在本次请求的attributes属性中,这样就可以在本次请求中的所有地方进行使用了
                                    mutatedExchange.getAttributes().put("CommonConstant.PARAMS", "ParamsUtil.buildParams(mutatedRequest,objectValue)");
                                }).then(chain.filter(mutatedExchange));

//
//                        return chain.filter(exchange.mutate().request(mutatedRequest)
//                                .build());
                    });
        } else if ("GET".equals(method)) {
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            log.info("请求参数：" + queryParams);
            log.info("****************************************************************************\n");
            return chain.filter(exchange);
        }
        log.info("****************************************************************************\n");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
