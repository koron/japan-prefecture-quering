package net.kaoriya.jpquery;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonArray;

import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2RegionCoverer;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import com.google.common.geometry.S2PolygonBuilder;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import com.univocity.parsers.common.record.Record;

public class Database {

    public static class Region {
        public String pref;
        public String boff;
        public String oc;
        public String city;
        public String code;
        public S2CellUnion cellUnion;
    }

    S2RegionCoverer coverer = new S2RegionCoverer();
    ArrayList<Region> regions = new ArrayList<>();

    public Database() {
        coverer.setMinLevel(2);
        coverer.setMaxLevel(30);
        coverer.setMaxCells(48);
    }

    public void loadTSV(File file) throws IOException {
        loadTSV(file.toPath());
    }

    public void loadTSV(Path path) throws IOException {
        TsvParserSettings s = new TsvParserSettings();
        s.setMaxCharsPerColumn(8*1024*1024);
        TsvParser p = new TsvParser(s);
        try (
                BufferedReader br = Files.newBufferedReader(path,
                    Charset.forName("UTF-8"))
        ) {
            int num = 0;
            p.beginParsing(br);
            for (Record rec; (rec = p.parseNextRecord()) != null;) {
                Region reg = toRegion(rec);
                if (reg != null) {
                    regions.add(reg);
                }
                ++num;
                if ((num % 5000) == 0) {
                    System.out.println(num);
                }
            }
        }
    }

    Region toRegion(Record rec) {
        S2CellUnion cu = toCU(rec.getString(6));
        if (cu == null) {
            // FIXME: log warning
            return null;
        }
        Region reg = new Region();
        reg.pref = rec.getString(1);
        reg.boff = rec.getString(2);
        reg.oc = rec.getString(3);
        reg.city = rec.getString(4);
        reg.code = rec.getString(5);
        reg.cellUnion = cu;
        return reg;
    }

    S2CellUnion toCU(String coords) {
        boolean multi = false;
        if (coords.startsWith("[[[[")) {
            multi = true;
        } else if (coords.startsWith("[[[")) {
            multi = false;
        } else {
            return null;
        }
        try (JsonReader jr = Json.createReader(new StringReader(coords))) {
            JsonArray array = jr.readArray();
            return multi ? toMultiCU(array) : toCU(array);
        }
    }

    S2Point arrayToPoint(JsonArray array) {
        double longitude = array.getJsonNumber(0).doubleValue();
        double latitude = array.getJsonNumber(1).doubleValue();
        return S2LatLng.fromDegrees(latitude, longitude).toPoint();
    }

    S2Loop arrayToLoop(JsonArray array) {
        int size = array.size() - 1;
        ArrayList<S2Point> points = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            JsonArray item = array.getJsonArray(i);
            points.add(arrayToPoint(item));
        }
        return new S2Loop(points);
    }

    void appendAsPolygon(S2PolygonBuilder b, JsonArray array) {
        for (int i = 0; i < array.size(); ++i) {
            JsonArray item = array.getJsonArray(i);
            b.addLoop(arrayToLoop(item));
        }
    }

    S2CellUnion toCU(JsonArray array) {
        S2PolygonBuilder b = new S2PolygonBuilder();
        appendAsPolygon(b, array);
        return coverer.getCovering(b.assemblePolygon());
    }

    S2CellUnion toMultiCU(JsonArray array) {
        S2PolygonBuilder b = new S2PolygonBuilder();
        for (int i = 0; i < array.size(); ++i) {
            JsonArray item = array.getJsonArray(i);
            appendAsPolygon(b, item);
        }
        return coverer.getCovering(b.assemblePolygon());
    }

    public List<Region> find(double latitude, double longitude) {
        S2Point p = S2LatLng.fromDegrees(latitude, longitude).toPoint();
        return regions.parallelStream()
            .filter(r -> r.cellUnion.contains(p))
            .collect(Collectors.toList());
    }
}
