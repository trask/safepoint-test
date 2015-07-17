package org.example;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;

public class SafepointTest {

    @Test
    public void test() throws Exception {
        Class<?> sunManagementFactoryHelperClass =
                Class.forName("sun.management.ManagementFactoryHelper");
        Method registerInternalMBeansMethod = sunManagementFactoryHelperClass
                .getDeclaredMethod("registerInternalMBeans", MBeanServer.class);
        registerInternalMBeansMethod.setAccessible(true);
        registerInternalMBeansMethod.invoke(null, ManagementFactory.getPlatformMBeanServer());

        ExecutorService executorService = Executors.newCachedThreadPool();

        final AtomicInteger threadCount = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                public void run() {
                    threadCount.incrementAndGet();
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                    }
                }
            });
        }

        while (threadCount.get() < 100) {
            Thread.sleep(10);
        }

        long initialCount = getSafepointCount();

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadBean.getAllThreadIds();
        threadBean.getThreadInfo(threadIds, 1000);

        System.out.println(getSafepointCount() - initialCount);

        executorService.shutdownNow();
    }

    private static long getSafepointCount() throws Exception {
        ObjectName objectName = ObjectName.getInstance("sun.management:type=HotspotRuntime");
        return (Long) ManagementFactory.getPlatformMBeanServer().getAttribute(objectName,
                "SafepointCount");
    }
}
