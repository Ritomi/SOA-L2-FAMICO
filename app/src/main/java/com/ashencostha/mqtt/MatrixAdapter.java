package com.ashencostha.mqtt;

import android.content.Context;
import android.text.Editable;import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;

public class MatrixAdapter extends BaseAdapter {

    /**
     * Interfaz de comunicación. Define un contrato que MainActivity debe cumplir.
     * Permite que el Adapter notifique a la Activity sin conocerla directamente (buena práctica).
     */
    public interface OnCellEditListener {
        void onCellEdited(int row, int col, int value);
    }

    private final Context context;
    private final int[][] matrix;
    private final OnCellEditListener listener; // Referencia a la clase que implementa la interfaz (MainActivity)
    private final int ROWS;
    private final int COLS;

    public MatrixAdapter(Context context, int[][] matrix, OnCellEditListener listener) {
        this.context = context;
        this.matrix = matrix;
        this.listener = listener; // Guardamos la referencia a MainActivity
        this.ROWS = matrix.length;
        this.COLS = matrix[0].length;
    }

    @Override
    public int getCount() {
        return ROWS * COLS; // El número total de celdas
    }

    @Override
    public Object getItem(int position) {
        int row = position / COLS;
        int col = position % COLS;
        return matrix[row][col];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final EditText editText;

        // Patrón ViewHolder para reutilizar vistas y mejorar el rendimiento
        if (convertView == null) {
            // Si la vista no existe, la "inflamos" desde nuestro layout grid_item.xml
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
            editText = convertView.findViewById(R.id.grid_item_value);
            convertView.setTag(new ViewHolder(editText));
        } else {
            // Si la vista ya existe, la reutilizamos
            ViewHolder holder = (ViewHolder) convertView.getTag();
            editText = holder.editText;
        }

        // Obtenemos el TextWatcher antiguo para removerlo y evitar llamadas múltiples
        TextWatcher oldWatcher = (TextWatcher) editText.getTag(R.id.grid_item_value);
        if (oldWatcher != null) {
            editText.removeTextChangedListener(oldWatcher);
        }

        // Calculamos la fila y columna a partir de la posición lineal
        final int row = position / COLS;
        final int col = position % COLS;

        // Establecemos el valor actual de la celda en el EditText
        editText.setText(String.valueOf(matrix[row][col]));

        // Creamos un nuevo listener para detectar cambios en el texto
        TextWatcher newWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    // Cuando el texto cambia, intentamos convertirlo a un número
                    int newValue = Integer.parseInt(s.toString());

                    // Comprobamos si el valor realmente cambió para evitar publicaciones innecesarias
                    if (matrix[row][col] != newValue) {
                        matrix[row][col] = newValue; // Actualizamos el valor en nuestro array de datos

                        // Usamos la interfaz para notificar a MainActivity sobre el cambio
                        if (listener != null) {
                            listener.onCellEdited(row, col, newValue);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Si el usuario borra el texto o introduce algo inválido, no hacemos nada o asignamos 0
                }
            }
        };

        editText.addTextChangedListener(newWatcher);
        // Guardamos el nuevo watcher en un tag para poder quitarlo la próxima vez que se reutilice la vista
        editText.setTag(R.id.grid_item_value, newWatcher);

        return convertView;
    }

    // Clase ViewHolder para almacenar las vistas y mejorar el rendimiento de la GridView
    private static class ViewHolder {
        final EditText editText;

        ViewHolder(EditText editText) {
            this.editText = editText;
        }
    }
}
