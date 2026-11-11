package com.vovan4ok.appliance.store.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

@Configuration
public class MainConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("locale");
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        // ?lang=<anything else> keeps the current language instead of failing the request
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // The public recommendations endpoint reads ?lang itself; it must not set a locale cookie on a response
        // that browsers and caches share
        registry.addInterceptor(localeChangeInterceptor()).excludePathPatterns("/api/v1/recommendations");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
        // The embeddable widget (built from widget/ by `npm run build` or the Docker build). Other websites load
        // it on every page view, so browsers may keep it an hour; the file name has no version, so not longer
        registry.addResourceHandler("/widget/**")
                .addResourceLocations("classpath:/static/widget/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }


    // Replaces SessionFlashMapManager so Spring MVC never touches HttpSession.
    // We use addAttribute() (URL params) for redirects, never addFlashAttribute().
    @Bean
    public FlashMapManager flashMapManager() {
        return new FlashMapManager() {
            @Override
            public FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response) {
                return null;
            }
            @Override
            public void saveOutputFlashMap(FlashMap flashMap,
                    HttpServletRequest request, HttpServletResponse response) {
            }
        };
    }
}
