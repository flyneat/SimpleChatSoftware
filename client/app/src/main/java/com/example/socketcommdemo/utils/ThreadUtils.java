package com.example.socketcommdemo.utils;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils{
    private ThreadPoolExecutor mExecutor;
    private static volatile ThreadUtils INSTANCE;

    private ThreadUtils() {
        mExecutor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "pool_thread:" + new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date()));
            }
        });
    }

    public static ThreadUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (ThreadUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ThreadUtils();
                }
            }
        }
        return INSTANCE;
    }

    public void run(@NonNull Runnable task) {
        mExecutor.execute(task);
    }


    public void shutDown() {
//        mExecutor.shutdownNow();
        mExecutor.shutdown();
        // 收回ThreadUtils对象，为了关闭线程池，避免重新启动app时，复用上次创建的线程池。
        // 但上次创建的线程池调用了shutdown()方法，线程池的状态会处于Terminated状态，导致重新启动app,
        // 一旦开始执行任务，就会触发java.util.concurrent.RejectedExecutionException。
        // 因为处于terminated状态的线程池是不允许添加或执行新的任务的。
        INSTANCE = null;
    }

}
