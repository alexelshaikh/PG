package utils.fasta;

import core.BaseSequence;
import utils.FuncUtils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Flushable;

public class WriteableFASTAFile extends FASTAFile implements Flushable {
    private static final int DEFAULT_BUFF_SIZE = 8 * 1024;

    private int count;
    private final BufferedWriter bw;

    /**
     * Creates a buffered handler for appending to a fasta file.
     * @param path the path to the fasta file.
     * @param buffSize the size of the buffer that is used to append to the file.
     * @param append a flag denoting whether to append or to write to the file.
     */
    public WriteableFASTAFile(String path, int buffSize, boolean append) {
        super(path);
        this.bw = FuncUtils.safeCall(() -> new BufferedWriter(new FileWriter(path, append), buffSize));
        this.count = FuncUtils.safeCall(() -> {
            int linesCount = FuncUtils.countLinesInFile(path, buffSize);
            if (linesCount % 2 != 0)
                throw new RuntimeException("fasta file has an odd number of lines: " + linesCount);
            return linesCount / 2;
        });
    }

    /**
     * Creates a buffered handler for appending to a fasta file.
     * @param path the path to the fasta file.
     * @param buffSize the size of the buffer that is used to write to the file.
     */
    public WriteableFASTAFile(String path, int buffSize) {
        this(path, buffSize, true);
    }

    /**
     * Creates a buffered handler for writing to a fasta file.
     * @param path the path to the fasta file.
     */
    public WriteableFASTAFile(String path) {
        this(path, DEFAULT_BUFF_SIZE);
    }

    /**
     * Creates a buffered handler for writing/appending to a fasta file.
     * @param path the path to the fasta file.
     * @param append a flag denoting whether to append or to write to the file. If this flag is set true, the content of the fasta file will be read first.
     */
    public WriteableFASTAFile(String path, boolean append) {
        this(path, DEFAULT_BUFF_SIZE, append);
    }

    /**
     * Appends the given sequence to the fasta file.
     * @param seq the DNA sequence to append.
     * @param caption the sequence's caption as a string.
     */
    public void append(BaseSequence seq, String caption) {
        if (count == 0)
            FuncUtils.safeRun(() -> bw.append(CAPTION_PREFIX).append(caption).append(LINE_SEPARATOR).append(seq.toString()));
        else
            FuncUtils.safeRun(() -> bw.append(LINE_SEP_PLUS_CAPTION_PRE).append(caption).append(LINE_SEPARATOR).append(seq.toString()));

        count++;
    }

    /**
     * Appends the given sequence to the fasta file.
     * @param seq the DNA sequence to append.
     * @param caption the sequence's caption as an integer value.
     */
    public void append(BaseSequence seq, int caption) {
        append(seq, String.valueOf(caption));
    }

    /**
     * Appends the given sequence to the fasta file.
     * @param seqs the DNA sequences to append.
     * @param startCaptionId the first caption to start from.
     */
    public void append(Iterable<BaseSequence> seqs, int startCaptionId) {
        for (BaseSequence seq : seqs)
            append(seq, String.valueOf(startCaptionId++));
    }

    /**
     * Appends the given sequence to the fasta file.
     * @param seq the DNA sequence to append.
     */
    public void append(BaseSequence seq) {
        append(seq, count + 1);
    }

    /**
     * Appends the given sequence to the fasta file.
     * @param seqs the DNA sequences to append.
     */
    public void append(Iterable<BaseSequence> seqs) {
        append(seqs, count + 1);
    }

    /**
     * @return The number of DNA sequences present in the fasta file (including buffered items).
     */
    public int getCount() {
        return count;
    }

    @Override
    public void close() {
        FuncUtils.safeRun(this.bw::close);
    }

    @Override
    public void flush() {
        FuncUtils.safeRun(this.bw::flush);
    }
}
