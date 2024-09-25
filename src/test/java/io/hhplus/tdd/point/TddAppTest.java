package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TddAppTest {

    @Autowired
    PointService pointService;
    @Test
    void pointTest() {
        System.out.println(pointService.charge(1,200));
    }
}
