package org.redhatsummit.damagesource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterState;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
    StrongCounter assumptions:
    - CounterListener: onUpdate is only invoked if the update method is valid (i.e. cannont decrement a
        counter whose state is LOWER_BOUND_REACHED
    - If lower_bound=0 and current_value=0, a decrement event will change the state to LOWER_BOUND_REACHED
        and onUpdate event will be invoked

    Problem:
    - Once a CounterOutOfBoundsException is thrown: OptaPlanner thread stops handling counter updates.
 */
public class AsyncDamageEventsTest {

    private static final String HEALTH_COUNTER_NAME = "health.counter.name";
    private static final int NUM_THREADS = 1;
    private static final int NUM_DECREMENTS_PER_THREAD = 10;
    private static final String HEALTH_COUNTER_INITIAL_VALUE = "health.counter.initial.value";
    private static final String HEALTH_COUNTER_LOWER_BOUND = "health.counter.lower.bound";

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private CountDownLatch startLatch = new CountDownLatch(1);
    private AtomicInteger numAttemptedDecrements = new AtomicInteger(0);
    private AtomicInteger numSuccessfulDecrements = new AtomicInteger(0);
    private AtomicInteger numNonSuccessfulDecrements = new AtomicInteger(0);
    private Properties props = new Properties();

    private ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
    private ScheduledExecutorService resetExecutorService = Executors.newSingleThreadScheduledExecutor();

    @Before
    public void init() {
        try {
            props.load(AsyncDamageEventsTest.class.getClassLoader().getResourceAsStream("infinispan.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void decrementCounterAsync() {
        Configuration configuration = ClientConfiguration.get().build();
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
        CounterManager counterManager = RemoteCounterManagerFactory.asCounterManager(remoteCacheManager);
        String counterName = infinispanProperty(HEALTH_COUNTER_NAME);
        if (counterManager.isDefined(counterName)) {
            LOGGER.info("Health counter exist: removing it.");
            counterManager.remove(counterName);
        }
        CounterConfiguration cc = CounterConfiguration.builder(CounterType.BOUNDED_STRONG)
                .initialValue(Long.parseLong(infinispanProperty(HEALTH_COUNTER_INITIAL_VALUE)))
                .lowerBound(Long.parseLong(infinispanProperty(HEALTH_COUNTER_LOWER_BOUND)))
                .build();
        counterManager.defineCounter(counterName, cc);
        StrongCounter healthCounter = counterManager.getStrongCounter(counterName);

        // Start Listener thread to listen to counter decrements
        ListenerThread listenerThread = new ListenerThread(healthCounter);
        listenerThread.setDaemon(true);
        listenerThread.setPriority(Thread.MAX_PRIORITY);

        listenerThread.start();
        try {
            startLatch.await(); // Wait until Listener thread register a listener
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(0, listenerThread.getNumDecrementEvents());
        assertEquals(0, listenerThread.getNumResets());

        // Reset counter every second:
//        resetExecutorService.scheduleAtFixedRate(healthCounter::reset, 1, 1, TimeUnit.SECONDS);

        // Start threads to submit damage events
        List<ThreadDecrementTask> threadDecrementTaskList = new ArrayList<>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            threadDecrementTaskList.add(new ThreadDecrementTask(healthCounter));
        }
        try {
            executorService.invokeAll(threadDecrementTaskList)
                    .parallelStream()
                    .flatMap(listFuture -> {
                        try {
                            return listFuture.get().parallelStream();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(integerFuture -> {
                        try {
                            if (integerFuture.get() == 1) {
                                numSuccessfulDecrements.incrementAndGet();
                            } else {
                                numNonSuccessfulDecrements.incrementAndGet();
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOGGER.info("Number of attempted decrements: " + numAttemptedDecrements.get());
        LOGGER.info("Number of successful decrements: " + numSuccessfulDecrements.get());
        LOGGER.info("Number of non-successful decrements: " + numNonSuccessfulDecrements.get());
        LOGGER.info(String.format("Number of listener successfulDecrements: %d, resets: %d.",
                listenerThread.getNumDecrementEvents(), listenerThread.getNumResets()));
        healthCounter.getValue().thenAccept(value -> LOGGER.info("Counter final value is: " + value));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        assertEquals(numAttemptedDecrements.get(), numSuccessfulDecrements.get() + numNonSuccessfulDecrements.get());
//        assertEquals(numSuccessfulDecrements.get(), listenerThread.getNumDecrementEvents());
//        int expectedNumResets = Math.floorDiv(
//                numSuccessfulDecrements.get(), Integer.parseInt(infinispanProperty(HEALTH_COUNTER_INITIAL_VALUE)));
//        assertEquals(expectedNumResets, listenerThread.getNumResets());
//        assertEquals(NUM_THREADS * NUM_DECREMENTS_PER_THREAD, numAttemptedDecrements.get());
    }

    private String infinispanProperty(String name) {
        return props.getProperty(name);
    }

    private class ThreadDecrementTask implements Callable<List<Future<Integer>>> {

        private StrongCounter strongCounter;

        public ThreadDecrementTask(StrongCounter strongCounter) {
            this.strongCounter = strongCounter;
        }

        @Override
        public List<Future<Integer>> call() throws InterruptedException {
            List<Future<Integer>> decrementFutureList = new ArrayList<>(NUM_DECREMENTS_PER_THREAD);
            for (int i = 0; i < NUM_DECREMENTS_PER_THREAD; i++) {
                decrementFutureList.add(strongCounter.decrementAndGet()
                        .handle((aLong, throwable) -> {
                            numAttemptedDecrements.incrementAndGet();
                            LOGGER.info("Handling... by " + Thread.currentThread());
                            if (throwable == null) {
                                return 1;
                            }
                            LOGGER.warning(throwable.toString());
                            return 0;
                        }));
                if (i % Integer.parseInt(infinispanProperty(HEALTH_COUNTER_INITIAL_VALUE)) == 0) { // Give listener thread a chance to reset counter
//                    Thread.sleep(100); // TODO: uncomment this when testing resetting
                }
            }
            return decrementFutureList;
        }
    }

    private class ListenerThread extends Thread {

        private final long healthCounterLowerBound = Long.parseLong(infinispanProperty(HEALTH_COUNTER_LOWER_BOUND));

        private StrongCounter strongCounter;
        private AtomicInteger numResets = new AtomicInteger(0);
        private AtomicInteger numDecrementEvents = new AtomicInteger(0);

        public ListenerThread(StrongCounter strongCounter) {
            this.strongCounter = strongCounter;
        }

        public int getNumResets() {
            return numResets.get();
        }

        public int getNumDecrementEvents() {
            return numDecrementEvents.get();
        }

        @Override
        public void run() {
            strongCounter.addListener(counterEvent -> {
                LOGGER.info(counterEvent.toString());
                if (counterEvent.getNewValue() < counterEvent.getOldValue()) { // only count decrement events
                    numDecrementEvents.incrementAndGet();
                }
                if (counterEvent.getNewValue() > counterEvent.getOldValue()) { // another thread reset the counter
                    numResets.incrementAndGet();
                }
//                if (counterEvent.getNewValue() == healthCounterLowerBound
//                        && counterEvent.getNewState().equals(CounterState.VALID)) { // first time to reach lower_bound, reset counter
//                    strongCounter.reset();
//                    numResets.incrementAndGet();
//                }
            });
            startLatch.countDown();
        }
    }
}
