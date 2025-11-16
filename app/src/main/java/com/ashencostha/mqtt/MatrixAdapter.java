package com.ashencostha.mqtt;

import android.content.Context;
import android.graphics.Color; // <-- Correct Color class
import android.view.LayoutInflater;import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

public class MatrixAdapter extends BaseAdapter {

    // --- Member Variables ---
    private final Context context;
    private final int[][] matrix;
    private final OnCellEditListener listener;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean isEditing = false;
    // ------------------------

    /**
     * Listener interface to communicate events back to the Activity.
     */
    public interface OnCellEditListener {
        void onCellEdited(int row, int col, int value);
    }

    /**
     * Constructor for the adapter.
     * @param context The application context.
     * @param matrix The 2D array holding the matrix data.
     * @param listener The listener to be notified of cell events.
     */
    public MatrixAdapter(Context context, int[][] matrix, OnCellEditListener listener) {
        this.context = context;
        this.matrix = matrix;
        this.listener = listener;
    }

    // --- New methods for state management ---

    /**
     * Sets the currently selected cell to be highlighted.
     * @param row The selected row.
     * @param col The selected column.
     */
    public void setSelection(int row, int col) {
        selectedRow = row;
        selectedCol = col;
        notifyDataSetChanged(); // Redraws the grid to apply the highlight.
    }

    /**
     * Enables or disables the editing mode.
     * @param editing True to enable editing, false to disable.
     */
    public void setEditing(boolean editing) {
        isEditing = editing;
        // When we exit edit mode, clear the visual selection.
        if (!editing) {
            setSelection(-1, -1);
        }
    }
    // ----------------------------------------


    @Override
    public int getCount() {
        // The total number of cells is ROWS * COLS
        if (matrix == null || matrix.length == 0) return 0;
        return matrix.length * matrix[0].length;
    }

    @Override
    public Object getItem(int position) {
        int row = position / matrix[0].length;
        int col = position % matrix[0].length;
        return matrix[row][col];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Button button;

        if (convertView == null) {
            // If it's not recycled, inflate a new view.
            // We assume you have a layout file `grid_item.xml` with just a Button.
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_item, parent, false);
        }

        button = (Button) convertView;

        // Get row and column from the 1D position
        final int numCols = matrix[0].length;
        final int row = position / numCols;
        final int col = position % numCols;

        // Set the button's text to the value from the matrix
        button.setText(String.valueOf(matrix[row][col]));

        // --- Block for Highlighting ---
        if (row == selectedRow && col == selectedCol) {
            button.setBackgroundColor(Color.CYAN); // Highlight color for the selected cell
        } else {
            button.setBackgroundColor(Color.LTGRAY); // Default cell color
        }
        // ------------------------------

        // --- OnClickListener for the cell ---
        button.setOnClickListener(v -> {
            // If in editing mode, increment the cell's value
            if (isEditing) {
                // Increment value logic (e.g., cycles from 0 to 127)
                matrix[row][col] = (matrix[row][col] + 1) % 128;
                button.setText(String.valueOf(matrix[row][col]));
            }
            // Always notify the listener, whether for selection (in IDLE mode) or for editing.
            if (listener != null) {
                listener.onCellEdited(row, col, matrix[row][col]);
            }
        });

        return convertView;
    }
}
