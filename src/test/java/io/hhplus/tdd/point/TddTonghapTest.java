package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class TddTonghapTest {

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

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < iterationCount; j++) {
                    pointService.charge(1L, 100L); // 예시: 사용자 ID 1, 충전 금액 100
                    System.out.println("test:"+"" +"and"+ j);
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
        executorService.shutdown();
        System.out.println("test:"+pointService.lookup(1L));
    }
}
