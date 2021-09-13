package utils.csv;

import utils.FuncUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BufferedCsvReader extends CsvFile implements Iterable<BufferedCsvReader.Line> {
    private final BufferedReader bf;
    private Map<String, Integer> colMapping;

    public BufferedCsvReader(String path, String delim, int buffSize) {
        super(path, delim);
        this.bf = FuncUtils.safeCall(() -> new BufferedReader(new FileReader(path), buffSize));
        this.colMapping = new HashMap<>();

        putColMapping();
    }

    public BufferedCsvReader(String path, String delim) {
        this(path, delim, DEFAULT_BUFF_SIZE);
    }

    public BufferedCsvReader(String path) {
        this(path, DEFAULT_DELIM);
    }

    public Line readLine() {
        return new Line(FuncUtils.safeCall(bf::readLine));
    }

    public boolean available() {
        return FuncUtils.safeCall(bf::ready);
    }

    public Set<String> getHeaderSet() {
        return colMapping.keySet();
    }

    private void putColMapping() {
        String[] colNames = FuncUtils.safeCall(() -> this.bf.readLine().split(delim));
        for (int i = 0; i < colNames.length; i++) {
            if (colMapping.containsKey(colNames[i]))
                throw new RuntimeException("duplicate column name: " + colNames[i]);

            colMapping.put(colNames[i], i);
        }
        colMapping = Collections.unmodifiableMap(colMapping);
    }

    public String getHeaderLine() {
        return colMapping.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).collect(Collectors.joining(delim));
    }

    public Stream<Line> stream() {
        return FuncUtils.stream(this);
    }

    public Map<String, Integer> getColMapping() {
        return colMapping;
    }

    @Override
    public Iterator<Line> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return available();
            }
            @Override
            public Line next() {
                return readLine();
            }
        };
    }

    @Override
    public void close() {
        FuncUtils.safeRun(this.bf::close);
    }

    public class Line {
        private final String line;
        private final String[] splitLine;

        private Line(String line) {
            this.line = line;
            this.splitLine = line.split(delim);
        }

        public String get(String colName) {
            Integer index = colMapping.get(colName);
            if (index == null)
                throw new RuntimeException("column not found: " + colName);

            return get(index);
        }

        public String get(int index) {
            if (index < 0 || index > splitLine.length)
                throw new RuntimeException("index out of bounds: " + index);

            return splitLine[index];
        }

        public String getLine() {
            return line;
        }

        public String[] getSplitLine() {
            return splitLine;
        }

        @Override
        public String toString() {
            return line;
        }
    }
}
