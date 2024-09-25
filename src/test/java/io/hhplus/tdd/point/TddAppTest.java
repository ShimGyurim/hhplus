package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

//@SpringBootTest
public class tddAppTest {

    @Autowired
    PointService pointService;
    @Test
    void lookup1Return100() {
        UserPoint expectedUserPoint = new UserPoint(id, 100, 0);
        Mockito.when(userPointTable.selectById(id)).thenReturn(expectedUserPoint);

        // When
        UserPoint actualUserPoint = pointService.lookup(id);

        // Then
        assertEquals(expectedUserPoint, actualUserPoint);
    }    

    @Test
    void lookup1ReturnNull() {
        // Given
        long id = 1L;
        Mockito.when(userPointTable.selectById(id)).thenReturn(null);

        // When
        UserPoint actualUserPoint = pointService.lookup(id);

        // Then
        assertNull(actualUserPoint);
    }

@Test
public void from100charge100success() {
    // Given
    long id = 1L;
    long amount = 100L;

    UserPoint expectedOriginUserPoint = new UserPoint(id,100,0);
    UserPoint expectedUserPoint = new UserPoint(id, 200, 0);
    Mockito.when(userPointTable.selectById(id)).thenReturn(expectedOriginUserPoint);
    Mockito.when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenReturn(expectedUserPoint);

    // When
    UserPoint actualUserPoint = pointService.charge(id, amount);

    // Then
    assertAll(
        () -> assertEquals(expectedUserPoint.id,actualUserPoint.id),
        () -> assertEquals(expectedUserPoint.amount,actualUserPoint.amount
    );   
   
    verify(pointHistoryTable).insert(anyLong(), anyLong(), eq(TransactionType.CHARGE), anyLong());
}    

@Test
public void from100() {
    // Given
    long id = 1L;
    long amount = 200L;
    UserPoint expectedUserPoint = new UserPoint(id, 100, 0);
    Mockito.when(userPointTable.selectById(id)).thenReturn(expectedUserPoint);
    Mockito.when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenReturn(expectedUserPoint);

    // When
    UserPoint actualUserPoint = pointService.use(id, amount);

    // Then
    assertEquals(expectedUserPoint, actualUserPoint);
    verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), eq(TransactionType.USE), anyLong());
}

}
