package userservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }

    @Bean
    public Counter userCreatedCounter(MeterRegistry registry) {
        return Counter.builder("userservice.users.created")
                .description("Total number of users successfully created")
                .tag("service", "user-service")
                .register(registry);
    }

    @Bean
    public Counter cardCreatedCounter(MeterRegistry registry) {
        return Counter.builder("userservice.cards.created")
                .description("Total number of cards successfully created")
                .tag("service", "user-service")
                .register(registry);
    }

    @Bean
    public Counter userNotFoundCounter(MeterRegistry registry) {
        return Counter.builder("userservice.users.not_found")
                .description("Total number of UserNotFoundException occurrences")
                .tag("service", "user-service")
                .register(registry);
    }

    @Bean
    public Counter cardNotFoundCounter(MeterRegistry registry) {
        return Counter.builder("userservice.cards.not_found")
                .description("Total number of CardNotFoundException occurrences")
                .tag("service", "user-service")
                .register(registry);
    }

    @Bean
    public Timer userLookupTimer(MeterRegistry registry) {
        return Timer.builder("userservice.users.lookup.duration")
                .description("Time spent looking up a user from the DB")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("service", "user-service")
                .register(registry);
    }
}