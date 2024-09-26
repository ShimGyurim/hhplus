package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.apache.catalina.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

//@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class TddAppTest {

    @Mock
    UserPointTable userPointTable;

    @Mock
    PointHistoryTable pointHistoryTable;

    @InjectMocks
    PointService pointService;

    @Test
    void lookup_id_return_same_value() {
        long id = 1L;

        UserPoint expectedUserPoint = new UserPoint(id, 100, 0);
        Mockito.when(userPointTable.selectById(id)).thenReturn(expectedUserPoint);

        // When
        UserPoint actualUserPoint = pointService.lookup(id);

        // Then
        Assertions.assertEquals(expectedUserPoint, actualUserPoint);
    }

    @Test
    void lookup_id_return_null_fail() {
        // Given
        long id = 1L;
        Mockito.when(userPointTable.selectById(id)).thenReturn(null);

        // When
        UserPoint actualUserPoint = pointService.lookup(id);

        // Then
        Assertions.assertNotNull(actualUserPoint);
    }

    @Test
    public void history_find_success() {
        // given
        long userId = 1L;
        List<PointHistory> expected = new ArrayList<>();

        expected.add(new PointHistory(1L, userId, 0L,TransactionType.CHARGE,0L));
        Mockito.when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expected);

        // when
        List<PointHistory> actual = pointService.history(userId);

        // then
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("검색하는 사용자를 못 찾았을때")
    public void history_find_fail() {
        // given
        long userId = -1L;

        // when, then
        Assertions.assertThrows(RuntimeException.class, () -> pointService.history(userId));
    }

    @Test
    @DisplayName("원래 100 충전량에서 100을 더하는 테스트")
    public void charge_success() {
        // Given
        long id = 1L;
        long amount = 100L;

        UserPoint originUserPoint = new UserPoint(id, 100, 0);
        UserPoint expectedUserPoint = new UserPoint(id, 200, 0);
        Mockito.when(userPointTable.selectById(id)).thenReturn(originUserPoint);
        Mockito.when(userPointTable.insertOrUpdate(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(expectedUserPoint);

        // When
        UserPoint actualUserPoint = pointService.charge(id, amount);

//        System.out.println(expectedUserPoint.id());
        // Then
        Assertions.assertAll(
                () -> Assertions.assertEquals(expectedUserPoint.id(), actualUserPoint.id()),
                () -> Assertions.assertEquals(expectedUserPoint.point(), actualUserPoint.point())
        );

        Mockito.verify(pointHistoryTable).insert(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.eq(TransactionType.CHARGE), ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("원래 100 충전량에서 음수를 입력: 실패")
    public void charge_fail() {
        long id = 1L;
        long amount = -100L;

        UserPoint originUserPoint = new UserPoint(id, 100, 0);
        UserPoint expectedUserPoint = new UserPoint(id, 0, 0);
//        Mockito.when(userPointTable.selectById(id)).thenReturn(originUserPoint);
//        Mockito.when(userPointTable.insertOrUpdate(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(expectedUserPoint);

//        UserPoint actualUserPoint = pointService.charge(id, amount);

        Assertions.assertThrows(IllegalArgumentException.class, () -> pointService.charge(id, amount));
//
//        Assertions.assertEquals(expectedUserPoint.id(), actualUserPoint.id());
//        Assertions.assertEquals(expectedUserPoint.point(), actualUserPoint.point());

//        Mockito.verify(pointHistoryTable).insert(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.eq(TransactionType.CHARGE), ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("음의 충전금액 충전") //TODO : validation
    public void charge_negativeAmount() {
        // Given
        long id = 1L;
        long amount = -100L;
//        UserPoint expectedUserPoint = new UserPoint(id, 100, 0);
//        Mockito.when(userPointTable.selectById(id)).thenReturn(expectedUserPoint);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    pointService.charge(id, amount);
                });
//        // When
//        UserPoint actualUserPoint = pointService.charge(id, amount);
//
//        // Then
//        Assertions.assertEquals(expectedUserPoint, actualUserPoint);
//        Mockito.verify(pointHistoryTable, Mockito.never()).insert(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.eq(TransactionType.CHARGE), ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("음의 사용금액") //TODO : validation
    public void use_negativeAmount() {
        // Given
        long id = 1L;
        long amount = -100L;
//        UserPoint expectedUserPoint = new UserPoint(id, 100, 0);
//        Mockito.when(userPointTable.selectById(id)).thenReturn(expectedUserPoint);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    pointService.use(id, amount);
                });
//        // When
//        UserPoint actualUserPoint = pointService.charge(id, amount);
//
//        // Then
//        Assertions.assertEquals(expectedUserPoint, actualUserPoint);
//        Mockito.verify(pointHistoryTable, Mockito.never()).insert(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.eq(TransactionType.CHARGE), ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("원래 100 충전량에서 200을 사용하는 테스트: 실패")
    public void use_overAmount() {
        // Given
        long id = 1L;
        long amount = 200L;

        UserPoint originUserPoint = new UserPoint(id, 100, 0);
//        UserPoint expectedUserPoint = new UserPoint(id, 200, 0);
        Mockito.when(userPointTable.selectById(id)).thenReturn(originUserPoint);
//        Mockito.when(userPointTable.insertOrUpdate(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(expectedUserPoint);

        // When
//        UserPoint actualUserPoint = pointService.use(id, amount);

        Assertions.assertThrows(RuntimeException.class, () -> pointService.use(id, amount));
//        System.out.println("test: "+actualUserPoint.point());
        // Then
//        Assertions.assertEquals(expectedUserPoint.id(), actualUserPoint.id());
//        Assertions.assertEquals(expectedUserPoint.point(),actualUserPoint.point());
//        Mockito.verify(pointHistoryTable,
//                Mockito.never()).insert(ArgumentMatchers.anyLong(),
//                ArgumentMatchers.anyLong(),
//                ArgumentMatchers.eq(TransactionType.USE),
//                ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("원래 200 충전량에서 100을 사용하는 테스트")
    public void use_success() {
        // Given
        long id = 1L;
        long amount = 100L;

        UserPoint originUserPoint = new UserPoint(id,200L,0);
        UserPoint expectedUserPoint = new UserPoint(id, 100L, 0);
        Mockito.when(userPointTable.selectById(id)).thenReturn(originUserPoint);
        Mockito.when(userPointTable.insertOrUpdate(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(expectedUserPoint);

        // When
        UserPoint actualUserPoint = pointService.use(id, amount);

        // Then
        Assertions.assertEquals(expectedUserPoint, actualUserPoint);
        Mockito.verify(pointHistoryTable).insert(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.eq(TransactionType.USE), ArgumentMatchers.anyLong());
    }

//    @Test
//    public void concurrentChargeTest() throws InterruptedException {
//
//        long id = 1L;
//        long amount = 200L;
//
//        int threadCount = 2;
//        int iterationCount = 10;
//        int initialScore = 0;
//
//        UserPoint userPoint = new UserPoint(id,amount,0);
//        Mockito.when(userPointTable.selectById(ArgumentMatchers.anyLong())).thenAnswer(invocation -> initialScore); //TODO : 나중에 확인
//
//        // 첫 호출 시 initialScore 반환
//        initialScore += 10; // 다음 호출을 위한 initialScore 업데이트
//
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount * iterationCount);
//
//        AtomicInteger chargeCallCount = new AtomicInteger(0);
//
//        for (int i = 0; i < threadCount; i++) {
//            executorService.submit(() -> {
//                for (int j = 0; j < iterationCount; j++) {
//                    pointService.charge(id, amount); // 예시: 사용자 ID 1, 충전 금액 100
//                    chargeCallCount.incrementAndGet();
//                    System.out.println("test:"+"" +"and"+ j);
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
//        executorService.shutdown();
//        int expectedChargeCallCount = threadCount * iterationCount;
//        Assertions.assertEquals(expectedChargeCallCount, chargeCallCount.get());
//    }

//    @Test
//    void threadTest() throws InterruptedException {
////        SomethingCounter count = new SomethingCounter();
//        Mockito.when(userPointTable.findById(1L)).thenReturn(Optional.of(expectedUser));
//
//        Thread thread = new Thread(() -> {
//            System.out.println("thread >>>");
//            for (int i = 0; i < 1000; i++) {
//
//            }
//        });
//        thread.start();
//
//        Thread thread2 = new Thread(() -> {
//            System.out.println("thread >>>");
//            for (int i = 0; i < 1000; i++) {
//                count.count = count.count + 1;
//            }
//        });
//        thread2.start();
//
//        thread.join();
//        thread2.join();
//
//        count.printCount();
//        Assertions.assertEquals(count.printCount(), 2000)
//    }
}


