package net.kaoriya.jpquery;

import java.io.File;

public class Loader {
    public static void main(String[] args) throws Exception {
        Database db = new Database();
        //db.loadTSV(new File("./data/japan.tsv"));
        db.loadTSV(new File("./data/japan_cities.tsv"));
    }
}
