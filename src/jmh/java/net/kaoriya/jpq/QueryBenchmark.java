package net.kaoriya.jpq;

import java.util.Random;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class QueryBenchmark {

    @State(Scope.Benchmark)
    public static class Aerospike {
        public AerospikeClient client;
        public long hitCount;
        public long missCount;

        public Statement outConstStatement = newQueryStatement(0, 0);
        public Statement constStatement =
            newQueryStatement(139.780935, 35.702265);
        public Random r = new Random();

        @Setup(Level.Trial)
        public void setup() {
            client = new AerospikeClient("127.0.0.1", 3000);
            hitCount = 0;
            missCount = 0;
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            client.close();
            client = null;
            System.out.println();
            System.out.println(String.format(
                        "hit:%d miss:%d rate:%.3f", hitCount, missCount,
                        (double)hitCount / (hitCount + missCount)));
        }

        public Statement randomAllJapan() {
            double lng = 130.0 + r.nextDouble() * 12.0;
            double lat = 32.0 + r.nextDouble() * 9.0;
            return newQueryStatement(lng, lat);
        }

        public Statement randomKanto() {
            double lng = 138.6 + r.nextDouble() * 2.0;
            double lat = 35.2 + r.nextDouble() * 1.6;
            return newQueryStatement(lng, lat);
        }

        public Statement randomOut() {
            double lng = -1.0 + r.nextDouble() * 2.0;
            double lat = -1.0 + r.nextDouble() * 2.0;
            return newQueryStatement(lng, lat);
        }
    }

    /**
     * Query constant point.
     *
     * hit-rate is 100%
     */
    //@Benchmark
    public void queryConstant(Aerospike as) {
        Statement s = as.constStatement;
        try (RecordSet rs = as.client.query(null, s)) {
            if (rs.next()) {
                as.hitCount++;
            } else {
                as.missCount++;
            }
        }
    }

    /**
     * Query points on all Japan.
     *
     * hit-rate is about 25%
     */
    //@Benchmark
    public void queryRandomAllJapan(Aerospike as) {
        Statement s = as.randomAllJapan();
        try (RecordSet rs = as.client.query(null, s)) {
            if (rs.next()) {
                as.hitCount++;
            } else {
                as.missCount++;
            }
        }
    }

    /**
     * Query points on Kanto.
     *
     * hit-rate is about 92%
     */
    //@Benchmark
    public void queryRandomKanto(Aerospike as) {
        Statement s = as.randomKanto();
        try (RecordSet rs = as.client.query(null, s)) {
            if (rs.next()) {
                as.hitCount++;
            } else {
                as.missCount++;
            }
        }
    }

    /**
     * Query constant outer point.
     *
     * hit-rate is 0%
     */
    //@Benchmark
    public void queryOutConstant(Aerospike as) {
        Statement s = as.outConstStatement;
        try (RecordSet rs = as.client.query(null, s)) {
            if (rs.next()) {
                as.hitCount++;
            } else {
                as.missCount++;
            }
        }
    }

    /**
     * Query points on Kanto.
     *
     * hit-rate is about 0%
     */
    //@Benchmark
    public void queryOutRandom(Aerospike as) {
        Statement s = as.randomOut();
        try (RecordSet rs = as.client.query(null, s)) {
            if (rs.next()) {
                as.hitCount++;
            } else {
                as.missCount++;
            }
        }
    }

    static String toGeoJSONPoint(double lng, double lat) {
        StringBuilder b =
            new StringBuilder("{\"type\":\"Point\",\"coordinates\":[")
            .append(lng).append(',').append(lat)
            .append("]}");
        return b.toString();
    }

    static Statement newQueryStatement(double lng, double lat) {
        Statement s = new Statement();
        s.setNamespace("test");
        s.setSetName("demo");
        s.setBinNames("pref", "boff", "oc", "city", "code");
        Filter f = Filter.geoContains("gj", toGeoJSONPoint(lng, lat));
        s.setFilters(f);
        return s;
    }

}
