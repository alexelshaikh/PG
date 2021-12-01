package generator;

import core.BaseSequence;

import java.util.stream.Stream;

public interface SeqGenerator {
    /**
     * @return a BaseSequence.
     */
    BaseSequence generate();
    /**
     * @return a stream of BaseSequence that are generated.
     */
    default Stream<BaseSequence> stream() {
        return Stream.generate(this::generate);
    }
}
