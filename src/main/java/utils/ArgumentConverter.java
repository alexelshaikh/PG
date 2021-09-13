package utils;

import java.util.function.Function;

public class ArgumentConverter<T>  {
    private final Function<String, T> func;
    private final T orElse;

    public ArgumentConverter(Function<String, T> func, T orElse) {
        this.func = func;
        this.orElse = orElse;
    }

    public ArgumentConverter(Function<String, T> func) {
        this(func, null);
    }

    public T convert(String s) {
        try {
            return func.apply(s);
        }
        catch(Throwable t) {
            return orElse;
        }
    }

    public static ArgumentConverter<Integer> INT(int orElse) {
        return new ArgumentConverter<>(Integer::parseInt, orElse);
    }
    public static ArgumentConverter<Long> LONG(long orElse) {
        return new ArgumentConverter<>(Long::parseLong, orElse);
    }
    public static ArgumentConverter<Float> FLOAT(float orElse) {
        return new ArgumentConverter<>(Float::parseFloat, orElse);
    }
    public static ArgumentConverter<Double> DOUBLE(double orElse) {
        return new ArgumentConverter<>(Double::parseDouble, orElse);
    }
    public static ArgumentConverter<Boolean> BOOLEAN(boolean orElse) {
        return new ArgumentConverter<>(s -> s.equals("y") || s.equals("yes") || s.equals("true") || s.equals("1"), orElse);
    }
    public static ArgumentConverter<String> STRING(String orElse) {
        return new ArgumentConverter<>(s -> s != null? s : orElse, orElse);
    }

    public static ArgumentConverter<String> STRING_LOWERCASE(String orElse) {
        return new ArgumentConverter<>(s -> s != null? s.toLowerCase() : orElse, orElse);
    }
    public static ArgumentConverter<String> IDENTITY() {
        return new ArgumentConverter<>(Function.identity(), null);
    }

}
