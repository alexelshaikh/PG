package utils.fasta;

public abstract class FASTAFile implements AutoCloseable {
    protected final static String CAPTION_PREFIX = ">";
    protected final static int CAPTION_START_INDEX = CAPTION_PREFIX.length();
    protected final static String LINE_SEPARATOR = "\n";
    protected final static String LINE_SEP_PLUS_CAPTION_PRE = LINE_SEPARATOR + CAPTION_PREFIX;

    protected String path;

    public FASTAFile(String path) {
        this.path = path;
    }

    public static String getCaptionPrefix() {
        return CAPTION_PREFIX;
    }

    public static String getLineSeparator() {
        return LINE_SEPARATOR;
    }

    public String getPath() {
        return path;
    }

}
