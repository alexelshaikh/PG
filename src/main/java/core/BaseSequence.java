package core;

import utils.Pair;
import utils.Permutation;
import utils.fasta.ReadableFASTAFile;
import utils.fasta.WriteableFASTAFile;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseSequence implements Iterable<Base>, Cloneable {

    // enthalpy change in kcal mol-1 (accounts for the energy change during annealing / melting)
    private static final Map<BaseSequence, Pair<Double, Double>> DELTA_H_S__5_TO_3;

    // constant of -0.0108 kcal K-1 ᐧ mol-1 (accounts for helix initiation during annealing / melting)
    private static final double A = -0.0108d;

    // gas constant of 0.00199 kcal K-1 ᐧ mol-1 (constant that scales energy to temperature)
    private static final double R = 0.00199d;

    // oligonucleotide concentration in M or mol L-1 (we use 0.0000005, i.e. 0.5 µM)
    private static final double C = 0.0000005d;

    // = R * ln(0.25d * C)
    private static final double A_PLUS_R_MULTI_LN_C_QUARTER = A + R * Math.log(C / 4d);

    // NA+ sodium ion concentration in M or mol L-1 (we use 0.05, i.e. 50 mM)
    private static final double NA_PLUS_CONCENTRATION = 0.05d;

    // factor multiplicand for log([NA+])
    private static final double NA_PLUS_FACTOR = 16.6d;

    // addition term used for Tm
    private static final double FACTOR_LOG_NA_PLUS_CONCENTRATION = NA_PLUS_FACTOR * Math.log10(NA_PLUS_CONCENTRATION);

    // to convert from kelvin to celsius
    private static final double ZERO_KELVIN_IN_CELSIUS = -273.15d;


    public static final Collector<Base, BaseSequence, BaseSequence>         COLLECTOR_BASE   = new CollectorBaseSeq<>(BaseSequence::new, BaseSequence::append, (seq1, seq2) -> {seq1.append(seq2); return seq1;});
    public static final Collector<CharSequence, BaseSequence, BaseSequence> COLLECTOR_STRING = new CollectorBaseSeq<>(BaseSequence::new, BaseSequence::append, (seq1, seq2) -> {seq1.append(seq2); return seq1;});


    static {
        DELTA_H_S__5_TO_3 = new HashMap<>();
        DELTA_H_S__5_TO_3.put(new BaseSequence("AA"), new Pair<>(-9.1d, -0.0240d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("AT"), new Pair<>(-8.6d, -0.0239d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("TA"), new Pair<>(-6.0d, -0.0169d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("CA"), new Pair<>(-5.8d, -0.0129d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("GT"), new Pair<>(-6.5d, -0.0173d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("CT"), new Pair<>(-7.8d, -0.0208));
        DELTA_H_S__5_TO_3.put(new BaseSequence("GA"), new Pair<>(-5.6d, -0.0135d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("CG"), new Pair<>(-11.9d, -0.0278d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("GC"), new Pair<>(-11.1d, -0.0267d));
        DELTA_H_S__5_TO_3.put(new BaseSequence("GG"), new Pair<>(-11.0d, -0.0266d));
    }

    private List<Base> bases;
    private Map<String, Object> properties;


    public BaseSequence() {
        this.bases = new ArrayList<>();
    }
    public BaseSequence(List<Base> bases) {
        this.bases = bases;
    }
    public BaseSequence(String seq) {
        this();
        append(seq);
    }
    public BaseSequence(BaseSequence... seqs) {
        this();
        for (BaseSequence seq : seqs) {
            append(seq);
        }
    }
    public BaseSequence(Base... bases) {
        this();
        for (Base b : bases) {
            append(b);
        }
    }

    public void insert(int index, Base b) {
        this.bases.add(index, b);
    }

    public void insert(int index, BaseSequence seq) {
        this.bases.addAll(index, seq.bases);
    }

    public void append(char b) {
        this.bases.add(Base.valueOfChar(b));
    }
    public void append(Base b) {
        this.bases.add(b);
    }
    public void append(BaseSequence seq) {
        this.bases.addAll(seq.bases);
    }
    public void append(CharSequence charSequence) {
        int len = charSequence.length();
        for(int i = 0; i < len; i++)
            append(charSequence.charAt(i));
    }

    public void set(int index, Base b) {
        this.bases.set(index, b);
    }

    public void setRange(int i, int j, Base b) {
        for (int k = i; k < j; k++) {
            this.bases.set(k, b);
        }
    }

    public void setRange(int i, int j, BaseSequence seq) {
        for (int k = i; k < j; k++) {
            this.bases.set(k, seq.get(k - i));
        }
    }

    public <T> BaseSequence putProperty(String propertyName, T value) {
        if (properties == null)
            properties = new HashMap<>();
        properties.put(propertyName, value);
        return this;
    }

    public <T> T getProperty(String propertyName) {
        return getProperty(propertyName, () -> null);
    }

    public <T> T getProperty(String propertyName, Supplier<T> orElse) {
        if (properties == null)
            return orElse.get();

        T t = (T) properties.get(propertyName);
        return t != null? t : orElse.get();
    }

    public List<BaseSequence> kmers(int len) {
        int thisLen = length();
        if (len > thisLen)
            throw new RuntimeException("cannot create q grams of len " + len + " for seq of len " + thisLen);

        int sizeLimit = 1 + thisLen - len;
        List<BaseSequence> qGrams = new ArrayList<>(sizeLimit);
        for (int i = 0; i < sizeLimit; i++)
            qGrams.add(window(i, i + len));

        return qGrams;
    }

    public Set<BaseSequence> kmersSet(int len) {
        int thisLen = length();
        if (len > thisLen)
            throw new RuntimeException("cannot create q grams of len " + len + " for seq of len " + thisLen);

        int sizeLimit = 1 + thisLen - len;
        Set<BaseSequence> qGrams = new HashSet<>(sizeLimit);
        for (int i = 0; i < sizeLimit; i++)
            qGrams.add(window(i, i + len));

        return qGrams;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public int length() {
        return this.bases.size();
    }

    public BaseSequence replace(BaseSequence source, BaseSequence target) {
        return clone().replaceInPlace(source, target);
    }


    public BaseSequence replaceInPlace(BaseSequence source, BaseSequence target) {
        int index = Collections.indexOfSubList(bases, source.bases);
        if (index >= 0) {
            BaseSequence before = index == 0? new BaseSequence() : window(0, index);
            BaseSequence after = window(index + source.length());
            List<Base> basesReplaced = new ArrayList<>(before.length() + target.length() + after.length());
            basesReplaced.addAll(before.bases);
            basesReplaced.addAll(target.bases);
            basesReplaced.addAll(after.bases);
            this.bases = basesReplaced;
        }
        return this;
    }


    public BaseSequence[] splitRegex(String regex) {
        String[] splitted = this.toString().split(regex);
        BaseSequence[] seqs = new BaseSequence[splitted.length];
        for (int i = 0; i < splitted.length; i++) {
            seqs[i] = new BaseSequence(splitted[i]);
        }
        return seqs;
    }

    public BaseSequence[] splitInHalves(int numHalves) {
        int lenThis = length();
        int len = lenThis / numHalves;
        if (len == 0)
            throw new RuntimeException("too many halves to split this BaseSequence!");
        BaseSequence[] seqs = new BaseSequence[numHalves];
        int start = 0;
        for (int i = 0, end = len; i < numHalves; i++, start = end, end += len)
            seqs[i] = subSequence(start, end);


        if (start < lenThis) {
            seqs[numHalves - 1] = subSequence(start - len, lenThis);
        }
        return seqs;
    }

    public BaseSequence[] splitEvery(int n) {
        int length = length();
        if (n > length)
            return new BaseSequence[] {this};

        BaseSequence[] split = new BaseSequence[(int) Math.ceil((double) length / n)];
        int i = 0;
        int start = 0;
        for (int end = n; end <= length;) {
            split[i++] = subSequence(start, end);
            start = end;
            end += n;
        }
        if (split[split.length - 1] == null)
            split[split.length - 1] = subSequence(start, length);

        return split;
    }

    public BaseSequence reverse() {
        List<Base> reversed = new ArrayList<>(bases.size());
        ListIterator<Base> it = bases.listIterator(bases.size());
        while(it.hasPrevious())
            reversed.add(it.previous());
        return new BaseSequence(reversed);
    }

    public BaseSequence[] split(BaseSequence seq) {
        return splitToList(seq).toArray(new BaseSequence[0]);
    }

    public List<BaseSequence> splitToList(BaseSequence seq) {
        List<BaseSequence> seqs = new ArrayList<>();
        int lenSeq = seq.length();
        int len = length();
        int hitIndex = Collections.indexOfSubList(bases, seq.bases);
        int nextStartPos = 0;
        int nextEndPos;
        while (hitIndex >= 0) {
            nextEndPos = nextStartPos + hitIndex;
            if (nextEndPos != 0)
                seqs.add(subSequence(nextStartPos, nextEndPos));
            nextStartPos += hitIndex + lenSeq;
            if (nextStartPos >= len)
                break;

            hitIndex = Collections.indexOfSubList(bases.subList(nextStartPos, len), seq.bases);

        }
        if (nextStartPos < len)
            seqs.add(subSequence(nextStartPos, len));

        return seqs;
    }

    public int lastIndexOf(BaseSequence seq) {
        return Collections.lastIndexOfSubList(this.bases, seq.bases);
    }
    public int indexOf(BaseSequence seq) {
        return Collections.indexOfSubList(this.bases, seq.bases);
    }

    public static BaseSequence join(Iterable<BaseSequence> seqs) {
        BaseSequence joined = new BaseSequence();
        for (BaseSequence seq : seqs) {
            joined.append(seq);
        }
        return joined;
    }

    public static BaseSequence join(Iterable<BaseSequence> seqs, BaseSequence joiner) {
        BaseSequence joined = new BaseSequence();
        Iterator<BaseSequence> it = seqs.iterator();
        if (it.hasNext())
            joined.append(it.next());
        while (it.hasNext()) {
            joined.append(joiner);
            joined.append(it.next());
        }
        return joined;
    }


    @Override
    public Iterator<Base> iterator() {
        return bases.listIterator();
    }

    public Stream<Base> stream() {
        return bases.stream();
    }

    public void swap(int i, int j) {
        Base bi = this.bases.get(i);
        this.bases.set(i, this.bases.get(j));
        this.bases.set(j, bi);
    }

    public void clear() {
        this.bases.clear();
    }
    public int count(Base base) {
        return (int) stream().filter(b -> b == base).count();
    }
    public int gcCount() {
        return (int) stream().filter(b -> b == Base.C || b == Base.G).count();
    }

    public Map<Base, Integer> histogram() {
        return stream().collect(Collectors.toMap(b -> b, b -> 1, Integer::sum));
    }

    public BaseSequence subSequence(int i, int j) {
        return new BaseSequence(new ArrayList<>(this.bases.subList(i, j)));
    }
    public BaseSequence subSequence(int i) {
        return subSequence(i, length());
    }

    public BaseSequence window(int i, int j) {
        return new BaseSequence(this.bases.subList(i, j));
    }
    public BaseSequence window(int i) {
        return new BaseSequence(this.bases.subList(i, length()));
    }

    public boolean contains(BaseSequence seq) {
        return Collections.indexOfSubList(this.bases, seq.bases) >= 0;
    }
    public boolean contains(BaseSequence seq, int fromIndex) {
        return window(fromIndex).contains(seq);
    }

    public boolean contains(String seq) {
        return contains(new BaseSequence(seq));
    }

    public BaseSequence complement() {
        return stream().map(Base::complement).collect(COLLECTOR_BASE);
    }

    public int homopolymerScore(int threshold) {
        int[] homopolymerIndexes = indexOfHomopolymersAboveThreshold(threshold);
        int sum = 0;
        // add the square to add penalty for long hps
        for(int index : homopolymerIndexes) {
            int hpLen = lengthOfHomopolymerAtIndex(index);
            sum += hpLen * hpLen;
        }
        return sum;
    }

    public int[] indexOfHomopolymersAboveThreshold(int threshold) {
        List<Integer> indexes = new ArrayList<>();
        int startIndex;
        int currentIndex = 0;
        int currentStreak = 1;
        Base lastBase = this.bases.get(0).complement();
        for (Base currentBase: this.bases) {
        	boolean stillInHomopolymer = currentBase.equals(lastBase);
            if (stillInHomopolymer) {
                currentStreak += 1;
            }
            if (currentIndex == this.bases.size() - 1){
            	stillInHomopolymer = false;
            	currentIndex++;
            }

            if (currentStreak >= threshold - 1 && !stillInHomopolymer) {
                startIndex = currentIndex - currentStreak;
                if (startIndex < 2)
                    indexes.add(1);
                else
                    indexes.add(startIndex - 1);
                currentStreak = 0;
            } else if(!stillInHomopolymer) {
                currentStreak = 0;
            }
            currentIndex++;
            lastBase = currentBase;
        }
        return indexes.stream().mapToInt(i->i).toArray();
    }

    public int indexOfLongestHomopolymer() {
        int longest = 1;
        int startIndex = 0;
        int currentIndex = 0;
        int currentStreak = 1;
        Base lastBase = this.bases.get(0).complement();
        for (Base currentBase: this.bases) {
	        boolean stillInHomopolymer = currentBase.equals(lastBase);
            if (stillInHomopolymer) {
                currentStreak += 1;
            }
            if(currentIndex == this.bases.size() - 1) {
	            stillInHomopolymer = false;
	            currentIndex++;
            }

            if (currentStreak > longest && !stillInHomopolymer) {
                startIndex = currentIndex - currentStreak;
                longest = currentStreak;
                currentStreak = 0;
            } else if (!stillInHomopolymer){
                currentStreak = 0;
            }
            currentIndex++;
            lastBase = currentBase;
        }
        if (startIndex < 2)
            return 1;
        else
            return startIndex - 1;
    }

    public int lengthOfHomopolymerAtIndex(int index) {
        Base hpBase = this.bases.get(index);
        int i = 0;
        while (hpBase == this.bases.get(index + i) && (i + index) < this.bases.size() - 1)
            i++;
        return i;
    }

    public BaseSequence permute(Permutation p) {
        return new BaseSequence(p.apply(this.bases));
    }

	public BaseSequence permuteInPlace(Permutation p) {
        p.applyInPlace(this.bases);
        return this;
    }

	public float gcContent() {
		return gcContentOf(bases);
    }
    public double gcContentAsDouble() {
        return gcContent();
    }

    private static float gcContentOf(List<Base> list) {
        return (float) list.stream().filter(b -> b == Base.G || b == Base.C).count() / list.size();
    }


    public float[] gcContentWindowed(int windowSize) {
        int len = length();
        int windowCount = len/windowSize;
        if (len % windowSize >= 10)
            windowCount = len/windowSize + 1;

        float[] gcs = new float[windowCount];
        for (int start = 0, end = windowSize, c = 0; start < len; start = end, end += windowSize, c++) {
            if (end > len)
                end = len;
            if(end - start >= 10)
                gcs[c] = gcContentOf(this.bases.subList(start, end));
        }
        return gcs;
    }

    public float gcWindow(int i, int j) {
        return gcContentOf(bases.subList(i, Math.min(j, length())));
    }

    public float hammingDistance(BaseSequence seq) {
        int len = this.length();
        int thatLen = seq.length();
        int minLen = Math.min(len, thatLen);
        float dist = Math.abs(len - thatLen);
        for (int i = 0; i < minLen; i++) {
            if (seq.get(i) != get(i)) {
                dist++;
            }
        }

        return dist / Math.max(len, thatLen);
    }

    public float editDistance(BaseSequence seq) {
        int maxLen = Math.max(length(), seq.length());
        return levenshteinDistance(seq, maxLen) / maxLen;
    }
    public double editDistanceAsDouble(BaseSequence seq) {
        return editDistance(seq);
    }

    public float jaccardDistance(BaseSequence seq, int qgramLength) {
        Set<BaseSequence> s1 = this.kmersSet(qgramLength);
        Set<BaseSequence> s2 = seq.kmersSet(qgramLength);
        Set<BaseSequence> union = new HashSet<>(s1);
        union.addAll(s2);

        s1.retainAll(s2);
        return 1.0f - (float) s1.size() / union.size();
    }

    public static void appendFASTA(String path, BaseSequence seq) {
        try (WriteableFASTAFile aff = new WriteableFASTAFile(path)) {
            aff.append(seq);
        }
    }

    public static void appendFASTA(String path, Iterable<BaseSequence> seqs) {
        try (WriteableFASTAFile aff = new WriteableFASTAFile(path)) {
            aff.append(seqs);
        }
    }

    public static List<BaseSequence> readFASTA(String path) {
        try (ReadableFASTAFile rff = new ReadableFASTAFile(path)) {
           return rff.readRemainingSeqs();
        }
    }

    public static List<ReadableFASTAFile.Entry> readFASTAEntries(String path) {
        try (ReadableFASTAFile aff = new ReadableFASTAFile(path)) {
            return aff.readRemaining();
        }
    }

    public static void appendFASTA(String path, BaseSequence seq, String caption) {
        try (WriteableFASTAFile aff = new WriteableFASTAFile(path)) {
            aff.append(seq, caption);
        }
    }

    public BaseSequence removeSubSequence(BaseSequence subSeq) {
        int startIndex = Collections.indexOfSubList(this.bases, subSeq.bases);
        BaseSequence result = new BaseSequence();
        if (startIndex > 0)
            result.bases.addAll(this.bases.subList(0, startIndex));

        result.bases.addAll(this.bases.subList(startIndex + subSeq.length(), this.bases.size()));
        return new BaseSequence(result);
    }

	public void removeBasesAtIndex(int basesCount, int index) {
    	// Not index + i because list is being modified
		for (int i = 0; i < basesCount; i++) {
			this.bases.remove(index);
		}
	}

	public int lcs(BaseSequence seq){
		int m = length();
        int n = seq.length();

        int max = 0;
        int[][] dp = new int[m][n];

        for (int i = 0; i < m && m - i > max; i++) {
            for (int j = 0; j < n && n - j > max; j++) {
                if (get(i) == seq.get(j)) {
                    if (i==0 || j==0)
                        dp[i][j]=1;
                    else
                        dp[i][j] = dp[i-1][j-1]+1;

                    if (max < dp[i][j])
                        max = dp[i][j];
                }
            }
        }
        return max;
    }

    public int lcsWithGaps(BaseSequence seq) {
        int seqLen = seq.length();
        int len = length();
        int[][] dp = new int[len + 1][seqLen + 1];
        for (int i = 1; i < dp.length; i++) {
            for (int j = 1; j < dp[0].length; j++) {
                if (get(i - 1) == seq.get(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[len][seqLen];
    }

    public double tm() {
        if (length() <= 14)
            return basicTm();
        return nearestNeighborsTm();
    }

    private double basicTm() {
        Map<Base, Integer> histogram = histogram();
        return 2 * (histogram.get(Base.A) + histogram.get(Base.T)) + 4 * (histogram.get(Base.C) + histogram.get(Base.G)) - 7;
    }

    private double nearestNeighborsTm() {
        int len = length();
        double sumDeltaH = 0d;
        double sumDeltaS = 0d;
        for (int i = 1; i < len; i++) {
            BaseSequence seq = window(i - 1, i + 1);
            Pair<Double, Double> hs = DELTA_H_S__5_TO_3.get(seq);
            if (hs == null)
                hs = DELTA_H_S__5_TO_3.get(seq.complement().reverse());

            sumDeltaH += hs.getT1();
            sumDeltaS += hs.getT2();
        }

        return sumDeltaH / (A_PLUS_R_MULTI_LN_C_QUARTER + sumDeltaS) + ZERO_KELVIN_IN_CELSIUS + FACTOR_LOG_NA_PLUS_CONCENTRATION;
    }

    public int countMatches(BaseSequence slice, boolean consecutive) {
        int lenThis = length();
        int sliceLen = slice.length();
        if (lenThis < sliceLen)
            return 0;

        int start = 0;
        int end = sliceLen;
        int count = 0;
        int maxConsecutiveCount = 0;
        int consecutiveCount = 0;

        while (end < lenThis) {
            if (bases.subList(start, end).equals(slice.bases)) {
                count += 1;
                consecutiveCount += 1;
                start += sliceLen;
                end += sliceLen;
            } else {
                consecutiveCount = 0;
                start += 1;
                end += 1;
            }
            maxConsecutiveCount = Math.max(maxConsecutiveCount, consecutiveCount);
        }

        return consecutive? maxConsecutiveCount : count;
    }


    public boolean isEmpty() {
        return this.bases.isEmpty();
    }

    private float levenshteinDistance(BaseSequence seq, int limit) {
        int len = length();
        int seqLen = seq.length();
        if (len == 0)
            return seqLen;
        if (seqLen == 0)
            return len;

        int[] v0 = new int[seqLen + 1];
        int[] v1 = new int[seqLen + 1];
        int[] vtemp;

        for (int i = 0; i < v0.length; i++)
            v0[i] = i;

        for (int i = 0; i < len; i++) {
            v1[0] = i + 1;
            int minv1 = v1[0];
            for (int j = 0; j < seqLen; j++) {
                int cost = 1;
                if (get(i) == seq.get(j)) {
                    cost = 0;
                }
                v1[j + 1] = Math.min(v1[j] + 1, Math.min(v0[j + 1] + 1, v0[j] + cost));
                minv1 = Math.min(minv1, v1[j + 1]);
            }

            if (minv1 >= limit)
                return limit;
            vtemp = v0;
            v0 = v1;
            v1 = vtemp;
        }

        return v0[seqLen];
    }

    public int longestHomopolymer() {
        int longest = 1;
        int current = 1;
        int len = length();
        for (int i = 1; i < len; i++) {
            if (get(i) == get(i - 1)) {
                current++;
            }
            else {
                longest = Math.max(longest, current);
                current = 1;
            }
        }
        return Math.max(longest, current);
    }

    public Base get(int i) {
        return this.bases.get(i);
    }

    public List<Base> getBases() {
        return bases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (o instanceof String s)
            return toString().equals(s);
        if (getClass() != o.getClass())
            return false;

        return this.bases.equals(((BaseSequence) o).bases);
    }

    @Override
    public int hashCode() {
        return this.bases.hashCode();
    }

    public static BaseSequence random(int len, double gcContent) {
        return new BaseSequence(Stream.generate(() -> Base.randomGC(gcContent)).limit(len).collect(Collectors.toList()));
    }

    public static BaseSequence random(int len) {
        return random(len, 0.5);
    }

    public static BaseSequence random() {
        return random((int) (50.0d + Math.random() * 151.0d), 0.5);
    }

    @Override
    public String toString() {
        return stream().map(Base::name).collect(Collectors.joining());
    }

    @Override
    public BaseSequence clone() {
        return new BaseSequence(this);
    }


    private static class CollectorBaseSeq<T, A, R> implements Collector<T, A, R> {
        private static final Set<Characteristics> ID_FINISH = Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));

        private final Supplier<A>          supplier;
        private final BiConsumer<A, T>     accumulator;
        private final BinaryOperator<A>    combiner;
        private final Function<A, R>       finisher;
        private final Set<Characteristics> characteristics;

        CollectorBaseSeq(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner) {
            this.supplier        = supplier;
            this.accumulator     = accumulator;
            this.combiner        = combiner;
            this.finisher        = f -> (R) f;
            this.characteristics = ID_FINISH;
        }
        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }
        @Override
        public Supplier<A> supplier() {
            return supplier;
        }
        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }
        @Override
        public Function<A, R> finisher() {
            return finisher;
        }
        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
}
