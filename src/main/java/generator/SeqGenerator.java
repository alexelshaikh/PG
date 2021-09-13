package generator;

import core.BaseSequence;

import java.util.stream.Stream;

public interface SeqGenerator {
    BaseSequence generate();
    default Stream<BaseSequence> stream() {
        return Stream.generate(this::generate);
    }
}
