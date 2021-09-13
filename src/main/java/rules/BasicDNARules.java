package rules;

import core.Base;
import core.BaseSequence;
import java.util.function.Function;

public class BasicDNARules extends DNARulesCollection {
    public static final int MIN_GC_WINDOW_SIZE = 10;
    public static final Function<BaseSequence, Integer> COMPUTE_GC_WINDOW_SIZE = seq -> Math.max(seq.length() >> 2, MIN_GC_WINDOW_SIZE);
    public static final int REPEATABLE_SEQ_NOT_STRICT_SIZE = 9;
    public static final int REPEATABLE_SEQ_STRICT_SIZE = 20;
    public static final int MAX_HP_LEN = 6;
    public static final int MAX_STRAND_LEN = 170;
    public static final float MIN_GC_CONTENT = 0.45f;
    public static final float MAX_GC_CONTENT = 0.55f;


    public BasicDNARules() {
        this(false);
    }

    public BasicDNARules(boolean withDG) {
        this(withDG, SecondaryStructureRule.DEFAULT_TEMP);
    }

    public BasicDNARules(boolean withDG, float temp) {
        super();
        addOrReplaceRule("GC Error", this::gcError);
        addOrReplaceRule("HP Error", this::hpError);
        addOrReplaceRule("GC Window Error", this::gcWindowError);
        addOrReplaceRule("Microsatellites Run 2 Error", this::microSatellitesRun2Error);
        addOrReplaceRule("Microsatellites Run 3 Error", this::microSatellitesRun3Error);
        addOrReplaceRule("Repeatable Region Error(unstrict)", seq -> this.repeatableRegionError(seq, REPEATABLE_SEQ_NOT_STRICT_SIZE, false));
        addOrReplaceRule("Repeatable Region Error (strict)", seq -> this.repeatableRegionError(seq, REPEATABLE_SEQ_STRICT_SIZE, true));
        if (withDG)
            addOrReplaceRule("dg_rule", new SecondaryStructureRule(temp));
    }

    public float gcError(BaseSequence seq) {
        float gc = seq.gcContent();

        if (gc >= MIN_GC_CONTENT && gc <= MAX_GC_CONTENT)
            return 0.0f;

        // 5% deviation
        if (gc >= MIN_GC_CONTENT - 0.05f && gc <= MAX_GC_CONTENT + 0.05f)
            return 0.1f;

        // 10% deviation
        if (gc >= MIN_GC_CONTENT - 0.10f && gc <= MAX_GC_CONTENT + 0.10f)
            return 0.4f;

        // 15% deviation
        if (gc >= MIN_GC_CONTENT - 0.15f && gc <= MAX_GC_CONTENT + 0.15f)
            return 0.8f;

        return 1.0f;
    }

    public float hpError(BaseSequence seq) {
        return seq.homopolymerScore(MAX_HP_LEN);
    }

    public float gcWindowError(BaseSequence seq) {
        float[] windows = seq.gcContentWindowed(COMPUTE_GC_WINDOW_SIZE.apply(seq));
        float min = 1.0f;
        float max = 0.0f;
        for (float gc : windows) {
            if (gc < min)
                min = gc;
            if (gc > max)
                max = gc;
        }

        float diff = max - min;
        return Math.min(1.0f, diff * diff * 5.0f);
    }

    public float microSatellitesRun2Error(BaseSequence seq) {
        float err = 0.0f;
        for (Base b1 : Base.values())
            for (Base b2 : Base.values())
                err += microSatellitesCountsError(seq.countMatches(new BaseSequence(b1, b2), true));

        return err;
    }

    public float microSatellitesRun3Error(BaseSequence seq) {
        float err = 0.0f;
        for (Base b1 : Base.values())
            for (Base b2 : Base.values())
                for (Base b3 : Base.values())
                    err += microSatellitesCountsError(seq.countMatches(new BaseSequence(b1, b2, b3), true));

        return err;
    }

    protected float microSatellitesCountsError(int count) {
        float err = 0.0f;

        if (count > 10)
            err += 0.001f;

        if (count > 15)
            err += 0.002f;

        if (count > 20)
            err += 0.003f;

        if (count > 25)
            err += 0.004f;

        if (count > 30)
            err += 0.005f;

        if (count > 35)
            err += 0.006f;

        if (count > 40)
            err += 0.007f;

        if (count > 45)
            err += 0.008f;

        if (count > 50)
            err += 0.009f;

        if (count > 55)
            err += 0.01f;

        if (count > 60)
            err += 0.011f;

        if (count > 65)
            err += 0.012f;

        if (count > 70)
            err += 0.013f;

        if (count > 75)
            err += 0.014f;

        if (count > 80)
            err += 0.015f;

        if (count > 85)
            err += 0.016f;

        if (count > 90)
            err += 0.017f;

        if (count > 95)
            err += 0.018f;

        if (count > 100)
            err += 0.019f;

        return err;
    }

    public float longStrandsError(BaseSequence seq) {
        return seq.length() > MAX_STRAND_LEN ? 0.2f : 0.0f;
    }

    public float repeatableRegionError(BaseSequence seq, int size, boolean strict) {
        int hits = 1;
        int len = seq.length();
        for (int startPos = 0; startPos < len; startPos++) {
            int end_pos = startPos + size;
            if (end_pos > len)
                break;

            BaseSequence subSeq = seq.window(startPos, end_pos);
            if (seq.window(startPos + 1, len).countMatches(subSeq, false) > 0) {
                hits += 1;
                if (strict)
                    return 1.0f;
            }
        }

        if (strict)
            return 0.0f;
        float f = (float) hits * size / len;
        return f > 0.44f ? 1.0f : 0.5f * f;
    }
}

