package com.ashencostha.mqtt;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; // Es importante usar AppCompatActivity

import org.json.JSONException;
import org.json.JSONObject;

// 1. Usa AppCompatActivity para mejor compatibilidad
// 2. Implementa la interfaz que creamos en el Adapter
public class MainActivity extends AppCompatActivity implements MatrixAdapter.OnCellEditListener {

    // --- Matriz y sus dimensiones ---
    private static final int ROWS = 16;
    private static final int COLS = 4;
    private int[][] matrixVals = new int[ROWS][COLS];
    // ---------------------------------

    // --- Componentes de la UI ---
    private GridView matrixGridView;
    private MatrixAdapter matrixAdapter;
    private TextView txtJson;
    private TextView txtStatus;
    // --- Botones Menú Principal (Idle) ---
    private Button cmdEditar;
    private Button cmdReproducir;
    private LinearLayout menuPrincipalLayout;
    // --- Botones Menú Edición ---
    private Button cmdPlayCell;
    private Button cmdSave;
    private Button cmdBackToMenu;
    private LinearLayout menuEdicionLayout;
    // ----------------------------

    // --- Gestión de Estado y Selección ---
    private enum AppState { IDLE, EDITING }
    private AppState currentState = AppState.IDLE;
    private int selectedRow = -1;
    private int selectedCol = -1;
    // ------------------------------------

    // --- MQTT y BroadcastReceiver ---
    private MqttHandler mqttHandler;
    public IntentFilter filterReceive;
    public IntentFilter filterConnectionLost;
    private final ReceptorOperacion receiver = new ReceptorOperacion();
    private final ConnectionLost connectionLost = new ConnectionLost();
    // --------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Vinculación de Vistas ---
        txtJson = findViewById(R.id.txtJson);
        txtStatus = findViewById(R.id.txtStatus);
        matrixGridView = findViewById(R.id.matrixGridView);

        // Vistas del menú principal
        cmdEditar = findViewById(R.id.cmdEditar);
        cmdReproducir = findViewById(R.id.cmdReproducir);
        menuPrincipalLayout = findViewById(R.id.menuPrincipalLayout); // Asume que tienes un LinearLayout con este ID

        // Vistas del menú de edición
        cmdPlayCell = findViewById(R.id.cmdPlayCell); // Asume que tienes este botón en tu layout
        cmdSave = findViewById(R.id.cmdSave);         // Asume que tienes este botón en tu layout
        cmdBackToMenu = findViewById(R.id.cmdBackToMenu); // Asume que tienes este botón en tu layout
        menuEdicionLayout = findViewById(R.id.menuEdicionLayout); // Asume que tienes un LinearLayout con este ID
        // ------------------------------

        // --- Configuración de Listeners ---
        cmdEditar.setOnClickListener(botonesListeners);
        cmdReproducir.setOnClickListener(botonesListeners);
        cmdPlayCell.setOnClickListener(botonesListeners);
        cmdSave.setOnClickListener(botonesListeners);
        cmdBackToMenu.setOnClickListener(botonesListeners);
        // ----------------------------------

        // --- Lógica de la Matriz ---
        initializeMatrix(); // Llena la matriz con ceros
        // Se crea el adapter, pasando 'this' como el listener para que la actividad reciba los eventos
        matrixAdapter = new MatrixAdapter(this, matrixVals, this);
        matrixGridView.setAdapter(matrixAdapter);
        // ---------------------------

        // --- Estado Inicial de la UI ---
        updateUIVisibility();
        // ---------------------------

