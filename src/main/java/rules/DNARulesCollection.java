package rules;

import core.BaseSequence;
import java.util.*;

public class DNARulesCollection implements DNARule {

    private static final String NO_NAME_RULE_PREFIX = "NO_NAME_RULE_";

    private int noNameRuleCounter;
    protected Map<String, DNARule> rules;

    public DNARulesCollection() {
        this.rules = new HashMap<>();
        this.noNameRuleCounter = 0;
    }

    public float evalErrorByLimit(BaseSequence seq, float maxError) {
        return evalErrorByLimitByRules(seq, maxError, rules.values());
    }

    public static float evalErrorByLimitByRules(BaseSequence seq, float maxError, Collection<DNARule> rs) {
        float sumError = 0.0f;
        for (DNARule rule : rs) {
            sumError += rule.evalErrorProbability(seq);
            if (sumError > maxError)
                return sumError;
        }
        return sumError;
    }

    public void addRule(DNARule rule) {
        addOrReplaceRule(NO_NAME_RULE_PREFIX + (noNameRuleCounter++), rule);
    }

    public void addOrReplaceRule(String ruleName, DNARule rule) {
        this.rules.put(ruleName, rule);
    }

    public DNARule removeRule(String ruleName) {
        return this.rules.remove(ruleName);
    }

    public Map<String, DNARule> getRules() {
        return Collections.unmodifiableMap(rules);
    }

    @Override
    public float evalErrorProbability(BaseSequence seq) {
        return evalErrorProbabilityByRules(seq, rules.values());
    }


    public static float evalErrorProbabilityByRules(BaseSequence seq, Collection<DNARule> rs) {
        float totalError = 0.0f;
        for (DNARule rule : rs)
            totalError += rule.evalErrorProbability(seq);

        return totalError;
    }
}
