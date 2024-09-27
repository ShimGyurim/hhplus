package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class TddIntegTest {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    @Autowired
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("정상적으로 조회 성공")
    public void lookup_shouldReturnUserPoint() {
        long userId = 1L;
        UserPoint expectedUserPoint = new UserPoint(userId, 0, 0L);
        Mockito.when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);
        UserPoint actualUserPoint = pointService.lookup(userId);
        Assertions.assertEquals(expectedUserPoint.id(), actualUserPoint.id());
        Mockito.verify(userPointTable).selectById(userId);
    }
    @Test
    @DisplayName("동시성 통합 테스트 : 0에서 시작하여 100을 두개의 쓰레드로 각각 10번 loop : 결과 2000원 충전")
    public void concurrentChargeTest() throws InterruptedException {

        long userId = 1L;

        int threadCount = 2;
        int iterationCount = 10;

        UserPoint expectedUserPoint = new UserPoint(userId, 0, 0L);
        Mockito.when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * iterationCount);

        ThreadLocal<Integer> threadNumber = new ThreadLocal<>();
        AtomicInteger globalThreadCount = new AtomicInteger(1);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                int currentThreadNumber = globalThreadCount.getAndIncrement();
                threadNumber.set(currentThreadNumber);

                for (int j = 0; j < iterationCount; j++) {
                    UserPoint userPoint = pointService.charge(userId, 100L);
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
        executorService.shutdown();
        Assertions.assertEquals(pointService.lookup(userId).id(),userId);
        Assertions.assertEquals(pointService.lookup(userId).point(),2000);
    }
}
