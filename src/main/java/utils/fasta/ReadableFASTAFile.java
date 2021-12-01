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

    /**
     * Creates a buffered handler for reading from a fasta file.
     * @param path the path to the fasta file.
     * @param buffSize the size of the buffer that is used to read in the file.
     */
    public ReadableFASTAFile(String path, int buffSize) {
        super(path);
        this.br = FuncUtils.safeCall(() -> new BufferedReader(new FileReader(path), buffSize));
    }

    /**
     * Creates a buffered handler for reading from a fasta file.
     * @param path the path to the fasta file.
     */
    public ReadableFASTAFile(String path) {
        this(path, DEFAULT_BUFF_SIZE);
    }

    /**
     * @return true if there is more to read.
     */
    public boolean available() {
        return FuncUtils.safeCall(this.br::ready);
    }

    /**
     * @return an Entry object that contains the read DNA sequence along with its caption.
     */
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

    /**
     * @return the remaining sequences in the file as a list of Entry.
     */
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

    /**
     * @return the next read sequence in the file.
     */
    public BaseSequence readSeq() {
        return read().getSeq();
    }

    /**
     * @return the remaining sequences in the file as a list of BaseSequence.
     */
    public List<BaseSequence> readRemainingSeqs() {
        return readRemaining().stream().map(Entry::getSeq).collect(Collectors.toList());
    }

    /**
     * @return a stream representing the fasta sequences as Entry objects.
     */
    public Stream<Entry> stream() {
        return FuncUtils.stream(this);
    }

    /**
     * @return an iterable of BaseSequence of the fasta sequences.
     */
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
        /**
         * @return the caption of the given Entry object.
         */
        public String getCaption() {
            return super.t1;
        }
        /**
         * @return the DNA sequence of the given Entry object.
         */
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
