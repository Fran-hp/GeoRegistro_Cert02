package com.example.georegistro;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListadoActivity extends AppCompatActivity {

    ListView lvIncidencias;
    TextView tvVacio;

    FirebaseFirestore db;
    List<Incidencia> listaIncidencias = new ArrayList<Incidencia>();
    IncidenciaAdapter adaptador;
    ProgressBar progressCarga;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.listado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        lvIncidencias = findViewById(R.id.lvIncidencias);
        tvVacio = findViewById(R.id.tvVacio);

        db = FirebaseFirestore.getInstance();

        adaptador = new IncidenciaAdapter(this, listaIncidencias);
        lvIncidencias.setAdapter(adaptador);
        progressCarga = findViewById(R.id.progressCarga);

        cargarIncidencias();
    }

    private void cargarIncidencias() {
        progressCarga.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);
        lvIncidencias.setVisibility(View.GONE);

        db.collection("incidencias")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        progressCarga.setVisibility(View.GONE);
                        listaIncidencias.clear();
                        for (QueryDocumentSnapshot documento : snapshot) {
                            Incidencia incidencia = documento.toObject(Incidencia.class);
                            listaIncidencias.add(incidencia);
                        }
                        adaptador.notifyDataSetChanged();

                        if (listaIncidencias.isEmpty()) {
                            mostrarMensaje("Aún no hay incidencias registradas");
                        } else {
                            tvVacio.setVisibility(View.GONE);
                            lvIncidencias.setVisibility(View.VISIBLE);
                        }

                        if (snapshot.getMetadata().isFromCache()) {
                            Toast.makeText(ListadoActivity.this,
                                    "Sin conexión: se muestran datos guardados en el dispositivo",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressCarga.setVisibility(View.GONE);
                        mostrarMensaje("No se pudieron cargar las incidencias. Revise su conexión.");
                        Toast.makeText(ListadoActivity.this,
                                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarMensaje(String texto) {
        tvVacio.setText(texto);
        tvVacio.setVisibility(View.VISIBLE);
        lvIncidencias.setVisibility(View.GONE);
    }
}