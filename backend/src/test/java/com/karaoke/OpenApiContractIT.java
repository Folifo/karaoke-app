package com.karaoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/** Garde le contrat docs/openapi.yaml aligné avec les routes réellement exposées. */
class OpenApiContractIT extends IntegrationTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @SuppressWarnings("unchecked")
    void everyImplementedRouteIsDocumented() throws Exception {
        Map<String, Map<String, Object>> paths;
        try (InputStream in = new ClassPathResource("static/openapi.yaml").getInputStream()) {
            paths = (Map<String, Map<String, Object>>) new Yaml().<Map<String, Object>>load(in).get("paths");
        }

        List<String> routes = new ArrayList<>();
        handlerMapping.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().stream()
                .filter(pattern -> pattern.startsWith("/api/"))
                .forEach(pattern -> info.getMethodsCondition().getMethods()
                        .forEach(method -> routes.add(method.name().toLowerCase(Locale.ROOT) + " " + pattern))));

        assertThat(routes).as("routes de l'API").contains("get /api/v1/me", "post /api/v1/auth/login");
        assertThat(routes).as("routes absentes de docs/openapi.yaml").allSatisfy(route -> {
            String[] parts = route.split(" ");
            assertThat(paths.get(parts[1])).as(route).isNotNull().containsKey(parts[0]);
        });
    }

    @Test
    void contractAndSwaggerUiArePublic() throws Exception {
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
