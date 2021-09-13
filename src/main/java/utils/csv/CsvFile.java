package utils.csv;

public abstract class CsvFile implements AutoCloseable {
    protected static final int DEFAULT_BUFF_SIZE = 8 * 1024;
    protected static final String DEFAULT_DELIM = ",";
    protected static final String LINE_SEPARATOR = "\n";

    protected final String path;
    protected final String delim;

    public CsvFile(String path, String delim) {
        this.path = path;
        this.delim = delim;
    }

    public String getPath() {
        return path;
    }

    public String getDelim() {
        return delim;
    }
}
