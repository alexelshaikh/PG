package utils;

public class PseudoPermutation {

    private final long m; // num rows/elements (q grams)
    private final long p; // prime > m
    private final long a; // random; 1 <= a <= p - 1
    private final long b; // random; 1 <= b <= p - 1

    public PseudoPermutation(long m) {
        this(m,  m);
    }

    public PseudoPermutation(long m, long p_1) {
        if (p_1 < m)
            throw new RuntimeException("p must be >= m");
        this.m = m;
        this.p = nextPrime(p_1);
        this.a = (long) (1 + Math.random() * p);
        this.b = (long) (1 + Math.random() * p);
    }

    public long getP() {
        return p;
    }

    public static long nextPrime(long start) {
        long p = start + 1L;
        if ((p & 1L) == 0L)
            p++;
        while (!isOddNumberAlsoPrime(p))
            p += 2L;

        return p;
    }

    public long apply(long x) {
        long y = ((a * x + b) % p) % m;
        while (y < 0L)
            y += m;
        return y;
    }

    public static boolean isOddNumberAlsoPrime(long odd) {
        long root = (long) Math.sqrt(odd) + 1L;
        for (long r = 3L; r <= root; r += 2L) {
            if (odd % r == 0L)
                return false;
        }
        return true;
    }
}
