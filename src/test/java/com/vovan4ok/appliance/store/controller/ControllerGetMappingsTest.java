package com.vovan4ok.appliance.store.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET requests must never change data. A GET is what a plain link, an image tag or a prefetch sends, and the
 * login cookie (SameSite=Lax) still goes along when someone follows a link from another website: a GET that
 * deletes or approves could be triggered by a booby-trapped link. The read-only demo filter also relies on it.
 * Changing actions are POST forms instead, whose cross-site requests arrive without the cookie.
 */
class ControllerGetMappingsTest {

    // Last path segment that names an action rather than a page
    private static final Pattern ACTION = Pattern.compile(
            ".*/(delete|remove|approved?|unapproved?|cancel|checkout|logout|update|save|reset)/?$");

    @Test
    void noGetHandler_isAnActionThatChangesData() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (Class<?> controller : controllers()) {
            String[] prefixes = classPaths(controller);
            for (Method method : controller.getDeclaredMethods()) {
                GetMapping get = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
                if (get == null) {
                    continue;
                }
                for (String prefix : prefixes) {
                    for (String path : paths(get.path())) {
                        if (ACTION.matcher(prefix + path).matches()) {
                            offenders.add(controller.getSimpleName() + "." + method.getName() + " GET " + prefix + path);
                        }
                    }
                }
            }
        }

        assertThat(offenders).as("GET handlers that look like actions; make them POST forms").isEmpty();
    }

    @Test
    void scan_findsTheControllers() throws Exception {
        // Guards the test above against silently scanning nothing
        assertThat(controllers()).extracting(Class::getSimpleName)
                .contains("OrdersController", "ApplianceController", "ApplianceApiController");
    }

    private static List<Class<?>> controllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.vovan4ok.appliance.store.controller")) {
            classes.add(ClassUtils.forName(definition.getBeanClassName(), null));
        }
        return classes;
    }

    private static String[] classPaths(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        return mapping == null || mapping.path().length == 0 ? new String[] {""} : mapping.path();
    }

    private static String[] paths(String[] declared) {
        return declared.length == 0 ? new String[] {""} : declared;
    }
}
