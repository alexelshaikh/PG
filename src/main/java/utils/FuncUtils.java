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

    /**
     * Runs the given runnableAttempt and converts all exceptions to RuntimeExceptions.
     * @param runnableAttempt the RunnableAttempt object.
     */
    public static void safeRun(RunnableAttempt runnableAttempt) {
        try {
            runnableAttempt.run();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param callable the Callable object.
     * @return the result of the callable object. Converts all exceptions to RuntimeExceptions.
     */
    public static <T> T safeCall(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param callable the Callable object.
     * @return the result of the callable object, ignoring all exceptions. Returns null in the case of exceptions.
     */
    public static <T> T superSafeCall(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Converts an iterator object to a respective stream.
     * @param it the input iterator.
     * @return a stream containing the elements of the given iterator.
     */
    public static <T> Stream<T> stream(Iterable<T> it) {
        return StreamSupport.stream(it.spliterator(), false);
    }

    /**
     * @param array an array of elements.
     * @return a random element from the given array.
     */
    public static <T> T random(T... array) {
        return array[(int) (Math.random() * array.length)];
    }


    /**
     * @param fileName the file path.
     * @return the number of lines in the given file.
     */
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
