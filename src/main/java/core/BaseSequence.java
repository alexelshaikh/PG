package core;

import utils.Pair;
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

    /**
     * Creates an empty BaseSequence
     */
    public BaseSequence() {
        this.bases = new ArrayList<>();
    }
    /**
     * Creates a BaseSequence containing a list of provided DNA bases.
     * @param bases the list of DNA bases that will be added to this instance.
     */
    public BaseSequence(List<Base> bases) {
        this.bases = bases;
    }

    /**
     * Creates a BaseSequence containing the bases provided in the string.
     * @param seq the string of DNA bases that will be parsed and added to this instance.
     */
    public BaseSequence(String seq) {
        this();
        append(seq);
    }
    /**
     * Creates a BaseSequence containing the DNA sequences provided.
     * @param seqs the array of BaseSequence that will be each added into this instance.
     */
    public BaseSequence(BaseSequence... seqs) {
        this();
        for (BaseSequence seq : seqs) {
            append(seq);
        }
    }
    /**
     * Creates a BaseSequence containing an array of provided DNA bases.
     * @param bases the array of DNA bases that will be added to this instance.
     */
    public BaseSequence(Base... bases) {
        this();
        for (Base b : bases) {
            append(b);
        }
    }

    /**
     * Inserts a base at the specified index.
     * @param index the index where the base will be inserted.
     * @param b the base that will be inserted.
     */
    public void insert(int index, Base b) {
        this.bases.add(index, b);
    }

    /**
     * Inserts a BaseSequence at the specified index.
     * @param index the index where the BaseSequence will be inserted.
     * @param seq the base that will be inserted.
     */
    public void insert(int index, BaseSequence seq) {
        this.bases.addAll(index, seq.bases);
    }


    /**
     * Appends a character representing a DNA base to this instance.
     * @param b the character representing a DNA base.
     */
    public void append(char b) {
        this.bases.add(Base.valueOfChar(b));
    }

    /**
     * Appends a DNA base to this instance.
     * @param b the DNA base.
     */
    public void append(Base b) {
        this.bases.add(b);
    }
    /**
     * Appends a BaseSequence to this instance.
     * @param seq the BaseSequence.
     */
    public void append(BaseSequence seq) {
        this.bases.addAll(seq.bases);
    }

    /**
     * Appends a CharSequence representing a DNA sequence to this instance.
     * @param charSequence the CharSequence representing a DNA sequence.
     */
    public void append(CharSequence charSequence) {
        int len = charSequence.length();
        for(int i = 0; i < len; i++)
            append(charSequence.charAt(i));
    }

    /**
     * Puts a property to this instance.
     * @param propertyName the property name.
     * @param value the property's value.
     */
    public <T> BaseSequence putProperty(String propertyName, T value) {
        if (properties == null)
            properties = new HashMap<>();
        properties.put(propertyName, value);
        return this;
    }

    /**
     * @param propertyName the property name.
     * @return the value of the given property.
     */
    public <T> T getProperty(String propertyName) {
        return getProperty(propertyName, () -> null);
    }

    /**
     * @param propertyName the property name.
     * @param orElse is the given property does not exist, will return this instead.
     * @return the value of the given property. If no value exists, returns orElse instead.
     */
    public <T> T getProperty(String propertyName, Supplier<T> orElse) {
        if (properties == null)
            return orElse.get();

        T t = (T) properties.get(propertyName);
        return t != null? t : orElse.get();
    }

    /**
     * @param len the k-mer length.
     * @return the list of k-mers (can contain duplicates).
     */
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

    /**
     * @param len the k-mer length.
     * @return the set of k-mers (cannot contain duplicates).
     */
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


    /**
     * @return the number of DNA bases in this instance.
     */
    public int length() {
        return this.bases.size();
    }

    /**
     * @return an Iterator of Base for the DNA bases in this instance.
     */
    @Override
    public Iterator<Base> iterator() {
        return bases.listIterator();
    }

    /**
     * @return a stream of Base for the DNA bases in this instance.
     */
    public Stream<Base> stream() {
        return bases.stream();
    }


    /**
     * Returns a an immutable subsequence of this instance.
     * @param i the starting (inclusive) index.
     * @param j the ending (exclusive) index.
     * @return the subsequence at indexes [i..j) of this instance.
     */
    public BaseSequence window(int i, int j) {
        return new BaseSequence(this.bases.subList(i, j));
    }

    /**
     * Returns a score for homopolymers in this instance. The higher the score, the longer and more abundant homopolymers are found in this instance.
     * @param threshold the smallest homopolymer length to search for.
     * @return the homopolymer score.
     */
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

    /**
     * @param threshold the smallest homopolymer length to search for.
     * @return the indexes of found homopolymers.
     */
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

    /**
     * @param index the index at which the present homopolymer's length is calculated.
     * @return the length of the homopolymer starting at index.
     */
    public int lengthOfHomopolymerAtIndex(int index) {
        Base hpBase = this.bases.get(index);
        int i = 0;
        while (hpBase == this.bases.get(index + i) && (i + index) < this.bases.size() - 1)
            i++;
        return i;
    }


    /**
     * @return the GC content of this instance.
     */
	public float gcContent() {
		return gcContentOf(bases);
    }
    private static float gcContentOf(List<Base> list) {
        return (float) list.stream().filter(b -> b == Base.G || b == Base.C).count() / list.size();
    }

    /**
     * @param windowSize the window size of which the GC content is calculated.
     * @return GC content for each window.
     */
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

    /**
     * @param seq the BaseSequence for which to calculate the Jaccard distance to.
     * @param qgramLength the k-mers length.
     * @return the Jaccard distance.
     */
    public float jaccardDistance(BaseSequence seq, int qgramLength) {
        Set<BaseSequence> s1 = this.kmersSet(qgramLength);
        Set<BaseSequence> s2 = seq.kmersSet(qgramLength);
        Set<BaseSequence> union = new HashSet<>(s1);
        union.addAll(s2);

        s1.retainAll(s2);
        return 1.0f - (float) s1.size() / union.size();
    }

    /**
     * @param slice the BaseSequence.
     * @param consecutive if set true, then only consecutive repeats of slice will be counted.
     * @return the count of slice in this instance.
     */
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

    /**
     * @param i the index.
     * @return the DNA base at the specified index.
     */
    public Base get(int i) {
        return this.bases.get(i);
    }

    /**
     * Checks if this instance is equal to o.
     * @param o the other object.
     * @return true, if o and this instance contains the same DNA bases in the same order.
     */
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

    /**
     * @return a hash value for this instance.
     */
    @Override
    public int hashCode() {
        return this.bases.hashCode();
    }

    /**
     * Generates a random BaseSequence with lgiven length and GC content.
     * @param len the length of the returned BaseSequence.
     * @param gcContent the target GC content.
     * @return the random BaseSequence.
     */
    public static BaseSequence random(int len, double gcContent) {
        return new BaseSequence(Stream.generate(() -> Base.randomGC(gcContent)).limit(len).collect(Collectors.toList()));
    }

    /**
     * Converts the BaseSequence to a string representing the DNA bases.
     * @return a string representation of this instance.
     */
    @Override
    public String toString() {
        return stream().map(Base::name).collect(Collectors.joining());
    }

    /**
     * @return a copy of this instance.
     */
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
