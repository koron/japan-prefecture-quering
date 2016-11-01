package net.kaoriya.jpquery.cmd;

import java.io.File;

import net.kaoriya.jpquery.LongIndex;
import net.kaoriya.jpquery.IntervalTreeBuilder;

public class LoadLongIndex {
    public static void main(String[] args) throws Exception {
        File f = new File("./data/japan_cities-covercells.tsv");
        LongIndex idx = LongIndex.loadTSV(f);
        idx.sort();
        //idx.levelCheck();
        long st = System.currentTimeMillis();
        IntervalTreeBuilder.build(idx);
        long du = System.currentTimeMillis() - st;
        System.out.printf("it.build() tooks %f secs\n", (double)du / 1000);
    }
}
