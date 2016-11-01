package net.kaoriya.jpquery.cmd;

import java.io.File;

import net.kaoriya.jpquery.LongIndex;

public class LoadLongIndex {
    public static void main(String[] args) throws Exception {
        File f = new File("./data/japan_cities-covercells.tsv");
        LongIndex idx = LongIndex.loadTSV(f);
        idx.sort();
    }
}
