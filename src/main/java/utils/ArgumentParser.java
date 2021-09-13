package utils;

import java.util.HashMap;
import java.util.Map;

public class ArgumentParser {
    public static final String DEFAULT_ARGS_ASSIGN = "=";

    private final Map<String, String> params;
    boolean nameCaseSensitive;
    boolean valueCaseSensitive;


    public ArgumentParser(String[] args) {
        this(args, false, false);
    }

    public ArgumentParser(String[] args, boolean nameCaseSensitive, boolean valueCaseSensitive) {
        this.nameCaseSensitive = nameCaseSensitive;
        this.valueCaseSensitive = valueCaseSensitive;
        this.params = parse(args, nameCaseSensitive, valueCaseSensitive);
    }

    public static Map<String, String> parse(String[] args, boolean nameCaseSensitive, boolean valueCaseSensitive) {
        if (args == null || args.length <= 0)
            return new HashMap<>();

        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            String[] split = splitArgument(arg);
            String name = nameCaseSensitive? split[0] : split[0].toLowerCase();
            String value = valueCaseSensitive? split[1] : split[1].toLowerCase();
            if (map.containsKey(name))
                throw new RuntimeException("duplicate parameter");
            map.put(name, value);
        }

        return map;
    }

    public static Map<String, String> parse(String[] args) {
        return parse(args, false, false);
    }

    public static String[] splitArgument(String arg) {
        try {
            String[] split = arg.split(DEFAULT_ARGS_ASSIGN);
            if (split.length == 2)
                return split;
            else
                throw new RuntimeException("too many '=' characters in argument");
        }
        catch(Exception e) {
            throw new RuntimeException("Failed parsing argument: " + arg + "\nReason: " + e.getMessage());
        }
    }

    public <T> T getParam(String name, ArgumentConverter<T> converter) {
        return converter.convert(params.get(nameCaseSensitive? name : name.toLowerCase()));
    }
}
