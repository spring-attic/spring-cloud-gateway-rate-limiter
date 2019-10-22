package org.springframework.cloud.gateway.ratelimiter;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;

public class HeaderKeyResolver implements KeyResolver {

    private final String headerName;

    public HeaderKeyResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(headerName));
    }
}
