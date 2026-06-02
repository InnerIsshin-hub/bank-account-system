package com.student.bank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullRegisterLoginTransferRecordAndSecurityFlow() throws Exception {
        JsonNode unauthorized = performJson(get("/api/account/list"), null);
        assertThat(unauthorized.path("code").asInt()).isEqualTo(40101);

        String aliceAccount = register("Alice", "110101199003071234", "13800000001");
        JsonNode duplicate = postJson("/api/user/register", Map.of(
                "userName", "Alice2",
                "idCard", "110101199003071234",
                "phone", "13800000009",
                "password", "Password@123",
                "tradePassword", "123456"
        ), null);
        assertThat(duplicate.path("code").asInt()).isEqualTo(40001);

        String bobAccount = register("Bob", "110101199003071235", "13800000002");
        String aliceToken = login("110101199003071234", "Password@123");

        JsonNode badLogin = postJson("/api/user/login", Map.of(
                "idCard", "110101199003071234",
                "password", "Wrong@123"
        ), null);
        assertThat(badLogin.path("code").asInt()).isEqualTo(40101);

        JsonNode list = performJson(get("/api/account/list"), aliceToken);
        assertThat(list.path("code").asInt()).isEqualTo(200);
        assertThat(list.path("data").get(0).path("accountNumber").asText()).isEqualTo(aliceAccount);

        JsonNode forbiddenBalance = performJson(get("/api/account/" + bobAccount + "/balance"), aliceToken);
        assertThat(forbiddenBalance.path("code").asInt()).isEqualTo(40301);

        JsonNode transfer = postJson("/api/account/transfer", Map.of(
                "fromAccount", aliceAccount,
                "toAccount", bobAccount,
                "amount", "100.00",
                "remark", "test transfer",
                "tradePassword", "123456",
                "idempotencyKey", "idem-alice-bob-001"
        ), aliceToken);
        assertThat(transfer.path("code").asInt()).isEqualTo(200);
        assertThat(transfer.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(new BigDecimal(transfer.path("data").path("latestBalance").asText())).isEqualByComparingTo("9900.00");

        JsonNode repeated = postJson("/api/account/transfer", Map.of(
                "fromAccount", aliceAccount,
                "toAccount", bobAccount,
                "amount", "100.00",
                "remark", "test transfer",
                "tradePassword", "123456",
                "idempotencyKey", "idem-alice-bob-001"
        ), aliceToken);
        assertThat(repeated.path("code").asInt()).isEqualTo(200);
        assertThat(new BigDecimal(repeated.path("data").path("latestBalance").asText())).isEqualByComparingTo("9900.00");

        JsonNode records = performJson(get("/api/records").param("accountNumber", aliceAccount), aliceToken);
        assertThat(records.path("code").asInt()).isEqualTo(200);
        assertThat(records.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode insufficient = postJson("/api/account/transfer", Map.of(
                "fromAccount", aliceAccount,
                "toAccount", bobAccount,
                "amount", "1000000.00",
                "remark", "too much",
                "tradePassword", "123456",
                "idempotencyKey", "idem-alice-bob-002",
                "otpCode", "000000"
        ), aliceToken);
        assertThat(insufficient.path("code").asInt()).isNotEqualTo(200);
        JsonNode balance = performJson(get("/api/account/" + aliceAccount + "/balance"), aliceToken);
        assertThat(new BigDecimal(balance.path("data").asText())).isEqualByComparingTo("9900.00");

        JsonNode wrongOldPassword = putJson("/api/user/password", Map.of(
                "oldPassword", "BadPassword@123",
                "newPassword", "NewPassword@123"
        ), aliceToken);
        assertThat(wrongOldPassword.path("code").asInt()).isEqualTo(40301);

        JsonNode changed = putJson("/api/user/password", Map.of(
                "oldPassword", "Password@123",
                "newPassword", "NewPassword@123"
        ), aliceToken);
        assertThat(changed.path("code").asInt()).isEqualTo(200);

        JsonNode oldTokenRejected = performJson(get("/api/account/list"), aliceToken);
        assertThat(oldTokenRejected.path("code").asInt()).isEqualTo(40101);
        assertThat(login("110101199003071234", "NewPassword@123")).isNotBlank();
    }

    @Test
    void concurrentTransferNeverMakesBalanceNegative() throws Exception {
        String from = register("Carol", "110101199003071236", "13800000003");
        String to = register("Dave", "110101199003071237", "13800000004");
        String token = login("110101199003071236", "Password@123");

        var pool = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            int index = i;
            calls.add(() -> postJson("/api/account/transfer", Map.of(
                    "fromAccount", from,
                    "toAccount", to,
                    "amount", "1000.00",
                    "remark", "concurrent " + index,
                    "tradePassword", "123456",
                    "idempotencyKey", "idem-concurrent-" + index
            ), token).path("code").asInt());
        }
        List<Integer> codes = pool.invokeAll(calls).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        pool.shutdown();

        long success = codes.stream().filter(code -> code == 200).count();
        assertThat(success).isLessThanOrEqualTo(10);
        JsonNode balance = performJson(get("/api/account/" + from + "/balance"), token);
        assertThat(new BigDecimal(balance.path("data").asText())).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private String register(String name, String idCard, String phone) throws Exception {
        JsonNode result = postJson("/api/user/register", Map.of(
                "userName", name,
                "idCard", idCard,
                "phone", phone,
                "password", "Password@123",
                "tradePassword", "123456"
        ), null);
        assertThat(result.path("code").asInt()).isEqualTo(200);
        return result.path("data").asText();
    }

    private String login(String idCard, String password) throws Exception {
        JsonNode result = postJson("/api/user/login", Map.of(
                "idCard", idCard,
                "password", password
        ), null);
        assertThat(result.path("code").asInt()).isEqualTo(200);
        return result.path("data").path("accessToken").asText();
    }

    private JsonNode postJson(String path, Object body, String token) throws Exception {
        return performJson(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), token);
    }

    private JsonNode putJson(String path, Object body, String token) throws Exception {
        return performJson(put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), token);
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String token) throws Exception {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
