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

    public WriteableFASTAFile(String path, int buffSize) {
        this(path, buffSize, true);
    }

    public WriteableFASTAFile(String path) {
        this(path, DEFAULT_BUFF_SIZE);
    }

    public WriteableFASTAFile(String path, boolean append) {
        this(path, DEFAULT_BUFF_SIZE, append);
    }

    public void append(BaseSequence seq, String caption) {
        if (count == 0)
            FuncUtils.safeRun(() -> bw.append(CAPTION_PREFIX).append(caption).append(LINE_SEPARATOR).append(seq.toString()));
        else
            FuncUtils.safeRun(() -> bw.append(LINE_SEP_PLUS_CAPTION_PRE).append(caption).append(LINE_SEPARATOR).append(seq.toString()));

        count++;
    }

    public void append(BaseSequence seq, int caption) {
        append(seq, String.valueOf(caption));
    }

    public void append(Iterable<BaseSequence> seqs, int startCaptionId) {
        for (BaseSequence seq : seqs)
            append(seq, String.valueOf(startCaptionId++));
    }

    public void append(BaseSequence seq) {
        append(seq, count + 1);
    }

    public void append(Iterable<BaseSequence> seqs) {
        append(seqs, count + 1);
    }

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
