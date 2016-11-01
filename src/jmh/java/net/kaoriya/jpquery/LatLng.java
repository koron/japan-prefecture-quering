package net.kaoriya.jpquery;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;

public class LatLng {
    public double lat;
    public double lng;

    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public S2CellId toS2CellId() {
        return S2CellId.fromLatLng(S2LatLng.fromDegrees(lat, lng));
    }
}
