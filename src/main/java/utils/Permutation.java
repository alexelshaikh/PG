package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Permutation implements Serializable, Cloneable {

    protected PermIterator it;
    protected List<Pair<Integer, Integer>> ps;

    public Permutation(List<Pair<Integer, Integer>> perms) {
        this.it = new ForwardIterator(perms);
        this.ps = perms;
    }

    private Permutation(List<Pair<Integer, Integer>> perms, PermIterator it) {
        this.it = it;
        this.ps = perms;
    }

    public <T> List<T> apply(List<T> list) {
        return applyInPlace(new ArrayList<>(list));
    }

    public <T> List<T> applyInPlace(List<T> list) {
        it.forEachRemaining(p -> swap(list, p.getT1(), p.getT2()));
        it.resetIterator();
        return list;
    }

    public String apply(String s) {
        StringBuilder sb = new StringBuilder(s);
        it.forEachRemaining(p -> swap(sb, p.getT1(), p.getT2()));
        it.resetIterator();
        return sb.toString();
    }

    public Permutation reverseInPlace() {
        if (this.it instanceof ReverseIterator)
            this.it = new ForwardIterator(ps);
        else
            this.it = new ReverseIterator(ps);
        return this;
    }

    public Permutation reverse() {
        if (this.it instanceof ReverseIterator)
            return new Permutation(ps, new ForwardIterator(ps));
        else
            return new Permutation(ps, new ReverseIterator(ps));
    }

    private static void swap(StringBuilder sb, int i, int j) {
        char ti = sb.charAt(i);
        sb.setCharAt(i, sb.charAt(j));
        sb.setCharAt(j, ti);
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T ti = list.get(i);
        list.set(i, list.get(j));
        list.set(j, ti);
    }

    @Override
    protected Permutation clone() {
        return new Permutation(ps);
    }

    private abstract static class PermIterator implements Iterator<Pair<Integer, Integer>> {
        protected final List<Pair<Integer, Integer>> ps;
        protected ListIterator<Pair<Integer, Integer>> it;
        public PermIterator(List<Pair<Integer, Integer>> ps, ListIterator<Pair<Integer, Integer>> it) {
            this.ps = ps;
            this.it = it;
        }
        public abstract void resetIterator();
    }

    private static class ForwardIterator extends PermIterator {
        public ForwardIterator(List<Pair<Integer, Integer>> ps) {
            super(ps, ps.listIterator());
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }
        @Override
        public Pair<Integer, Integer> next() {
            return it.next();
        }
        @Override
        public void resetIterator() {
            it = ps.listIterator();
        }
    }

    private static class ReverseIterator extends PermIterator {
        public ReverseIterator(List<Pair<Integer, Integer>> ps) {
            super(ps, ps.listIterator(ps.size()));
        }

        @Override
        public boolean hasNext() {
            return it.hasPrevious();
        }
        @Override
        public Pair<Integer, Integer> next() {
            return it.previous();
        }
        @Override
        public void resetIterator() {
            it = ps.listIterator(ps.size());
        }
    }
}
