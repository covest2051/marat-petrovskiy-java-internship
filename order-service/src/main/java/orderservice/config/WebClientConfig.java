package orderservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("http://user-service:8080")
    private String userServiceUrl;

    @Bean
    public WebClient userWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(userServiceUrl)
                .filter((request, next) -> {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                    if (attributes != null) {
                        HttpServletRequest currentRequest = attributes.getRequest();
                        String authHeader = currentRequest.getHeader("Authorization");

                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            ClientRequest filtered = ClientRequest.from(request)
                                    .header("Authorization", authHeader)
                                    .build();
                            return next.exchange(filtered);
                        }
                    }
                    return next.exchange(request);
                })
                .build();
    }
}

