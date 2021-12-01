package rules;

import core.BaseSequence;
import utils.DGClient;

public class SecondaryStructureRule implements DNARule {
    public static final float DEFAULT_TEMP = 25.0f;
    private final float temp;
    private final DGClient client;

    /**
     * Creates an instance that checks the error via the dg server.
     * @param temp the temperature.
     */
    public SecondaryStructureRule(float temp) {
        super();
        this.temp = temp;
        this.client = DGClient.getInstance();
    }

    @Override
    public float evalErrorProbability(BaseSequence seq) {
        return 1.0f / (1.0f + (float) Math.exp(client.dg(seq, temp) + 4.0f));
    }
}
