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
    private Button cmdEditar;
    private Button cmdReproducir;
    // ----------------------------

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
        cmdEditar = findViewById(R.id.cmdEditar);
        cmdReproducir = findViewById(R.id.cmdReproducir);
        matrixGridView = findViewById(R.id.matrixGridView);
        // ------------------------------

        // --- Configuración de Listeners ---
        cmdEditar.setOnClickListener(botonesListeners);
        cmdReproducir.setOnClickListener(botonesListeners);
        // ----------------------------------

        // --- Lógica de la Matriz ---
        initializeMatrix(); // Llena la matriz con ceros
        // Se crea el adapter, pasando 'this' como el listener para que la actividad reciba los eventos
        matrixAdapter = new MatrixAdapter(this, matrixVals, this);
        matrixGridView.setAdapter(matrixAdapter);
        // ---------------------------

        // --- Conexión MQTT y Configuración de Receivers ---
        mqttHandler = new MqttHandler(getApplicationContext());
        configurarBroadcastReceiver();
        connect();
        // -------------------------------------------------
    }

    /**
     * Este método es OBLIGATORIO porque lo exige la interfaz OnCellEditListener.
     * Se ejecuta automáticamente cada vez que una celda de la matriz es editada.
     * @param row Fila de la celda modificada.
     * @param col Columna de la celda modificada.
     * @param value El nuevo valor numérico de la celda.
     */
    @Override
    public void onCellEdited(int row, int col, int value) {
        JSONObject payload = new JSONObject();
        try {
            // Creamos un JSON con la información precisa del cambio
            payload.put("row", row);
            payload.put("col", col);
            payload.put("value", value);

            // Publicamos el mensaje en el tópico dedicado a la matriz
            publishMessage(ConfigMQTT.topicEdit, payload.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al crear JSON", Toast.LENGTH_SHORT).show();
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
            int id = view.getId();
            if (id == R.id.cmdEditar) {
                publishMessage(ConfigMQTT.topicState, "Edit");
            } else if (id == R.id.cmdReproducir) {
                publishMessage(ConfigMQTT.topicState, "PlayAll");
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
