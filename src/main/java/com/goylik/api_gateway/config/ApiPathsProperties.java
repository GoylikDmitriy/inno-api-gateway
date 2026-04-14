package com.goylik.api_gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway")
@Getter @Setter
public class ApiPathsProperties {
    private List<String> publicPaths = new ArrayList<>();
    private List<String> internalPaths = new ArrayList<>();
}
