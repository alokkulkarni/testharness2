package app.todo.config;

import app.todo.common.RequestIdFilter;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the request id filter ahead of the Spring Security chain, for the REQUEST and ERROR dispatches, so on both
 * dispatches it runs before and around every other filter (sdd.md section 10). Spring Security's own registration sits
 * at order -100.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig {

    private static final int BEFORE_SECURITY = -200;

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setName("requestIdFilter");
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        registration.setOrder(BEFORE_SECURITY);
        return registration;
    }
}
