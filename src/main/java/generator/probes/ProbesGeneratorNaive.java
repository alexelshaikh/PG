package generator.probes;

import core.BaseSequence;
import generator.SeqGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProbesGeneratorNaive implements SeqGenerator {
    private final float minDist;
    private final SeqGenerator generator;
    private final List<BaseSequence> seqs;
    private final ReadWriteLock lock;
    private final int k;

    public ProbesGeneratorNaive(SeqGenerator generator, int k, float minDist) {
        this.generator = generator;
        this.minDist = minDist;
        this.seqs = new ArrayList<>();
        this.k = k;
        this.lock = new ReentrantReadWriteLock();
    }

    private boolean tryAdd(BaseSequence seq) {
        lock.readLock().lock();
        int count = seqs.size();
        if (seqs.stream().parallel().map(can -> can.jaccardDistance(seq, k)).min(Float::compare).orElse(1.0f) >= minDist) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            if (seqs.subList(count, seqs.size()).stream().map(can -> can.jaccardDistance(seq, k)).min(Float::compare).orElse(1.0f) >= minDist) {
                seqs.add(seq);
                lock.writeLock().unlock();
                return true;
            }
            else {
                lock.writeLock().unlock();
                return false;
            }
        }
        lock.readLock().unlock();
        return false;
    }

    @Override
    public BaseSequence generate() {
        return generator.stream().filter(this::tryAdd).findFirst().orElseThrow();
    }
}
