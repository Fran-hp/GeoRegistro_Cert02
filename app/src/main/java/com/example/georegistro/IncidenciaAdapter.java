package com.example.georegistro;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class IncidenciaAdapter extends ArrayAdapter<Incidencia> {

    public IncidenciaAdapter(@NonNull Context context, @NonNull List<Incidencia> lista) {
        super(context, 0, lista);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        Incidencia incidencia = getItem(position);

        TextView texto1 = convertView.findViewById(android.R.id.text1);
        TextView texto2 = convertView.findViewById(android.R.id.text2);

        if (incidencia != null) {
            texto1.setText(incidencia.getTitulo());
            texto1.setTextColor(Color.parseColor("#1F2D22"));
            texto1.setTextSize(18);

            texto2.setText(incidencia.getCategoria() + " · " + incidencia.getFechaHora());
            texto2.setTextColor(Color.parseColor("#5E6F62"));
        }

        return convertView;
    }
}