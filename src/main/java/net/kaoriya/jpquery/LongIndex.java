package net.kaoriya.jpquery;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.google.common.geometry.S2CellId;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

public class LongIndex implements Sortable {

    static class Group {
        int index;
        int size;

        Group(int index, int size) {
            this.index = index;
            this.size = size;
        }
    }

    public static class Loader {

        ArrayList<Long> values = new ArrayList<>();
        ArrayList<Group> groups = new ArrayList<>();

        public void load(File file) throws IOException {
            load(file.toPath());
        }

        public void load(Path path) throws IOException {
            TsvParserSettings s = new TsvParserSettings();
            TsvParser p = new TsvParser(s);
            try (
                    BufferedReader br = Files.newBufferedReader(path,
                        Charset.forName("UTF-8"))
            ) {
                int num = 0;
                p.beginParsing(br);
                for (Record rec; (rec = p.parseNextRecord()) != null;) {
                    append(rec, num);
                    ++num;
                }
            }
        }

        void append(Record rec, int index) {
            String[] ss = rec.getValues();
            groups.add(new Group(index, ss.length));
            for (String s : ss) {
                values.add(Long.parseLong(s));
            }
        }

        public LongIndex toLongIndex() {
            LongIndex idx = new LongIndex(values.size());
            int gc = 0, gi = 0;
            for (int i = 0, j = 0; i < values.size(); ++i) {
                while (gc <= 0) {
                    Group g = groups.get(j++);
                    gi = g.index;
                    gc = g.size;
                }
                idx.values[i] = values.get(i);
                idx.indexes[i] = gi;
                --gc;
            }
            return idx;
        }
    }

    public final long[] values;
    public final int[] indexes;

    public LongIndex(int size) {
        values = new long[size];
        indexes = new int[size];
    }

    public static LongIndex loadTSV(File file) throws IOException {
        Loader l = new Loader();
        l.load(file);
        return l.toLongIndex();
    }

    public int compare(int a, int b) {
        if (a == b) {
            return 0;
        }
        long va = values[a];
        long vb = values[b];
        if (va < vb) {
            return -1;
        } else if (va > vb) {
            return 1;
        } else {
            return 0;
        }
    }

    public void swap(int a, int b) {
        if (a == b) {
            return;
        }
        long v = values[a];
        values[a] = values[b];
        values[b] = v;
        int x = indexes[a];
        indexes[a] = indexes[b];
        indexes[b] = x;
    }

    public void sort() {
        Sort.sort(this, 0, values.length);
    }

    public void levelCheck() {
        int[] levels = new int[31];
        for (int i = 0; i < values.length; ++i) {
            S2CellId cid = new S2CellId(values[i]);
            levels[cid.level()]++;
        }
        for (int i = 0; i < levels.length; ++i) {
            System.out.printf("%d: %d/%d (%.5f)\n", i, levels[i],
                    values.length, (double)levels[i] / values.length);
        }
    }
}
