package rules;

import core.BaseSequence;

public interface DNARule {
    float evalErrorProbability(BaseSequence seq);
}
