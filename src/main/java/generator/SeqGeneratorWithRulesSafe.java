package generator;

import core.Base;
import core.BaseSequence;
import rules.DNARulesCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class SeqGeneratorWithRulesSafe implements SeqGenerator {

    private static final int MAX_SWAPS = 4;
    private static final String ERROR_PROPERTY_KEY = "error";

    private final int gcBases;
    private final int gcMaxDeviationBases;
    private final int len;
    private final float maxError;
    private final int swapsCount;
    private final DNARulesCollection rules;

    public SeqGeneratorWithRulesSafe(int len, float gcTarget, float gcMaxDeviation, float maxError, DNARulesCollection rules) {
        this.gcMaxDeviationBases = (int) (gcMaxDeviation * len);
        this.gcBases = (int) (gcTarget * len);
        this.len = len;
        this.maxError = maxError;
        this.swapsCount = Math.max(len >> 2, 1);
        this.rules = rules;
    }

    @Override
    public BaseSequence generate() {
        return Stream.generate(this::gen).filter(seq -> (float) seq.getProperty(ERROR_PROPERTY_KEY) <= maxError).findFirst().orElseThrow();
    }

    private BaseSequence gen() {
        Random rand = ThreadLocalRandom.current();
        List<Base> bases = new ArrayList<>(len);
        float error = -1.0f;
        int c = 0;
        int maxSwaps = 0;
        while (c++ < len)
            bases.add(null);
        BaseSequence seq = new BaseSequence(bases);
        int gcDeviationBases = gcMaxDeviationBases > 0? rand.nextInt(gcMaxDeviationBases) * (rand.nextBoolean()? 1 : -1) : 0;
        int gcs = gcBases + gcDeviationBases;
        int currentSize = gcs;
        while(gcs-- > 0)
            setBase(bases, rand.nextInt(len), rand.nextBoolean()? Base.C : Base.G);

        while(currentSize++ < len)
            setBase(bases, rand.nextInt(len), rand.nextBoolean()? Base.A : Base.T);

        while (maxSwaps++ < MAX_SWAPS && (error=rules.evalErrorByLimit(seq, maxError)) > maxError)
            shuffleBySwapping(bases, swapsCount);

        return seq.putProperty(ERROR_PROPERTY_KEY, error);
    }

    protected static void shuffleBySwapping(List<Base> bases, int swaps) {
        int len = bases.size();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int i;
        int j;
        while (swaps-- > 0) {
            i = rand.nextInt(len);
            j = rand.nextInt(len);
            Base bi = bases.get(i);
            bases.set(i, bases.get(j));
            bases.set(j, bi);
        }
    }
    
    private void setBase(List<Base> bases, int i, Base b) {
        while (bases.get(i) != null)
            i = (i + 1 < len) ? i + 1 : 0;

        bases.set(i, b);
    }
}
