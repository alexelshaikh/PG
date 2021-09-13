package generator.probes;

import core.BaseSequence;
import generator.SeqGenerator;
import utils.LSH;

public class ProbesGeneratorLSH implements SeqGenerator {

    private final float minDist;
    private final SeqGenerator generator;
    private final LSH lsh;
    private final int k;

    public ProbesGeneratorLSH(SeqGenerator generator, LSH lsh, float minDist) {
        this.generator = generator;
        this.minDist = minDist;
        this.lsh = lsh;
        this.k = lsh.getK();
    }

    private synchronized boolean tryAdd(BaseSequence seq) {
        if (lsh.similarSeqs(seq).stream().map(can -> can.jaccardDistance(seq, k)).min(Float::compare).orElse(1.0f) >= minDist) {
            lsh.insert(seq);
            return true;
        }

        return false;
    }

    @Override
    public BaseSequence generate() {
        return generator.stream().filter(this::tryAdd).findFirst().orElseThrow();
    }
}
