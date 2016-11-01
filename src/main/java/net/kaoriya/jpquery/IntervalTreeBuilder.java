package net.kaoriya.jpquery;

import com.google.common.geometry.S2CellId;
import intervalTree.IntervalTree;

public class IntervalTreeBuilder {

    public static IntervalTree<Integer> build(LongIndex idx) {
        IntervalTree<Integer> t = new IntervalTree<>();
        for (int i = 0; i < idx.values.length; ++i) {
            S2CellId cid = new S2CellId(idx.values[i]);
            t.addInterval(cid.rangeMin().id(), cid.rangeMax().id(),
                    idx.indexes[i]);
        }
        t.build();
        return t;
    }

}
