package com.example.georegistro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class RegistroActivity extends AppCompatActivity {

    private static final int CODIGO_PERMISO_UBICACION = 1000;

    EditText edNombre, edDescripcion;
    Spinner spCategoria;
    TextView txtviewFechayHora, txtviewLatitud, txtviewLongitud;
    ImageButton btnLocalizar;
    Button btnGuardar;
    FirebaseFirestore db;

    double latitud = 0.0;
    double longitud = 0.0;
    boolean ubicacionObtenida = false;

    LocationManager locationManager;
    Localizacion localizacion;

    String[] categorias = {"Seleccione categoría", "Seguridad", "Infraestructura", "Ambiental", "Otro"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edNombre = findViewById(R.id.edNombre);
        edDescripcion = findViewById(R.id.edDescripcion);
        spCategoria = findViewById(R.id.spCategoria);
        txtviewFechayHora = findViewById(R.id.txtviewFechayHora);
        txtviewLatitud = findViewById(R.id.txtviewLatitud);
        txtviewLongitud = findViewById(R.id.txtviewLongitud);
        btnLocalizar = findViewById(R.id.btnLocalizar);
        btnGuardar = findViewById(R.id.btnGuardar);
        db = FirebaseFirestore.getInstance();

        spCategoria.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, categorias));

        txtviewFechayHora.setText(obtenerFechaHora());

        btnLocalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tienePermisoUbicacion()) {
                    obtenerUbicacion();
                } else {
                    ActivityCompat.requestPermissions(RegistroActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                            CODIGO_PERMISO_UBICACION);
                }
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtviewFechayHora.setText(obtenerFechaHora());
                if (validarFormulario()) {
                    guardarIncidencia();
                }
            }
        });
    }

    // ---------- PERMISOS ----------

    private boolean tienePermisoUbicacion() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISO_UBICACION) {
            if (tienePermisoUbicacion()) {
                obtenerUbicacion();
            } else {
                mostrarDialogoPermisoDenegado();
            }
        }
    }

    private void mostrarDialogoPermisoDenegado() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso de ubicación")
                .setMessage("Sin este permiso no se puede registrar dónde ocurrió la incidencia. "
                        + "Puede activarlo en los ajustes de la aplicación.")
                .setPositiveButton("Abrir ajustes", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- GPS ----------

    @SuppressLint("MissingPermission")
    private void obtenerUbicacion() {
        if (!tienePermisoUbicacion()) {
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean redActiva = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsActivo && !redActiva) {
            Toast.makeText(this, "Active la ubicación del dispositivo", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        detenerUbicacion();
        localizacion = new Localizacion();

        boolean permisoExacto = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean escuchando = false;

        if (gpsActivo && permisoExacto) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, localizacion);
            escuchando = true;
        }
        if (redActiva) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, localizacion);
            escuchando = true;
        }

        if (escuchando) {
            txtviewLatitud.setText("Buscando...");
            txtviewLongitud.setText("Buscando...");
        } else {
            Toast.makeText(this, "No hay un proveedor de ubicación disponible con el permiso concedido",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void detenerUbicacion() {
        if (locationManager != null && localizacion != null) {
            locationManager.removeUpdates(localizacion);
        }
    }

    public class Localizacion implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location loc) {
            latitud = loc.getLatitude();
            longitud = loc.getLongitude();
            ubicacionObtenida = true;
            txtviewLatitud.setText(String.format(Locale.US, "%.6f", latitud));
            txtviewLongitud.setText(String.format(Locale.US, "%.6f", longitud));
            detenerUbicacion();
            Toast.makeText(RegistroActivity.this, "Ubicación obtenida", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerUbicacion();
        if (!ubicacionObtenida) {
            txtviewLatitud.setText("0.0");
            txtviewLongitud.setText("0.0");
        }
    }

    // ---------- FECHA Y VALIDACIÓN ----------

    private String obtenerFechaHora() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return formato.format(new Date());
    }

    private void guardarIncidencia() {
        if (!hayInternet()) {
            Toast.makeText(this, "Sin conexión a internet. No se pudo guardar.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Incidencia incidencia = new Incidencia(
                edNombre.getText().toString().trim(),
                edDescripcion.getText().toString().trim(),
                spCategoria.getSelectedItem().toString(),
                txtviewFechayHora.getText().toString(),
                latitud,
                longitud,
                System.currentTimeMillis());

        btnGuardar.setEnabled(false);

        db.collection("incidencias")
                .add(incidencia)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        btnGuardar.setEnabled(true);
                        Toast.makeText(RegistroActivity.this, "Incidencia guardada",
                                Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnGuardar.setEnabled(true);
                        Toast.makeText(RegistroActivity.this,
                                "Error al guardar: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network red = cm.getActiveNetwork();
        if (red == null) {
            return false;
        }
        NetworkCapabilities capacidades = cm.getNetworkCapabilities(red);
        return capacidades != null
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void limpiarFormulario() {
        edNombre.setText("");
        edDescripcion.setText("");
        edNombre.setError(null);
        edDescripcion.setError(null);
        spCategoria.setSelection(0);
        latitud = 0.0;
        longitud = 0.0;
        ubicacionObtenida = false;
        txtviewLatitud.setText("0.0");
        txtviewLongitud.setText("0.0");
        txtviewFechayHora.setText(obtenerFechaHora());
        edNombre.requestFocus();
    }

    private boolean validarFormulario() {
        boolean valido = true;

        String titulo = edNombre.getText().toString().trim();
        String descripcion = edDescripcion.getText().toString().trim();

        if (titulo.isEmpty()) {
            edNombre.setError("El título es obligatorio");
            edNombre.requestFocus();
            valido = false;
        } else if (titulo.length() < 3) {
            edNombre.setError("Debe tener al menos 3 caracteres");
            edNombre.requestFocus();
            valido = false;
        }

        if (descripcion.isEmpty()) {
            edDescripcion.setError("La descripción es obligatoria");
            if (valido) edDescripcion.requestFocus();
            valido = false;
        } else if (descripcion.length() < 10) {
            edDescripcion.setError("Debe tener al menos 10 caracteres");
            if (valido) edDescripcion.requestFocus();
            valido = false;
        }

        if (spCategoria.getSelectedItemPosition() == 0) {
            TextView itemSeleccionado = (TextView) spCategoria.getSelectedView();
            if (itemSeleccionado != null) {
                itemSeleccionado.setError("Seleccione una categoría");
            }
            valido = false;
        }

        if (!ubicacionObtenida) {
            Toast.makeText(this, "Debe obtener la ubicación antes de guardar",
                    Toast.LENGTH_LONG).show();
            valido = false;
        }

        return valido;
    }
}