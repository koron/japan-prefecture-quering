package net.kaoriya.jpquery.cmd;

import java.io.File;

import net.kaoriya.jpquery.Database;

public class Covering {
    public static void main(String[] args) throws Exception {
        File f1 = new File("./data/japan_cities.tsv");
        File f2 = new File("./data/japan_cities-covercells.tsv");
        Database db = new Database();
        db.loadTSV(f1);
        db.writeCoverCells(f2);
    }
}
