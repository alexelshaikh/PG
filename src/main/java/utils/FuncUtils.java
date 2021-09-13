package utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class FuncUtils {
    @FunctionalInterface
    public interface RunnableAttempt {
        void run() throws Exception;
    }

    public static void safeRun(RunnableAttempt runnableAttempt) {
        try {
            runnableAttempt.run();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T safeCall(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T superSafeCall(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    public static <T> Stream<T> stream(Iterable<T> it) {
        return StreamSupport.stream(it.spliterator(), false);
    }

    public static <T> T random(T... array) {
        return array[(int) (Math.random() * array.length)];
    }


    public static int countLinesInFile(String fileName, int buffSize) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
            byte[] c = new byte[buffSize];

            int readChars = is.read(c);
            if (readChars == -1)
                return 0;

            int count = 0;
            while (readChars == buffSize) {
                for (int i = 0; i < buffSize; i++) {
                    if (c[i] == '\n') {
                        count++;
                    }
                }
                readChars = is.read(c);
            }

            while (readChars != -1) {
                for (int i = 0; i < readChars; i++) {
                    if (c[i] == '\n') {
                        count++;
                    }
                }
                readChars = is.read(c);
            }

            return count + 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
