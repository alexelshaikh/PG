package utils;

import core.BaseSequence;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LSH {
    private final int k;
    private final int b;
    private final int bandSize;
    private final List<Map<String, Set<BaseSequence>>> bands;
    private final ReadWriteLock[] bandLocks;
    private final PseudoPermutation[] permutations;

    /**
     * Creates a tread-safe LSH instance that supports concurrent insertion and querying
     * @param k the kmer length
     * @param r the number of hash functions (permutations)
     * @param b the number of bands. Note that for r=120 and b=10, the resulting bandSize is 12 i.e. 12 hash functions per band/signature
     */
    public LSH(int k, int r, int b) {
        if (r % b != 0)
            throw new RuntimeException("r must be a multiple of b");
        if (k > 33)
            throw new RuntimeException("this LSH only supports k-mers up to k = 33");

        this.k = k;
        this.b = b;
        this.bandSize = r / b;

        long kMers = (long) Math.pow(4, k);
        this.permutations = Stream.iterate(new PseudoPermutation(kMers, kMers), p -> new PseudoPermutation(kMers, p.getP())).limit(r).toArray(PseudoPermutation[]::new);
        this.bands = Stream.generate((Supplier<Map<String, Set<BaseSequence>>>) HashMap::new).limit(b).toList();
        this.bandLocks = Stream.generate(ReentrantReadWriteLock::new).limit(b).toArray(ReadWriteLock[]::new);
    }

    public void insert(Iterable<BaseSequence> it) {
        it.forEach(this::insert);
    }

    public void insertParallel(Iterable<BaseSequence> it) {
        FuncUtils.stream(it).parallel().forEach(this::insert);
    }

    public void insert(BaseSequence seq) {
        var sigs = signatures(seq);
        Lock lock;
        for (int band = 0; band < b; band++) {
            var map = bands.get(band);
            lock = bandLocks[band].writeLock();
            lock.lock();
            map.computeIfAbsent(sigs[band], k1 -> new HashSet<>()).add(seq);
            lock.unlock();
        }
    }

    public long[] minHashes(BaseSequence seq) {
        List<BaseSequence> kmers = seq.kmers(k);
        int size = kmers.size();
        long[] shingles = new long[size];
        for (int i = 0; i < size; i++)
            shingles[i] = initialRowId(kmers.get(i));

        PseudoPermutation p;
        long[] minHashes = new long[permutations.length];
        long permHash;
        long minHash;
        for (int i = 0; i < permutations.length; i++) {
            p = permutations[i];
            minHash = Long.MAX_VALUE;
            for (long shingle : shingles) {
                permHash = p.apply(shingle);
                if (permHash == 0L) {
                    minHash = 0L;
                    break;
                }
                if (permHash < minHash)
                    minHash = permHash;
            }
            minHashes[i] = minHash;
        }
        return minHashes;
    }


    public Set<BaseSequence> similarSeqs(BaseSequence seq) {
        return similarSeqs(seq, Integer.MAX_VALUE);
    }

    public Set<BaseSequence> similarSeqs(BaseSequence seq, int maxCount) {
        var sigs = signatures(seq);
        Set<BaseSequence> result = new HashSet<>();
        Set<BaseSequence> matches;
        Lock lock;
        for (int band = 0; band < b; band++) {
            var map = bands.get(band);
            lock = bandLocks[band].readLock();
            lock.lock();
            matches = map.get(sigs[band]);
            if (matches != null) {
                result.addAll(matches);
                if (result.size() >= maxCount) {
                    lock.unlock();
                    return result;
                }
            }
            lock.unlock();
        }
        return result;
    }

    public String[] signatures(BaseSequence seq) {
        var minHashes = minHashes(seq);
        String[] sigs = new String[b];
        StringBuilder sb;
        int offset = 0;
        for (int band = 0; band < b; band++) {
            sb = new StringBuilder();
            for (int m = 0; m < bandSize; m++)
                sb.append(minHashes[m + offset]);

            sigs[band] = sb.toString();
            offset += bandSize;
        }
        return sigs;
    }

    private static long initialRowId(BaseSequence kmer) {
        int len = kmer.length();
        long id = 0L;
        int order;
        for (int i = 0; i < len; i++) {
            order = switch (kmer.get(i)) {
                case A -> 0;
                case C -> 1;
                case G -> 2;
                case T -> 3;
            };
            if (order == 0)
                continue;
            id += (long) (order * Math.pow(4, i));
        }

        return id;
    }

    public int getK() {
        return k;
    }

    public int getB() {
        return b;
    }

    public int getBandSize() {
        return bandSize;
    }
}
