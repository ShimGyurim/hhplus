## 동시성 문제 리포트

0. 동시성 프로그래밍 이란 : 여러 작업이 동시적으로 실행되거나/실행되는것 처럼 보이는 상태.
   기존의 순차적으로 실행되는 프로그래밍 구조에서 벗어난 형태.
   여러 이용자들이 존재하는 부하가 많은 시스템 등에서 효율적으로 일을 처리하기 위해 다중 쓰레드를 이용해 병렬적으로 실행된다.
   비슷한 개념으로 "비동기 프로그래밍" 이 있다. 이는 주 코드의 흐름을 멈추지 않고 별도의 코드 흐름을 생성하여 필요할 때에 해당 부분에서 연산된 결과를 주 쓰레드에서 사용 가능하다.
   
1. 적용한 블로킹 기법 : ConcurrentHashMap 과 ReentrantLock 을 같이 적용 

```java
public class PointService {
    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    public UserPoint charge(long id, long amount) {  
        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock(); //use와 동일한 lock 사용     
        try {
        } finally {
            lock.unlock();
        }
    }
    
    public UserPoint use(long id, long amount) {  
        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock(); //use와 동일한 lock 사용     
        try {
        } finally {
            lock.unlock();
        }
    }    
```

2. 선정이유
    
    synchronized 보다 구현은 복잡하나 id 별로 lock을 구현할 수 있고 , lock 을 걸 수 있는 범위가 더 자유로운 점이 있음.
    
    같은 id에 대해서는 동시에 charge (충전)과 use(사용) 접근도 불허하기 위해 ConcurrentHashMap 에 id 별 lock 을 넣어두고 lock 객체를 공유하였음.
    
    동기(순서 유지)까지는 따로 구현을 하지 않음. 
    
    이유는 첫 번째로 ReentrantLock 을 사용하는 것 자체로 어느정도 순서의 유지는 가능하다는 점.
    
    또한, 단순한 값을 변경하는 과정이기 때문에 순서가 조금 뒤틀려도 전체적인 로직에 영향이 거의 없을 것으로 판단하였음. 
    
    오히려 동기(순서 유지)로 구현을 하는 것이 성능상으로 안 좋을 수도 있다고 생각이 들음. 
    
    - ConcurrentHashMap 특징 : 읽기는 여러 쓰레드에서 가능하지만, 쓰기는 한 버킷 단위로 하나의 스레드만 가능 .
    - ReentrantLock 특징: 간단한 구현. 특정 조건에서 lock 을 풀고 다른 것을 처리후 재 진입 가능
    - 비슷한 개념
        - synchronized : 한 메소드 전체에 대해 동시에 단 하나의 스레드만 허용하여 동시성 보장. 구현이 간편
        - Future : 별도의 스레드로 비동기 작업. 작업 완료 후 필요시에 get() 메소드 이용하여 작업 결과를 가져옴. 메인 스레드의 블로킹을 유발하지 않음.
        - 낙관적 락 :  version 값을 이용하여 , version 값이 변경되면 해당 row는 갱신 불가 (이미 다른 트랜잭션에서 해당 row를 갱신한 상태를 의미)
        - 비관적 락: race condition 이 일어날 것을 가정한 락. 트랜잭션 시작 시 공유/배제 lock 을 걸고 시작. 배제 락 (수정) 시 해당 트랜잭션 이외 접근 불가.
        - StampedLock : ReentrantReadWriteLock + 낙관적 락
        
3. 테스트 결과 
    - 통합 테스트

```java
    @Test	
    public void concurrentChargeTest() throws InterruptedException {

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
```

- id = 1의 값이 동시성을 유지하며 100 씩 늘어남 .

```java
UserPoint[id=1, point=100, updateMillis=1727351272092] 스레드2의0번째
UserPoint[id=1, point=200, updateMillis=1727351272444] 스레드1의0번째
UserPoint[id=1, point=300, updateMillis=1727351272920] 스레드2의1번째
UserPoint[id=1, point=400, updateMillis=1727351273138] 스레드1의1번째
UserPoint[id=1, point=500, updateMillis=1727351273673] 스레드2의2번째
UserPoint[id=1, point=600, updateMillis=1727351274023] 스레드1의2번째
UserPoint[id=1, point=700, updateMillis=1727351274414] 스레드2의3번째
UserPoint[id=1, point=800, updateMillis=1727351274578] 스레드1의3번째
UserPoint[id=1, point=900, updateMillis=1727351274777] 스레드2의4번째
UserPoint[id=1, point=1000, updateMillis=1727351275444] 스레드1의4번째
UserPoint[id=1, point=1100, updateMillis=1727351275808] 스레드2의5번째
UserPoint[id=1, point=1200, updateMillis=1727351276048] 스레드1의5번째
UserPoint[id=1, point=1300, updateMillis=1727351276341] 스레드2의6번째
UserPoint[id=1, point=1400, updateMillis=1727351276810] 스레드1의6번째
UserPoint[id=1, point=1500, updateMillis=1727351277316] 스레드2의7번째
UserPoint[id=1, point=1600, updateMillis=1727351277533] 스레드1의7번째
UserPoint[id=1, point=1700, updateMillis=1727351278045] 스레드2의8번째
UserPoint[id=1, point=1800, updateMillis=1727351278403] 스레드1의8번째
UserPoint[id=1, point=1900, updateMillis=1727351278740] 스레드2의9번째
UserPoint[id=1, point=2000, updateMillis=1727351279124] 스레드1의9번째

```

4. 관련 소감 

동시성에 대해 application 단에서 처음으로 간단하게나마 구현할 수 있는 경험이 되었다 . 회사에서도 관리하는 프로그램이 싱글 스레드 위주로 프로그래밍 되어 있고, 웬만한 동시성 제어는 DB lock으로 처리하였기 때문이다. 

이번 기회에 여러 동기화 개념들을 배웠다 . 또한 ,동기/비동기  , 블로킹/논블로킹의 개념도 명확히 하는 계기가 되었다.
