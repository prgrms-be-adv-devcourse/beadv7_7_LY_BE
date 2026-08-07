package site.auctionservice.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.common.exception.GlobalExceptionHandler;
import site.common.web.MemberIdWebMvcConfig;
import site.auctionservice.application.AuctionService;
import site.auctionservice.application.dto.AuctionResult;
import site.auctionservice.application.dto.CreateAuctionCommand;
import site.auctionservice.presentation.dto.AuctionRequest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
@Import({MemberIdWebMvcConfig.class, GlobalExceptionHandler.class})
class AuctionControllerMemberIdTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuctionService auctionService;

    private final AuctionRequest request = new AuctionRequest(
            100L,
            "MINT",
            "충분히 긴 상품 설명입니다.",
            List.of("1.png"),
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(3_000),
            BigDecimal.valueOf(500),
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0),
            true,
            5
    );

    @Test
    @DisplayName("X-Member-Id 헤더 값이 sellerId로 서비스에 전달된다")
    void testCreateAuction_headerValue_passedAsSellerId() throws Exception {
        // given
        given(auctionService.createAuction(any(CreateAuctionCommand.class), eq(42L)))
                .willReturn(new AuctionResult(1L, "SCHEDULED"));

        // when & then
        mockMvc.perform(post("/api/v1/auctions")
                        .header("X-Member-Id", "42")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auctionId").value(1))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("X-Member-Id 헤더가 없으면 400을 반환하고 서비스는 호출되지 않는다")
    void testCreateAuction_missingHeader_returnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auctions")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
