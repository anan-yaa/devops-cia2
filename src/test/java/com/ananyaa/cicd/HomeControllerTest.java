package com.ananyaa.cicd;

import com.ananyaa.cicd.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.data.file=${java.io.tmpdir}/expense-tracker-controller-test.json")
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TransactionRepository repository;

    @BeforeEach
    void clearTransactions() { repository.deleteAll(); }

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

    @Test
    void createTransactionPersistsAndReturnsCreated() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/transactions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":125.50,"type":"INCOME","category":"Salary","description":"Monthly pay","date":"2026-11-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(125.50))
                .andExpect(jsonPath("$.type").value("INCOME"));
        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void invalidPostReturnsBadRequestWithFieldErrors() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/transactions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":0,"type":"EXPENSE","category":" ","date":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists())
                .andExpect(jsonPath("$.fields.category").exists())
                .andExpect(jsonPath("$.fields.date").exists());
        org.assertj.core.api.Assertions.assertThat(repository.count()).isZero();
    }

    @Test
    void getAllAndFilterByTypeAndCategory() throws Exception {
        create("40.00", "INCOME", "Salary", "2026-11-01");
        create("12.25", "EXPENSE", "Food", "2026-11-02");
        create("5.00", "EXPENSE", "Food", "2026-11-03");

        mvc.perform(get("/api/transactions"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[1].category").value("Food"))
                .andExpect(jsonPath("$[2].category").value("Salary"));
        mvc.perform(get("/api/transactions").param("type", "INCOME"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[1]").doesNotExist());
        mvc.perform(get("/api/transactions").param("category", "food"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[1].category").value("Food"))
                .andExpect(jsonPath("$[2]").doesNotExist());
    }

    @Test
    void invalidTransactionTypeFilterReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/transactions").param("type", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void getByIdUpdateDeleteAndMissingIdUseExpectedStatuses() throws Exception {
        long id = create("18.75", "EXPENSE", "Books", "2026-11-04");
        mvc.perform(get("/api/transactions/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(18.75));
        mvc.perform(get("/api/transactions/999999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Not found"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/transactions/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":20.00,"type":"EXPENSE","category":"Education","description":"Textbook","date":"2026-11-05"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(20.00))
                .andExpect(jsonPath("$.category").value("Education"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/transactions/{id}", id))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void dashboardTotalsReflectUpdatesAndDeletes() throws Exception {
        long incomeId = create("1000.00", "INCOME", "Salary", "2026-11-01");
        long expenseId = create("200.00", "EXPENSE", "Rent", "2026-11-02");
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpenses").value(200.00))
                .andExpect(jsonPath("$.currentBalance").value(800.00))
                .andExpect(jsonPath("$.recentTransactions.length()").value(2));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/transactions/{id}", incomeId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":900.00,"type":"INCOME","category":"Salary","date":"2026-11-01"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.totalIncome").value(900.00))
                .andExpect(jsonPath("$.currentBalance").value(700.00));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/transactions/{id}", expenseId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.totalIncome").value(900.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.currentBalance").value(900.00));
    }

    private long create(String amount, String type, String category, String date) throws Exception {
        String response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/transactions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%s,"type":"%s","category":"%s","date":"%s"}
                                """.formatted(amount, type, category, date)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
    }
}
