package site.common.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@ConditionalOnClass(WebMvcConfigurer.class)
@Configuration
public class MemberIdWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new MemberIdArgumentResolver());
    }
}
