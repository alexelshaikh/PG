package utils.fasta;

import core.BaseSequence;
import utils.FuncUtils;
import utils.Pair;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadableFASTAFile extends FASTAFile implements Iterable<ReadableFASTAFile.Entry> {
    private static final int DEFAULT_BUFF_SIZE = 8 * 1024;

    private final BufferedReader br;

    public ReadableFASTAFile(String path, int buffSize) {
        super(path);
        this.br = FuncUtils.safeCall(() -> new BufferedReader(new FileReader(path), buffSize));
    }

    public ReadableFASTAFile(String path) {
        this(path, DEFAULT_BUFF_SIZE);
    }

    public boolean available() {
        return FuncUtils.safeCall(this.br::ready);
    }

    public Entry read() {
        String captionLine = FuncUtils.safeCall(this.br::readLine);
        if (captionLine == null) {
            close();
            return null;
        }
        String seqLine = FuncUtils.safeCall(this.br::readLine);
        if (seqLine == null)
            throw new RuntimeException("no BaseSequence found for: " + captionLine.substring(CAPTION_START_INDEX));

        return Entry.of(captionLine, seqLine);
    }

    public List<Entry> readRemaining() {
        List<Entry> list = new ArrayList<>();
        Entry e = read();
        while(e != null) {
            list.add(e);
            e = read();
        }

        close();
        return list;
    }

    public BaseSequence readSeq() {
        return read().getSeq();
    }

    public List<BaseSequence> readRemainingSeqs() {
        return readRemaining().stream().map(Entry::getSeq).collect(Collectors.toList());
    }

    public Stream<Entry> stream() {
        return FuncUtils.stream(this);
    }

    public Iterable<BaseSequence> asSeqIterable() {
        return () -> new Iterator<>() {
            final Iterator<Entry> it = iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            @Override
            public BaseSequence next() {
                return it.next().getSeq();
            }
        };
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return available();
            }
            @Override
            public Entry next() {
                return read();
            }
        };
    }

    @Override
    public void close() {
        FuncUtils.safeRun(this.br::close);
    }
    public static class Entry extends Pair<String, BaseSequence> {
        private Entry(String caption, BaseSequence seq) {
            super(caption, seq);
        }
        public String getCaption() {
            return super.t1;
        }
        public BaseSequence getSeq() {
            return super.t2;
        }
        private static Entry of(String captionLine, String seqLine) {
            if (!captionLine.startsWith(CAPTION_PREFIX))
                throw new RuntimeException("wrong FASTA caption for caption line: " + captionLine);
            return new Entry(
                    captionLine.substring(CAPTION_START_INDEX),
                    new BaseSequence(seqLine));
        }
    }
}
