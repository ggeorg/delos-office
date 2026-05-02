package io.github.ggeorg.delos.writer.contract;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class JavaFxTestSupport {
    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        if (TOOLKIT_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException alreadyStarted) {
                latch.countDown();
            }
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while starting JavaFX toolkit");
            }
        }
    }

    protected static <T> T onFxThread(Callable<T> task) {
        if (Platform.isFxApplicationThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw propagate(e);
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(task.call());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        await(latch);
        if (failure.get() != null) {
            throw propagate(failure.get());
        }
        return result.get();
    }

    protected static void onFxThread(Runnable task) {
        onFxThread(() -> {
            task.run();
            return null;
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for JavaFX task");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for JavaFX task", e);
        }
    }

    private static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(throwable);
    }
}
