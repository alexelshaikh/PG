package packaging;

import core.BaseSequence;
import generator.SeqGenerator;
import generator.SeqGeneratorWithRulesProb;
import generator.probes.ProbesGeneratorLSH;
import generator.probes.ProbesGeneratorNaive;
import rules.BasicDNARules;
import generator.SeqGeneratorWithRulesSafe;
import rules.DNARulesCollection;
import utils.ArgumentConverter;
import utils.ArgumentParser;
import utils.FuncUtils;
import utils.LSH;
import utils.csv.BufferedCsvWriter;
import utils.fasta.WriteableFASTAFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class ProbesProducerPackage {
    public static final String COUNT_PARAM                = "count";
    public static final String LEN_PARAM                  = "len";
    public static final String GC_PARAM                   = "gc";
    public static final String MAX_GC_DEV_PARAM           = "d_gc";
    public static final String MIN_DIST_PARAM             = "min_dist";
    private static final String LSH_K_PARAM               = "lsh_k";
    private static final String LSH_R_PARAM               = "lsh_r";
    private static final String LSH_B_PARAM               = "lsh_b";
    private static final String SAVE_PARAM                = "save";
    private static final String MAX_ERR_PARAM             = "max_err";
    private static final String USE_DG_PARAM              = "use_dg_server";
    private static final String SAVE_PATH_PARAM           = "save_path";
    private static final String SAVE_APPEND_PARAM         = "save_append";
    private static final String PRINT_COUNTER_PARAM       = "print_counter";
    private static final String PRINT_COUNTER_STEP_PARAM  = "counter_step";
    private static final String NUM_THREADS_PARAM         = "threads";
    private static final String APPROVE_PARAM             = "approve";
    private static final String GEN_TYPE_PARAM            = "gen_type";
    private static final String DIST_CHECK_PARAM          = "dist_check";


    private static final int DEFAULT_NUM_THREADS          = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_LEN                  = 60;
    private static final int DEFAULT_COUNT                = 1;
    private static final int DEFAULT_COUNTER_STEP_FACTION = 100;
    private static final float DEFAULT_GC                 = 0.5f;
    private static final float DEFAULT_D_GC               = 0.1f;
    private static final float DEFAULT_MIN_DIST           = 0.4f;
    private static final float DEFAULT_MAX_ERROR          = 0.5f;

    private static final boolean DEFAULT_USE_DG           = true;
    private static final boolean DEFAULT_SAVE             = true;
    private static final boolean DEFAULT_SAVE_APPEND      = false;
    private static final boolean DEFAULT_COUNTER          = true;
    private static final boolean DEFAULT_APPROVE          = true;

    private static final String DEFAULT_SAVE_PATH         = "probes.fa";

    private static final int DEFAULT_LSH_K                = 4;
    private static final int DEFAULT_LSH_R                = 200;
    private static final int DEFAULT_LSH_B                = 20;


    private static final String GEN_TYPE_PROB             = "prob_gc";
    private static final String GEN_TYPE_SAFE             = "safe_gc";
    private static final String DEFAULT_GEN_TYPE          = GEN_TYPE_SAFE;

    private static final String DIST_CHECK_LSH            = "LSH";
    private static final String DIST_CHECK_NAIVE          = "Naive";
    private static final String DIST_CHECK_DEFAULT        = DIST_CHECK_LSH;


    private static long lastTime;
    private static long startTime;

    private static int count;
    private static int len;

    private static float targetGc;
    private static float maxGcDev;
    private static float maxErr;

    private static int k;
    private static int r;
    private static int b;

    private static float minDist;

    private static boolean useDg;

    private static boolean save;
    private static boolean saveAppend;
    private static String savePath;

    private static boolean printCounter;
    private static int counterStep;

    private static int threads;

    private static AtomicInteger counter;
    private static int seqCaption;
    private static int lastCycleCount;

    private static boolean approve;

    private static String genType;
    private static String distCheck;

    private static BufferedCsvWriter csv;
    private static WriteableFASTAFile fileFASTA;

    private static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        ArgumentParser argParser = new ArgumentParser(args);
        count = argParser.getParam(COUNT_PARAM, ArgumentConverter.INT(DEFAULT_COUNT));
        len = argParser.getParam(LEN_PARAM, ArgumentConverter.INT(DEFAULT_LEN));

        targetGc = argParser.getParam(GC_PARAM, ArgumentConverter.FLOAT(DEFAULT_GC));
        maxGcDev = argParser.getParam(MAX_GC_DEV_PARAM, ArgumentConverter.FLOAT(DEFAULT_D_GC));
        maxErr = argParser.getParam(MAX_ERR_PARAM, ArgumentConverter.FLOAT(DEFAULT_MAX_ERROR));

        k = argParser.getParam(LSH_K_PARAM, ArgumentConverter.INT(DEFAULT_LSH_K));
        r = argParser.getParam(LSH_R_PARAM, ArgumentConverter.INT(DEFAULT_LSH_R));
        b = argParser.getParam(LSH_B_PARAM, ArgumentConverter.INT(DEFAULT_LSH_B));

        minDist = argParser.getParam(MIN_DIST_PARAM, ArgumentConverter.FLOAT(DEFAULT_MIN_DIST));
        threads = argParser.getParam(NUM_THREADS_PARAM, ArgumentConverter.INT(DEFAULT_NUM_THREADS));

        useDg = argParser.getParam(USE_DG_PARAM, ArgumentConverter.BOOLEAN(DEFAULT_USE_DG));

        save = argParser.getParam(SAVE_PARAM, ArgumentConverter.BOOLEAN(DEFAULT_SAVE));
        saveAppend = argParser.getParam(SAVE_APPEND_PARAM, ArgumentConverter.BOOLEAN(DEFAULT_SAVE_APPEND));
        savePath = argParser.getParam(SAVE_PATH_PARAM, ArgumentConverter.STRING_LOWERCASE(DEFAULT_SAVE_PATH));

        printCounter = argParser.getParam(PRINT_COUNTER_PARAM, ArgumentConverter.BOOLEAN(DEFAULT_COUNTER));
        counterStep = argParser.getParam(PRINT_COUNTER_STEP_PARAM, ArgumentConverter.INT(Math.max(1, count / DEFAULT_COUNTER_STEP_FACTION)));

        approve = argParser.getParam(APPROVE_PARAM, ArgumentConverter.BOOLEAN(DEFAULT_APPROVE));
        distCheck = argParser.getParam(DIST_CHECK_PARAM, ArgumentConverter.STRING_LOWERCASE(DIST_CHECK_DEFAULT));

        genType = argParser.getParam(GEN_TYPE_PARAM, ArgumentConverter.STRING_LOWERCASE(DEFAULT_GEN_TYPE));

        printParams();
        if (approve && !approveParameters()) {
            System.out.println("-> Parameters were not approved -> program terminated.");
            return;
        }

        System.out.println("-> Initiating...\n");

        counter = new AtomicInteger();
        pool = createPool();
        SeqGenerator probesGenerator = createProbesGenerator();

        fileFASTA = save? new WriteableFASTAFile(savePath, saveAppend) : null;
        csv = new BufferedCsvWriter("PG_report.csv", false);
        csv.appendNewLine(
                "Progress(%)",
                "Cycle Time(s)",
                "Total Time(s)",
                "Current Count",
                "Probes/s",
                "ms/Probe",
                "Length",
                "Similarity Measure");
        startTime = System.currentTimeMillis();
        lastTime = startTime;
        seqCaption = 1;

        System.out.println("---> [Started] <---");
        IntStream.range(0, count + threads - 1).forEach(i -> pool.execute(() -> {
            int c = counter.incrementAndGet();
            if (c > count)
                return;
            BaseSequence seq = probesGenerator.generate();
            if (save)
                writeToFASTA(seq);

            report(c);
        }));

        pool.shutdown();
        boolean awaited = pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        if (!awaited)
            System.out.println("Should be impossible: pool still not terminated..");
        csv.close();
        if (fileFASTA != null)
            fileFASTA.close();

        System.out.println("---> [Finished] <---");
    }

    private static SeqGenerator createProbesGenerator() {
        DNARulesCollection rules = new BasicDNARules(useDg);
        LSH lsh = new LSH(k, r, b);
        SeqGenerator gen;
        if (genType.equals(GEN_TYPE_PROB))
            gen = new SeqGeneratorWithRulesProb(len, targetGc, maxErr, rules);
        else
            gen = new SeqGeneratorWithRulesSafe(len, targetGc, maxGcDev, maxErr, rules);

        if (distCheck.equals(DIST_CHECK_LSH))
            return new ProbesGeneratorLSH(gen, lsh, minDist);
        else
            return new ProbesGeneratorNaive(gen, k, minDist);
    }

    private static ExecutorService createPool() {
        if (distCheck.equals(DIST_CHECK_LSH))
            return Executors.newWorkStealingPool(threads);

        return Executors.newFixedThreadPool(threads);
    }

    private synchronized static void writeToFASTA(BaseSequence seq) {
        fileFASTA.append(seq, seqCaption++);
    }

    private synchronized static void report(int currentCount) {
        if (currentCount == count || currentCount % counterStep == 0) {
            int prevCycleCount = lastCycleCount;
            lastCycleCount = currentCount;
            float currentProgress = 100.0f * (float) currentCount / count;
            long currentTime = System.currentTimeMillis();
            long currentCycleTimeMs = currentTime - lastTime;
            lastTime = currentTime;
            float currentCycleTimeSec = currentCycleTimeMs / 1000.0f;
            float totalTimeSec = (currentTime - startTime) / 1000.0f;
            int probesFound = currentCount - prevCycleCount;
            float probesPerSec = Math.abs((float) probesFound / currentCycleTimeSec);
            float msPerProbe = Math.abs((float) currentCycleTimeMs / probesFound);
            csv.appendNewLine(
                    String.valueOf(currentProgress),
                    String.valueOf(currentCycleTimeSec),
                    String.valueOf(totalTimeSec),
                    String.valueOf(currentCount),
                    String.valueOf(probesPerSec),
                    String.valueOf(msPerProbe),
                    String.valueOf(len),
                    distCheck
            );
            if (printCounter)
                System.out.println("#" + currentCount + " / " + count + " (" + currentProgress + "%)" + " cycle time: " + currentCycleTimeSec + " seconds - total: " + totalTimeSec + " seconds");
        }
    }

    private static void printParams() {
        System.out.println("++++++++++++++++++++++++++++++++");
        System.out.println("-> Using following parameters <-");
        System.out.println("++++++++++++++++++++++++++++++++");
        System.out.println("--> " + COUNT_PARAM + ":         " + count);
        System.out.println("--> " + LEN_PARAM + ":           " + len);
        System.out.println("--> " + GC_PARAM + ":            " + targetGc);
        System.out.println("--> " + MAX_GC_DEV_PARAM + ":          " + maxGcDev);
        System.out.println("--> " + MAX_ERR_PARAM + ":       " + maxErr);
        System.out.println("--> " + NUM_THREADS_PARAM + ":       " + threads + (!distCheck.equals(DIST_CHECK_LSH)? " [fixed pool]" : " [work-stealing pool]"));
        System.out.println("--> " + GEN_TYPE_PARAM + ":      " + genType);
        System.out.println("--> " + DIST_CHECK_PARAM + ":    " + distCheck);
        System.out.println("--> " + USE_DG_PARAM + ": " + useDg);
        System.out.println("---------------------------");
        System.out.println("--> " + LSH_K_PARAM + ":         " + k);
        System.out.println("--> " + LSH_R_PARAM + ":         " + r);
        System.out.println("--> " + LSH_B_PARAM + ":         " + b);
        System.out.println("--> " + MIN_DIST_PARAM + ":      " + minDist);
        System.out.println("---------------------------");
        System.out.println("--> " + SAVE_PARAM + ":          " + save);
        System.out.println("--> " + SAVE_APPEND_PARAM + ":   " + saveAppend + ((save && !saveAppend && Files.exists(Paths.get(savePath)))? " [we found a file that will be overridden]" : ""));
        System.out.println("--> " + SAVE_PATH_PARAM + ":     " + savePath);
        System.out.println("---------------------------");
        System.out.println("--> " + PRINT_COUNTER_PARAM + ": " + printCounter);
        System.out.println("--> " + PRINT_COUNTER_STEP_PARAM + ":  " + counterStep);
        System.out.println("--> " + APPROVE_PARAM + ":       " + approve);
        System.out.println("------------------------------------------------------");
    }

    private static boolean approveParameters() {
        System.out.println("Are these parameters correct? [y/n]");
        String answer = FuncUtils.superSafeCall(() -> new Scanner(System.in).nextLine());
        return ArgumentConverter.BOOLEAN(false).convert(answer != null? answer.toLowerCase() : null);
    }
}
