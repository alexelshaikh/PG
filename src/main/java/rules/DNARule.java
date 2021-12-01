package rules;

import core.BaseSequence;

/**
 * The interface for DNA rules. The function evalErrorProbability has to be implemented in the subclasses.
 */
public interface DNARule {

    /**
     * @param seq the BaseSequence.
     * @return the error calculated for the given DNA sequence.
     */
    float evalErrorProbability(BaseSequence seq);
}
