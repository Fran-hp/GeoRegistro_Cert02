package com.example.georegistro;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;

// SENSOR: ahora la clase también implementa SensorEventListener
public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap mMap;
    FirebaseFirestore db;

    // SENSOR: variables nuevas
    TextView tvLuz;
    SensorManager sensorManager;
    Sensor sensorLuz;
    boolean modoNoche = false;
    static final float LUX_OSCURO = 20f;
    static final float LUX_CLARO = 40f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.mapa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // SENSOR: preparar el sensor de luz
        tvLuz = findViewById(R.id.tvLuz);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sensorLuz == null) {
            tvLuz.setText("Sensor de luz no disponible");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // SENSOR: empezar a escuchar cuando la pantalla es visible
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorLuz != null) {
            sensorManager.registerListener(this, sensorLuz, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // SENSOR: dejar de escuchar cuando la pantalla se oculta
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorLuz != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // SENSOR: llega cada vez que cambia la luz
    @Override
    public void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];

        boolean oscuro;
        if (lux < LUX_OSCURO) {
            oscuro = true;
        } else if (lux > LUX_CLARO) {
            oscuro = false;
        } else {
            oscuro = modoNoche;
        }

        tvLuz.setText(String.format(Locale.US, "Luz: %.0f lux · %s",
                lux, oscuro ? "Modo noche" : "Modo día"));

        if (oscuro != modoNoche) {
            modoNoche = oscuro;
            aplicarEstiloMapa();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // SENSOR: cambia el estilo del mapa según el modo
    private void aplicarEstiloMapa() {
        if (mMap == null) {
            return;
        }
        if (modoNoche) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapa_noche));
        } else {
            mMap.setMapStyle(null);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMinZoomPreference(4.0F);
        mMap.setMaxZoomPreference(19.0F);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-35.67, -71.54), 4.0F));

        aplicarEstiloMapa(); // SENSOR: por si la luz llegó antes que el mapa
        cargarMarcadores();
    }

    private void cargarMarcadores() {
        db.collection("incidencias")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        LatLngBounds.Builder limites = new LatLngBounds.Builder();
                        int cantidad = 0;
                        LatLng ultimaPosicion = null;

                        for (QueryDocumentSnapshot documento : snapshot) {
                            Incidencia incidencia = documento.toObject(Incidencia.class);

                            if (incidencia.getLatitud() == 0.0 && incidencia.getLongitud() == 0.0) {
                                continue;
                            }

                            LatLng posicion = new LatLng(incidencia.getLatitud(), incidencia.getLongitud());
                            mMap.addMarker(new MarkerOptions()
                                    .position(posicion)
                                    .title(incidencia.getTitulo())
                                    .snippet(incidencia.getCategoria() + " · " + incidencia.getFechaHora()));

                            limites.include(posicion);
                            ultimaPosicion = posicion;
                            cantidad++;
                        }

                        if (cantidad == 0) {
                            Toast.makeText(MapaActivity.this,
                                    "No hay incidencias con ubicación para mostrar",
                                    Toast.LENGTH_LONG).show();
                        } else if (cantidad == 1) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ultimaPosicion, 16.0F));
                        } else {
                            int ancho = getResources().getDisplayMetrics().widthPixels;
                            int alto = getResources().getDisplayMetrics().heightPixels;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                                    limites.build(), ancho, alto, 200));
                        }

                        if (snapshot.getMetadata().isFromCache()) {
                            Toast.makeText(MapaActivity.this,
                                    "Sin conexión: se muestran datos guardados en el dispositivo",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapaActivity.this,
                                "No se pudieron cargar las incidencias: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}