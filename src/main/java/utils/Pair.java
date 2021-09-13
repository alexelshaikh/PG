package utils;

import java.io.Serializable;
import java.util.Objects;

public class Pair<T1, T2> implements Serializable, Cloneable {

    protected T1 t1;
    protected T2 t2;

    public Pair(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getT1() {
        return t1;
    }

    public T2 getT2() {
        return t2;
    }

    public void setT1(T1 t1) {
        this.t1 = t1;
    }

    public void setT2(T2 t2) {
        this.t2 = t2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(t1, pair.t1) &&
                Objects.equals(t2, pair.t2);
    }
    @Override
    public int hashCode() {
        return Objects.hash(t1, t2);
    }

    public String toString() {
        return "{" + t1 + ", " + t2 + "}";
    }

    @Override
    public Pair<T1, T2> clone() {
        return new Pair<>(t1, t2);
    }
}
