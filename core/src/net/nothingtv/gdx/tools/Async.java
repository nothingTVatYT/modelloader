package net.nothingtv.gdx.tools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Async {

    public static Async getInstance() {
        if (instance == null)
            init();
        return instance;
    }

    private static Async instance;
    private final ExecutorService executor;

    private Async() {
        executor = Executors.newCachedThreadPool();
    }

    private void internalShutDown() {
        executor.shutdown();
    }

    public static void init() {
        if (instance == null) {
            instance = new Async();
        }
    }

    public static void shutDown() {
        if (instance != null)
            instance.internalShutDown();
    }

    public static void submit(Runnable runnable) {
        getInstance().executor.submit(runnable);
    }
}
