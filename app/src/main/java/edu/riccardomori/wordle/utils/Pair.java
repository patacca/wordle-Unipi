package edu.riccardomori.wordle.utils;

import java.io.Serializable;

/**
 * Class template that provides a way to store two heterogeneous objects as a single unit.
 * Comparing two pairs is equivalent to the lexicographic order between them
 */
public class Pair<T1 extends Comparable<? super T1>, T2 extends Comparable<? super T2>>
        implements Comparable<Pair<T1, T2>>, Serializable {
    private static final long serialVersionUID = 1;

    public T1 first;
    public T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int compareTo(Pair<T1, T2> o) {
        int ret = this.first.compareTo(o.first);
        if (ret == 0)
            return this.second.compareTo(o.second);
        return ret;
    }
}
