package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private UserPointTable userPointTable;

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
        UserPoint userPoint = userPointTable.selectById(id);
        //    public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis) {
        UserPoint userPoint1 = userPointTable.insertOrUpdate(id,userPoint.point()+amount);
        pointHistoryTable.insert(id,amount,TransactionType.CHARGE,userPoint1.updateMillis());
        return userPoint1;
    }
    //사용 use
    public UserPoint use(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);
        long tempval = userPoint.point()-amount;
        if(userPoint.point()-amount < 0)  tempval = userPoint.point();
        UserPoint userPoint1 = userPointTable.insertOrUpdate(id,userPoint.point()+amount);
        pointHistoryTable.insert(id,amount,TransactionType.USE,userPoint1.updateMillis());
        return userPoint1;
    }
}
