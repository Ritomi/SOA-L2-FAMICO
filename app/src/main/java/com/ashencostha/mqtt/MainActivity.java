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
    private TextView txtEspStatus;
    private TextView txtPhoneState;
    // --- Botones Menú Principal (Idle) ---
    private Button cmdEditar;
    private Button cmdReproducir;
    private LinearLayout menuPrincipalLayout;
    // --- Botones Menú Edición ---
    private Button cmdPlayRow;
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
        txtEspStatus = findViewById(R.id.txtEspStatus);
        txtPhoneState = findViewById(R.id.txtPhoneState);
        matrixGridView = findViewById(R.id.matrixGridView);

        // Vistas del menú principal
        cmdEditar = findViewById(R.id.cmdEditar);
        cmdReproducir = findViewById(R.id.cmdReproducir);
        menuPrincipalLayout = findViewById(R.id.menuPrincipalLayout); // Asume que tienes un LinearLayout con este ID

        // Vistas del menú de edición
        cmdPlayRow = findViewById(R.id.cmdPlayRow); // Asume que tienes este botón en tu layout
        cmdSave = findViewById(R.id.cmdSave);         // Asume que tienes este botón en tu layout
        cmdBackToMenu = findViewById(R.id.cmdBackToMenu); // Asume que tienes este botón en tu layout
        menuEdicionLayout = findViewById(R.id.menuEdicionLayout); // Asume que tienes un LinearLayout con este ID
        // ------------------------------

        // --- Configuración de Listeners ---
        cmdEditar.setOnClickListener(botonesListeners);
        cmdReproducir.setOnClickListener(botonesListeners);
        cmdPlayRow.setOnClickListener(botonesListeners);
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
        setPhoneState(AppState.IDLE);
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
     */
    @Override
    public void onCellEdited(int row, int col, int value) {
        if (currentState == AppState.IDLE) {
            // En modo IDLE, solo seleccionamos la celda
            selectedRow = row;
            selectedCol = col;
            matrixAdapter.setSelection(row, col); // Resaltar la celda
            txtJson.setText("Celda (" + row + ", " + col + ") seleccionada. Presiona EDITAR.");
        }
    }

    /**
     * Rellena la matriz con valores iniciales
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

        // Tiempo de espera de conexion
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
        txtJson.setText("Publicando: " + message + " en " + topic);
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
            if (view.getId() == R.id.cmdEditar) {
                if (selectedRow != -1 && selectedCol != -1) {
                    setPhoneState(AppState.EDITING); // <-- USE THE METHOD
                    matrixAdapter.setEditing(true);
                    publishMessage(ConfigMQTT.topicState, "Edit");
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, seleccione una celda primero", Toast.LENGTH_SHORT).show();
                }
        } else if (view.getId() == R.id.cmdReproducir) {
                publishMessage(ConfigMQTT.topicState, "PlayAll");
            }

            // --- Lógica del Menú de Edición ---
            else if (view.getId() == R.id.cmdPlayRow) {
                if (selectedRow != -1) {
                    publishMessage(ConfigMQTT.topicPlayRow, String.valueOf(selectedRow));
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
                setPhoneState(AppState.IDLE);
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
                        txtEspStatus.setText(String.format("Estado ESP: %s", value));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Changes the application's internal state and updates the UI.
     * @param newState The new state to transition to (IDLE or EDITING).
     */
    private void setPhoneState(AppState newState) {
        currentState = newState;
        if (newState == AppState.IDLE) {
            txtPhoneState.setText("Estado App: Idle");
        } else {
            txtPhoneState.setText("Estado App: Editando");
        }
        updateUIVisibility(); // This will handle showing/hiding menus
    }
}
