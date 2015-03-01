package com.example.ishida.handover;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.Map;

public class MapsActivity extends FragmentActivity implements HandOverCallback, GoogleMap.OnCameraChangeListener, GoogleMap.OnMapLoadedCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private HandOver ho;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        ho = HandOver.getHandOver(this);
        ho.registerCallback(this);

        String action = getIntent().getAction();
        if (action.equals("com.example.ishida.handover.RECOVER")) {
            ho.restore();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ho.unbind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        mMap.setOnCameraChangeListener(this);
        //mMap.setOnMapLoadedCallback(this);
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        // TODO Auto-generated method stub
        // カメラ位置変更時の処理を実装
        //Projection proj = mMap.getProjection();
        //VisibleRegion vRegion = proj.getVisibleRegion();
        // 北東 = top/right, 南西 = bottom/left
        //double topLatitude = vRegion.latLngBounds.northeast.latitude;
        //double bottomLatitude = vRegion.latLngBounds.southwest.latitude;
        //double leftLongitude = vRegion.latLngBounds.southwest.longitude;
        //double rightLongitude = vRegion.latLngBounds.northeast.longitude;
        //Toast.makeText(this, "地図表示範囲\n緯度:" + bottomLatitude + "～" + topLatitude +
        //        "\n経度:" + leftLongitude + "～" + rightLongitude , Toast.LENGTH_LONG).show();
        ho.activityChanged();
    }

    @Override
    public void onMapLoaded() {
        Toast.makeText(this, "地図の描画完", Toast.LENGTH_LONG).show();
    }

    @Override
    public void saveActivity(Map<String, Object> dictionary) {
        CameraPosition cameraPos = mMap.getCameraPosition();
        float zoom = cameraPos.zoom;
        double longitude = cameraPos.target.longitude;
        double latitude = cameraPos.target.latitude;

        dictionary.put("zoom", zoom);
        dictionary.put("longitude", longitude);
        dictionary.put("latitude", latitude);
    }

    @Override
    public void restoreActivity(Map<String, Object> dictionary) {
        float zoom = (float)dictionary.get("zoom");
        float f = (float)dictionary.get("longitude");
        double longitude = f;
        f = (float)dictionary.get("latitude");
        double latitude = f;

        CameraPosition cameraPos = new CameraPosition.Builder()
        .target(new LatLng(latitude, longitude)).zoom(zoom)
        .bearing(0).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
    }

}
