package com.ananyaa.cicd;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void homePageReturnsMessage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(HomeController.MESSAGE)));
    }

    @Test
    void infoEndpointReturnsJson() throws Exception {
        mvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(HomeController.MESSAGE))
                .andExpect(jsonPath("$.imageTag").exists());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