        // --- Conexión MQTT y Configuración de Receivers ---
        mqttHandler = new MqttHandler(getApplicationContext());
        configurarBroadcastReceiver();
        connect();
        // -------------------------------------------------
    }

    /**
     * Actualiza la visibilidad de los menús según el estado actual de la app.
     */
    private void updateUIVisibility() {
        if (currentState == AppState.IDLE) {
            menuPrincipalLayout.setVisibility(View.VISIBLE);
            menuEdicionLayout.setVisibility(View.GONE);
            matrixAdapter.setSelection(-1, -1); // Deseleccionar celda
            selectedRow = -1;
            selectedCol = -1;
        } else { // EDITING
            menuPrincipalLayout.setVisibility(View.GONE);
            menuEdicionLayout.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Este método es llamado por el Adapter cada vez que una celda es presionada.
     * En modo IDLE, selecciona la celda.
     * En modo EDITING, permite cambiar su valor.
     * @param row Fila de la celda modificada.
     * @param col Columna de la celda modificada.
     * @param value El nuevo valor numérico de la celda (si se cambió).
     */
    @Override
    public void onCellEdited(int row, int col, int value) {
        if (currentState == AppState.IDLE) {
            // En modo IDLE, solo seleccionamos la celda
            selectedRow = row;
            selectedCol = col;
            matrixAdapter.setSelection(row, col); // Resaltar visualmente la celda
            Toast.makeText(this, "Celda (" + row + ", " + col + ") seleccionada. Presiona EDITAR.", Toast.LENGTH_SHORT).show();
        } else { // EDITING
            // En modo EDITING, el valor ya se actualizó en la matriz localmente por el Adapter
            // No publicamos aquí, esperamos al botón "Guardar"
            Toast.makeText(this, "Valor de la celda cambiado a: " + value, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Rellena la matriz con valores iniciales (en este caso, 0).
     */
    private void initializeMatrix() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                matrixVals[i][j] = 0;
            }
        }
    }

    private void connect() {
        ConfigMQTT.useServerUBIDOTS();
        mqttHandler.connect(ConfigMQTT.mqttServer, ConfigMQTT.CLIENT_ID, ConfigMQTT.userName, ConfigMQTT.userPass);

        // Damos un tiempo prudencial para que se establezca la conexión antes de suscribir
        try {
            Thread.sleep(1000);
            subscribeToTopic(ConfigMQTT.topicStatus);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void configurarBroadcastReceiver() {
        filterReceive = new IntentFilter(MqttHandler.ACTION_DATA_RECEIVE);
        filterConnectionLost = new IntentFilter(MqttHandler.ACTION_CONNECTION_LOST);

        filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterConnectionLost.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(receiver, filterReceive);
        registerReceiver(connectionLost, filterConnectionLost);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHandler != null) {
            mqttHandler.disconnect();
        }
        unregisterReceiver(receiver);
        unregisterReceiver(connectionLost);
    }

    private void publishMessage(String topic, String message) {
        Toast.makeText(this, "Publicando: " + message, Toast.LENGTH_SHORT).show();
        if (mqttHandler != null) {
            mqttHandler.publish(topic, message);
        }
    }

    private void subscribeToTopic(String topic) {
        Toast.makeText(this, "Suscribiendo a " + topic, Toast.LENGTH_SHORT).show();
        if (mqttHandler != null) {
            mqttHandler.subscribe(topic);
        }
    }

    private final View.OnClickListener botonesListeners = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // --- Lógica del Menú Principal ---
            if (view.getId() == R.id.cmdEditar) {
                if (selectedRow != -1 && selectedCol != -1) {
                    currentState = AppState.EDITING;
                    matrixAdapter.setEditing(true); // Habilita la edición en el adapter
                    updateUIVisibility();
                    publishMessage(ConfigMQTT.topicState, "Edit");
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, seleccione una celda primero", Toast.LENGTH_SHORT).show();
                }
            } else if (view.getId() == R.id.cmdReproducir) {
                publishMessage(ConfigMQTT.topicState, "PlayAll");
            }

            // --- Lógica del Menú de Edición ---
            else if (view.getId() == R.id.cmdPlayCell) {
                if (selectedRow != -1) {
                    publishMessage(ConfigMQTT.topicState, "PlayCell");
                }
            } else if (view.getId() == R.id.cmdSave) {
                if (selectedRow != -1 && selectedCol != -1) {
                    int valueToSave = matrixVals[selectedRow][selectedCol];
                    JSONObject payload = new JSONObject();
                    try {
                        payload.put("row", selectedRow);
                        payload.put("col", selectedCol);
                        payload.put("value", valueToSave);
                        // Publicamos el mensaje en el tópico dedicado a la matriz
                        publishMessage(ConfigMQTT.topicEdit, payload.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error al crear JSON para guardar", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (view.getId() == R.id.cmdBackToMenu) {
                currentState = AppState.IDLE;
                matrixAdapter.setEditing(false); // Deshabilita la edición en el adapter
                updateUIVisibility();
                publishMessage(ConfigMQTT.topicState, "Idle");
            }
        }
    };

    // --- Clases Internas para Broadcasts ---

    public class ConnectionLost extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), "Conexión Perdida. Reconectando...", Toast.LENGTH_SHORT).show();
            connect();
        }
    }

    public class ReceptorOperacion extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getExtras() != null) {
                String topic = intent.getStringExtra("topic");
                String msgJson = intent.getStringExtra("msgJson");

                txtJson.setText(String.format("Tópico: %s, Mensaje: %s", topic, msgJson));

                try {
                    // Aquí procesas los mensajes que llegan
                    if (topic != null && topic.equals(ConfigMQTT.topicStatus)) {
                        JSONObject jsonObject = new JSONObject(msgJson);
                        String value = jsonObject.getString("value");
                        txtStatus.setText(String.format("%s°", value));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
