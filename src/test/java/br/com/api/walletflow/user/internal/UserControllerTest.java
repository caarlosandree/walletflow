package br.com.api.walletflow.user.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String body(String email, String document, String type) {
        return """
                {"fullName":"Maria Silva","document":"%s","email":"%s","password":"secret","type":"%s"}
                """.formatted(document, email, type);
    }

    @Test
    void cadastraUsuarioComumComStatus201() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("maria@example.com", "111.444.777-35", "COMMON")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.document").value("11144477735"));
    }

    @Test
    void rejeitaEmailDuplicadoComStatus409() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("dup@example.com", "111.444.777-35", "COMMON")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("dup@example.com", "11.222.333/0001-81", "MERCHANT")))
                .andExpect(status().isConflict());
    }

    @Test
    void rejeitaDocumentoInvalidoComStatus400() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("bad@example.com", "123.456.789-00", "COMMON")))
                .andExpect(status().isBadRequest());
    }
}
