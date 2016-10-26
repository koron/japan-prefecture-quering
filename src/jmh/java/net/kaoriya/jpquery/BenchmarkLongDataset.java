package net.kaoriya.jpquery;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class BenchmarkLongDataset {

    @State(Scope.Benchmark)
    public static class Dataset {
        public Random r;
        public Database db;

        public long hitCount;
        public long missCount;

        public final LatLng zeroLatLng = new LatLng(0, 0);
        public final LatLng constTokyo = new LatLng(35.702265, 139.780935);

        @Setup(Level.Trial)
        public void setup() {
            r = new Random();
            db = new Database();
            try {
                db.loadTSV(new File("./data/japan_cities.tsv"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            hitCount = 0;
            missCount = 0;
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            db = null;
            System.out.println();
            System.out.println(String.format(
                        "hit:%d miss:%d rate:%.3f", hitCount, missCount,
                        (double)hitCount / (hitCount + missCount)));
        }

        public LatLng randomAllJapan() {
            double lat = 32.0 + r.nextDouble() * 9.0;
            double lng = 130.0 + r.nextDouble() * 12.0;
            return new LatLng(lat, lng);
        }

        public LatLng randomKanto() {
            double lat = 35.2 + r.nextDouble() * 1.6;
            double lng = 138.6 + r.nextDouble() * 2.0;
            return new LatLng(lat, lng);
        }

        public LatLng randomOut() {
            double lat = -1.0 + r.nextDouble() * 2.0;
            double lng = -1.0 + r.nextDouble() * 2.0;
            return new LatLng(lat, lng);
        }

        public List<Region> query(LatLng p) {
            return db.find(p.lat, p.lng);
        }
    }

    void checkList(Dataset ds, List<Region> regions) {
        if (regions == null || regions.size() < 1) {
            ds.missCount++;
        } else {
            ds.hitCount++;
        }
    }

    /**
     * Query constant point in tokyo.
     *
     * hit-rate should be 100%
     */
    @Benchmark
    public void queryConstantTokyo(Dataset ds) {
        checkList(ds, ds.query(ds.constTokyo));
    }

    @Benchmark
    public void queryRandomAllJapan(Dataset ds) {
        checkList(ds, ds.query(ds.randomAllJapan()));
    }

    @Benchmark
    public void queryRandomKanto(Dataset ds) {
        checkList(ds, ds.query(ds.randomKanto()));
    }

    @Benchmark
    public void queryConstantZero(Dataset ds) {
        checkList(ds, ds.query(ds.zeroLatLng));
    }

    @Benchmark
    public void queryRandomOut(Dataset ds) {
        checkList(ds, ds.query(ds.randomOut()));
    }
}
