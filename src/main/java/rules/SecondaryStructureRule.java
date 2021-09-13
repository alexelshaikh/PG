package rules;

import core.BaseSequence;
import utils.DGClient;

public class SecondaryStructureRule implements DNARule {
    public static final float DEFAULT_TEMP = 25.0f;
    private final float temp;
    private final DGClient client;

    public SecondaryStructureRule(float temp) {
        super();
        this.temp = temp;
        this.client = DGClient.getInstance();
    }

    public SecondaryStructureRule() {
        this(DEFAULT_TEMP);
    }

    @Override
    public float evalErrorProbability(BaseSequence seq) {
        return 1.0f / (1.0f + (float) Math.exp(client.dg(seq, temp) + 4.0f));
    }
}
