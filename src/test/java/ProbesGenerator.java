import generator.SeqGenerator;
import generator.SeqGeneratorWithRulesProb;
import generator.SeqGeneratorWithRulesSafe;
import generator.probes.ProbesGeneratorLSH;
import rules.BasicDNARules;
import utils.FuncUtils;
import utils.LSH;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProbesGenerator {

    public static void main(String[] args) {
        Supplier<SeqGenerator> safe = () -> new ProbesGeneratorLSH(new SeqGeneratorWithRulesSafe(60, 0.5f, 0.1f, 0.5f, new BasicDNARules()), new LSH(4, 200, 20), 0.4f);
        Supplier<SeqGenerator> prob = () -> new ProbesGeneratorLSH(new SeqGeneratorWithRulesProb(0.5f, new BasicDNARules()), new LSH(4, 200, 20), 0.4f);

        int size = 10_000;
        test(safe.get(), size / 2);
        test(prob.get(), size / 2);
        System.gc();
        FuncUtils.safeRun(() -> Thread.sleep(3000L));
        test(safe.get(), size);
        test(prob.get(), size);
    }

    public static void test(SeqGenerator gen, int size) {
        time(() -> gen.stream().limit(size).collect(Collectors.toList()));
    }

    public static void time(Callable<?> r) {
        long t = System.currentTimeMillis();
        FuncUtils.safeCall(r);
        System.out.println("time=" + (System.currentTimeMillis() - t) / 1000f + " secs");
    }
}
