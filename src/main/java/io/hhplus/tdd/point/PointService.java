package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private UserPointTable userPointTable;

    private ReentrantLock lock = new ReentrantLock();

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /*
    amount 가 마이너스
    amount 가 0
    selectById 가
     */
    //조회 point
    public UserPoint lookup(long id) {
        return userPointTable.selectById(id);
    }
    ///충전,사용내역조회 history

    public List<PointHistory> history(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
    //충전
    public UserPoint charge(long id, long amount) {
        if (amount < 0 ) throw new RuntimeException("충전량이 이상합니다");

        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock(); //use와 동일한 lock 사용

        UserPoint userPoint = userPointTable.selectById(id);
        try {

            userPoint = userPointTable.insertOrUpdate(id,userPoint.point()+amount);
            pointHistoryTable.insert(id,amount,TransactionType.CHARGE,userPoint.updateMillis());
        } finally {
            lock.unlock();
        }

        return userPoint;
    }
    //사용 use
    public UserPoint use(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);

        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock(); // charge 와 use 동일한 lock 사용

        try {
            long tempval = userPoint.point()-amount;

            tempval = userPoint.point()-amount < 0 ? userPoint.point() : userPoint.point()-amount; // 값 유지
            userPoint = userPointTable.insertOrUpdate(id,tempval);

            if(userPoint.point()-amount >= 0)
                pointHistoryTable.insert(id,amount,TransactionType.USE,userPoint.updateMillis());
        } finally {
            lock.unlock();
        }

        return userPoint;
    }
    static class PointServiceValidator {
        public void nullValidator () {

        }
    }
}
