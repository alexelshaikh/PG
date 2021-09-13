package core;

import utils.FuncUtils;

public enum Base {
    A,
    T,
    C,
    G;

    public Base complement() {
        return switch (this) {
            case A  -> T;
            case T  -> A;
            case C  -> G;
            default -> C;
        };
    }

    public static Base valueOfChar(char c) {
        return switch (c) {
            case 'A' -> A;
            case 'T' -> T;
            case 'G' -> G;
            case 'C' -> C;
            default  -> throw new RuntimeException("char not a valid base: " + c);
        };
    }

    public char ordinalAsChar() {
        return Character.forDigit(this.ordinal(), 4);
    }

    public static Base random() {
        double rand = Math.random();
        if (rand <= 0.25)
            return Base.A;
        if (rand <= 0.5)
            return Base.T;
        if (rand <= 0.75)
            return Base.C;

        return Base.G;
    }

    public static Base randomGC(double gcContent) {
        double rand = Math.random();
        double gs = gcContent / 2d;
        double as = gcContent + 0.5d - gs;
        if (rand <= gs)
            return Base.G;
        if (rand <= gcContent)
            return Base.C;
        if (rand <= as)
            return Base.A;

        return Base.T;
    }

    public static Base random(Base... bs) {
        return FuncUtils.random(bs);
    }
}
