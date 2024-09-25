package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private UserPointTable userPointTable;

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();


    //조회 point
    public UserPoint lookup(long id) {
        UserPoint userPoint = userPointTable.selectById(id);
        PointServiceValidator.validateUserPoint(userPoint);
        return userPoint;
    }
    ///충전,사용내역조회 history

    public List<PointHistory> history(long id) {
        List<PointHistory> object = pointHistoryTable.selectAllByUserId(id);
        if(object == null) throw new RuntimeException("객체가 없습니다.");
        if(object.size() <= 0) throw new RuntimeException("리턴 데이터 없음");
        return object;
    }
    //충전
    public UserPoint charge(long id, long amount) {
        PointServiceValidator.validateAmount(amount);

        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock(); //use와 동일한 lock 사용

        UserPoint userPoint = userPointTable.selectById(id);
        PointServiceValidator.validateUserPoint(userPoint);

        UserPoint userPointUpd = null;
        long tempval = userPoint.point()+amount;
        try {
            userPointUpd = userPointTable.insertOrUpdate(id,userPoint.point()+amount);
            pointHistoryTable.insert(id,userPoint.point()+amount,TransactionType.CHARGE,userPoint.updateMillis());
        } finally {
            lock.unlock();
        }
        return new UserPoint(userPoint.id(), tempval,userPointUpd.updateMillis());
    }

    //사용 use
    public UserPoint use(long id, long amount) {
        PointServiceValidator.validateAmount(amount);

        UserPoint userPoint = userPointTable.selectById(id);

        PointServiceValidator.validateUserPoint(userPoint);

        UserPoint userPointUpd = null;

        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock(); // charge 와 use 동일한 lock 사용

        long tempval = 0;
        try {
            tempval = userPoint.point()-amount < 0 ? userPoint.point() : userPoint.point()-amount;

            userPointUpd = userPointTable.insertOrUpdate(id, tempval);
            if(userPoint.point()-amount >= 0) {

                pointHistoryTable.insert(id,tempval,TransactionType.USE,userPoint.updateMillis());
            }
        } finally {
            lock.unlock();
        }
        return new UserPoint(userPoint.id(), tempval,userPointUpd.updateMillis());
    }


    static class PointServiceValidator {
        public static void validateAmount(long amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
        }

        public static void validateUserPoint(UserPoint userPoint) {
            if (userPoint == null) {
                throw new RuntimeException("UserPoint cannot be null");
            }
        }

        public static void validateUserPoint(List<PointHistory> pointHistories) {
            if (pointHistories == null) {
                throw new RuntimeException("UserPoint cannot be null");
            }
        }
    }
}
