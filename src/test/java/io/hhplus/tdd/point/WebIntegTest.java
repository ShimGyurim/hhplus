package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class WebIntegTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPointTable userPointTable;

    @MockBean
    private PointHistoryTable pointHistoryTable;

    UserPoint initialUserPoint;

    @BeforeEach
    public void setUp() {
        // UserPoint 객체 생성 (초기 point=0)
        Long userId=1L;
        initialUserPoint = new UserPoint(userId, 0L, 0L);
        // UserPointTable.selectById 모의 설정 (항상 initialUserPoint 반환)
        Mockito.when(userPointTable.selectById(userId)).thenReturn(initialUserPoint);

        List<PointHistory> expected = new ArrayList<>();
        expected.add(new PointHistory(1L, userId, 0L,TransactionType.CHARGE,0L));
        Mockito.when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expected);
    }

    @Test
    @DisplayName("0으로 잘 나오는지 확인")
    public void lookup_correct_zero() throws Exception {
        long userId = 1L;
        long amount = 100L;

        // PointService 모의 설정 (charge 메서드 호출 시 특정 값 반환)
        mockMvc.perform(MockMvcRequestBuilders.get("/point/" + userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("회원 내역 기본 내역 확인")
    public void lookup_histories() throws Exception {
        long userId = 1L;
//        long amount = 100L;

        // PointService 모의 설정 (charge 메서드 호출 시 특정 값 반환)
        mockMvc.perform(MockMvcRequestBuilders.get("/point/" + userId+"/histories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1)) // 배열 길이가 1인지 확인
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].userId").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].amount").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("CHARGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].updateMillis").value(0));
    }

    @Test
    @DisplayName("충전 200원 확인") //XXX: talend API 와 다르게 content에 단순 숫자가 들어가면 에러남
    public void charge_correct() throws Exception {
        long userId = 1L;
//        long amount = 100L;

        // PointService 모의 설정 (charge 메서드 호출 시 특정 값 반환)
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/"+userId+"/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("200"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(200));
    }
    
    @Test
    @DisplayName("사용 100원 확인")
    public void use_correct() throws Exception {
        long userId = 1L;
        long amount = 200L;

        initialUserPoint = new UserPoint(userId, amount, 0L);
        // UserPointTable.selectById 모의 설정 (항상 initialUserPoint 반환)
        Mockito.when(userPointTable.selectById(userId)).thenReturn(initialUserPoint);

        // PointService 모의 설정 (charge 메서드 호출 시 특정 값 반환)
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/"+userId+"/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("100"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(100));
    }
}
