package com.lojapp.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O Swagger fica fechado em produção. Este teste guarda o contrato em {@code docs/openapi/openapi.json}
 * para quem avalia a API sem subir o serviço. Para atualizar o ficheiro: {@code UPDATE_OPENAPI=true}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentTest {

    private static final Path SPEC = Path.of("docs/openapi/openapi.json");

    @Autowired private MockMvc mockMvc;

    @Test
    void publishedSpecMatchesRuntimeDocument() throws Exception {
        String body =
                mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        JsonNode actual = mapper.readTree(body);
        assertThat(actual.path("openapi").asText()).startsWith("3.");
        JsonNode paths = actual.path("paths");
        assertThat(paths.has("/api/v1/auth/login")).isTrue();
        assertThat(paths.has("/api/v1/lojapp/nfe/import")).isTrue();
        assertThat(paths.has("/api/v1/lojapp/pos/sales/finalize")).isTrue();

        if ("true".equalsIgnoreCase(System.getenv("UPDATE_OPENAPI")) || Files.notExists(SPEC)) {
            Files.createDirectories(SPEC.getParent());
            mapper.writeValue(SPEC.toFile(), actual);
        }

        assertThat(SPEC).exists();
        JsonNode published = mapper.readTree(SPEC.toFile());
        assertThat(published).isEqualTo(actual);
    }
}
