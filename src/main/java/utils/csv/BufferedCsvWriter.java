package utils.csv;

import utils.FuncUtils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Flushable;

public class BufferedCsvWriter extends CsvFile implements Flushable {
    private final BufferedWriter bw;
    private int count;

    public BufferedCsvWriter(String path, String delim, int buffSize, boolean append) {
        super(path, delim);
        this.bw = FuncUtils.safeCall(() -> new BufferedWriter(new FileWriter(path, append), buffSize));
        this.count = FuncUtils.countLinesInFile(path, buffSize);
    }

    public BufferedCsvWriter(String path, String delim) {
        this(path, delim, DEFAULT_BUFF_SIZE, true);
    }

    public BufferedCsvWriter(String path) {
        this(path, DEFAULT_DELIM, DEFAULT_BUFF_SIZE, true);
    }

    public BufferedCsvWriter(String path, boolean append) {
        this(path, DEFAULT_DELIM, DEFAULT_BUFF_SIZE, append);
    }

    public BufferedCsvWriter(String path, String delim, boolean append) {
        this(path, delim, DEFAULT_BUFF_SIZE, append);
    }

    public void appendNewLine(String... values) {
        if (count == 0)
            FuncUtils.safeRun(() -> this.bw.write(String.join(delim, values)));
        else
            FuncUtils.safeRun(() -> this.bw.write(LINE_SEPARATOR + String.join(delim, values)));

        count++;
    }

    public void newLine() {
        FuncUtils.safeRun(() -> this.bw.write(LINE_SEPARATOR));
        count++;
    }

    @Override
    public void flush() {
        FuncUtils.safeRun(this.bw::flush);
    }

    @Override
    public void close() {
        FuncUtils.safeRun(this.bw::close);
    }
}
