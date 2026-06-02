package com.lojapp.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiVersioningConventionTest {

    @Test
    void restControllers_mustUseApiV1Prefix() throws Exception {
        List<String> offenders = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        for (var beanDef : scanner.findCandidateComponents("com.lojapp.controller")) {
            Class<?> clazz = Class.forName(beanDef.getBeanClassName());
            RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
            if (mapping == null || mapping.value().length == 0) {
                offenders.add(clazz.getName() + " (sem @RequestMapping base)");
                continue;
            }
            String basePath = mapping.value()[0];
            if (!basePath.startsWith("/api/v1")) {
                offenders.add(clazz.getName() + " -> " + basePath);
            }
        }
        assertThat(offenders)
                .as("Todos os @RestController devem usar prefixo /api/v1")
                .isEmpty();
    }
}
