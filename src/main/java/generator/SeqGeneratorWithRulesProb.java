package generator;

import core.BaseSequence;
import rules.DNARulesCollection;
import java.util.stream.Stream;

public class SeqGeneratorWithRulesProb implements SeqGenerator {
    public static final int DEFAULT_LEN = 60;
    public static final float DEFAULT_GC = 0.5f;

    private final DNARulesCollection rules;
    private final int len;
    private final float gc;
    private final float maxError;

    public SeqGeneratorWithRulesProb(float maxError, DNARulesCollection rules) {
        this(DEFAULT_LEN, DEFAULT_GC, maxError, rules);
    }

    public SeqGeneratorWithRulesProb(int len, float gc, float maxError, DNARulesCollection rules) {
        this.len = len;
        this.gc = gc;
        this.maxError = maxError;
        this.rules = rules;
    }

    @Override
    public BaseSequence generate() {
        return Stream.generate(() -> BaseSequence.random(len, gc)).filter(seq -> rules.evalErrorByLimit(seq, maxError) <= maxError).findFirst().orElseThrow();
    }
}
