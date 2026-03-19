package authenticationservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginBadCredentialsCounter;
    private final Counter registerSuccessCounter;
    private final Timer loginTimer;

    public AuthMetrics(MeterRegistry registry) {
        this.loginSuccessCounter = Counter.builder("authservice.login.success")
                .description("Успешные входы")
                .register(registry);

        this.loginBadCredentialsCounter = Counter.builder("authservice.login.bad_credentials")
                .description("Неверный пароль при входе")
                .register(registry);

        this.registerSuccessCounter = Counter.builder("authservice.register.success")
                .description("Успешные регистрации новых пользователей")
                .register(registry);

        this.loginTimer = Timer.builder("authservice.login.duration")
                .description("Время выполнения операции login (включая bcrypt и запись в БД)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incrementLoginSuccess()        { loginSuccessCounter.increment(); }
    public void incrementLoginBadCredentials() { loginBadCredentialsCounter.increment(); }
    public void incrementRegisterSuccess()     { registerSuccessCounter.increment(); }
    public Timer loginTimer()                  { return loginTimer; }
}