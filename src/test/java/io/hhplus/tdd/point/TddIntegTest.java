package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class TddTonghapTest {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    @Autowired
    PointService pointService;

    @Test
    public void concurrentChargeTest() throws InterruptedException {
//        PointService pointService = new PointService();

        long id = 1L;

        int threadCount = 2;
        int iterationCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * iterationCount);

        ThreadLocal<Integer> threadNumber = new ThreadLocal<>();
        AtomicInteger globalThreadCount = new AtomicInteger(1);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                int currentThreadNumber = globalThreadCount.getAndIncrement();
                threadNumber.set(currentThreadNumber);

                for (int j = 0; j < iterationCount; j++) {
                    UserPoint userPoint = pointService.charge(1L, 100L);
                    log.debug(userPoint+" 스레드"+threadNumber.get()+"의"+j+"번째"); // 예시: 사용자 ID 1, 충전 금액 100
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
        executorService.shutdown();
        System.out.println("test:"+pointService.lookup(1L));
    }
}
