package com.ashencostha.mqtt;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// New Songs Library Imports
import android.content.DialogInterface;
import android.text.InputType;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements MatrixAdapter.OnCellEditListener, SensorEventListener {

    // --- Matriz y sus dimensiones ---
    private static final int ROWS = 16;
    private static final int COLS = 4;
    private int[][] matrixVals = new int[ROWS][COLS];
    // ---------------------------------

    private static final int LOAD_SONG_REQUEST_CODE = 1;

    // --- Componentes de la UI ---
    private GridView matrixGridView;
    private MatrixAdapter matrixAdapter;
    private TextView txtJson;
    private TextView txtStatus;
    private TextView txtEspStatus;
    private TextView txtPhoneState;
    // ------------------------------
    // --- Botones Menú Principal (Idle) ---
    private Button cmdEditar;
    private Button cmdStop;
    private Button cmdReproducir;
    private Button cmdSaveSong;
    private Button cmdOpenLibrary;
    private Button cmdSync;
    private Button cmdGoHome;
    private LinearLayout menuPrincipalLayout;
    // ----------------------------

    // --- Botones Menú Edición ---
    private Button cmdPlayRow;
    private Button cmdSave;
    private Button cmdBackToMenu;
    private LinearLayout menuEdicionLayout;
    // ----------------------------

    // --- Botones Menú Sincronizacion ---
    private Button cmdSyncBack;
    private Button cmdSendMatrix;
    private Button cmdReceiveMatrix;
    private LinearLayout menuSyncLayout;
    // ----------------------------

    // --- Gestión de Estado y Selección ---
    private enum AppState { IDLE, EDITING, SYNC }
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

    // --- Sensor (Giroscopio) ---
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private long lastSensorUpdateTime = 0;
    // ----------------------------

    // --- Shake Detection ---
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 800;
    private boolean firstShakeSample = true;
    // ----------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        // --- Vinculación de Vistas ---
        txtJson = findViewById(R.id.txtJson);
        txtEspStatus = findViewById(R.id.txtEspStatus);
        txtPhoneState = findViewById(R.id.txtPhoneState);
        matrixGridView = findViewById(R.id.matrixGridView);
        // ------------------------------

        // Vistas del menú principal
        cmdSaveSong = findViewById(R.id.cmdSaveSong);
        cmdOpenLibrary = findViewById(R.id.cmdOpenLibrary);
        cmdEditar = findViewById(R.id.cmdEditar);
        cmdStop = findViewById(R.id.cmdStop);
        cmdReproducir = findViewById(R.id.cmdReproducir);
        cmdSync = findViewById(R.id.cmdSync);
        menuPrincipalLayout = findViewById(R.id.menuPrincipalLayout);
        // ------------------------------

        // Vistas del menú de edición
        cmdPlayRow = findViewById(R.id.cmdPlayRow);
        cmdSave = findViewById(R.id.cmdSave);
        cmdBackToMenu = findViewById(R.id.cmdBackToMenu);
        menuEdicionLayout = findViewById(R.id.menuEdicionLayout);
        // ------------------------------

        // Vistas del menú de sincronización
        cmdSyncBack = findViewById(R.id.cmdSyncBack);
        cmdSendMatrix = findViewById(R.id.cmdSendMatrix);
        cmdReceiveMatrix = findViewById(R.id.cmdReceiveMatrix);
        menuSyncLayout = findViewById(R.id.menuSyncLayout);
        // ------------------------------

        // --- Configuración de Listeners ---
        cmdSaveSong.setOnClickListener(botonesListeners);
        cmdOpenLibrary.setOnClickListener(botonesListeners);
        cmdEditar.setOnClickListener(botonesListeners);
        cmdStop.setOnClickListener(botonesListeners);
        cmdReproducir.setOnClickListener(botonesListeners);
        cmdPlayRow.setOnClickListener(botonesListeners);
        cmdSave.setOnClickListener(botonesListeners);
        cmdBackToMenu.setOnClickListener(botonesListeners);
        cmdSync.setOnClickListener(botonesListeners);
        cmdSyncBack.setOnClickListener(botonesListeners);
        cmdSendMatrix.setOnClickListener(botonesListeners);
        cmdReceiveMatrix.setOnClickListener(botonesListeners);
        // ----------------------------------

        // --- NEW: Inicialización del Sensor Manager ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                Toast.makeText(this, "Acelerómetro no encontrado. No se podrá agitar para reproducir.", Toast.LENGTH_LONG).show();
            }
        }

        // --- Lógica de la Matriz ---
        initializeMatrix();
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            Intent intent = new Intent(MainActivity.this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private void updateUIVisibility() {
        menuPrincipalLayout.setVisibility(View.GONE);
        menuEdicionLayout.setVisibility(View.GONE);
        menuSyncLayout.setVisibility(View.GONE);
        if (currentState == AppState.IDLE) {
            menuPrincipalLayout.setVisibility(View.VISIBLE);
            matrixAdapter.setSelection(-1, -1);
            selectedRow = -1;
            selectedCol = -1;
        } else if (currentState == AppState.EDITING) {
            menuEdicionLayout.setVisibility(View.VISIBLE);
        } else if (currentState == AppState.SYNC) {
            menuSyncLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCellEdited(int row, int col, int value) {
        if (currentState == AppState.IDLE) {
            selectedRow = row;
            selectedCol = col;
            matrixAdapter.setSelection(row, col);
            txtJson.setText("Celda (" + row + ", " + col + ") seleccionada. Presiona EDITAR.");
        }
    }

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
                    setPhoneState(AppState.EDITING);
                    matrixAdapter.setEditing(true);
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, seleccione una celda primero", Toast.LENGTH_SHORT).show();
                }
            } else if (view.getId() == R.id.cmdReproducir) {
                publishMessage(ConfigMQTT.topicState, "PlayAll");
            } else if (view.getId() == R.id.cmdStop) {
                publishMessage(ConfigMQTT.topicState, "Idle");
            } else if (view.getId() == R.id.cmdSaveSong) {
                showSaveSongDialog();
            } else if (view.getId() == R.id.cmdOpenLibrary) {
                Intent intent = new Intent(MainActivity.this, SavedSongsActivity.class);
                startActivityForResult(intent, LOAD_SONG_REQUEST_CODE);
            } else if (view.getId() == R.id.cmdSync) {
                setPhoneState(AppState.SYNC);
            }
            //---------------------------------------------------

            // --- Lógica del Menú de Edición ---
            else if (view.getId() == R.id.cmdPlayRow) {
                if (selectedRow != -1) {
                    publishMessage(ConfigMQTT.topicPlayRow, String.valueOf(selectedRow));
                }
            } else if (view.getId() == R.id.cmdSave) {
                if (selectedRow != -1 && selectedCol != -1) {
                    int valueToSave = matrixVals[selectedRow][selectedCol];
                    try {
                        publishMessage(ConfigMQTT.topicEdit, String.valueOf(selectedRow) + " "
                                + String.valueOf(selectedCol) + " "
                                + String.valueOf(valueToSave));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (view.getId() == R.id.cmdBackToMenu) {
                currentState = AppState.IDLE;
                matrixAdapter.setEditing(false);
                updateUIVisibility();
                setPhoneState(AppState.IDLE);
                publishMessage(ConfigMQTT.topicState, "Idle");
            }
            //---------------------------------------------------

            // --- Lógica del Menú de Sincronizacion ---
            else if (view.getId() == R.id.cmdSyncBack) {
                setPhoneState(AppState.IDLE);
            } else if (view.getId() == R.id.cmdSendMatrix) {
                sendMatrixAsString();
            }else if (view.getId() == R.id.cmdReceiveMatrix) {
                subscribeToTopic(ConfigMQTT.topicReceiveMatrix);
                Toast.makeText(MainActivity.this, "Esperando matriz del ESP...", Toast.LENGTH_SHORT).show();
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
                String message = intent.getStringExtra("msgJson");

                if (topic == null || message == null) {
                    return;
                }
                txtJson.setText(String.format("Tópico: %s, Mensaje: %s", topic, message));

                try {
                    if (topic.equals(ConfigMQTT.topicStatus)) {
                        txtEspStatus.setText(String.format("Estado ESP: %s", message));
                    }

                    if (topic.equals(ConfigMQTT.topicReceiveMatrix)) {
                        if(currentState == AppState.SYNC)
                        {
                            updateMatrixFromString(message);
                            mqttHandler.unsubscribe(ConfigMQTT.topicReceiveMatrix);
                        } else {
                            Toast.makeText(MainActivity.this, "Debe estar en la pantalla de Sincronizacion para recibir la matriz", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setPhoneState(AppState newState) {
        currentState = newState;
        if (newState == AppState.IDLE) {
            txtPhoneState.setText("Estado App: Idle");
            if (gyroscope != null) {
                sensorManager.unregisterListener(this, gyroscope);
            }
        } else if (newState == AppState.EDITING) {
            txtPhoneState.setText("Estado App: Editando");
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
            } else {
                Toast.makeText(this, "Giroscopio no encontrado", Toast.LENGTH_SHORT).show();
            }
        } else if (newState == AppState.SYNC) {
            txtPhoneState.setText("Estado App: Sync");
        }
        updateUIVisibility();
    }
    // Accelerometer Methods -----------------------------
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    //---------------------------------------------------

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Esta para que no de error
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && currentState == AppState.EDITING) {

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastSensorUpdateTime) < 200) {
                return;
            }
            lastSensorUpdateTime = currentTime;

            float rotationY = event.values[1];

            float threshold = 0.8f;

            if (rotationY > threshold) { // Giro a la derecha -> Aumentar valor
                incrementCellValue();
            } else if (rotationY < -threshold) { // Giro a la izquierda -> Disminuir valor
                decrementCellValue();
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastSensorUpdateTime) > 100) {
                long timeDiff = (currentTime - lastSensorUpdateTime);
                lastSensorUpdateTime = currentTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                if (firstShakeSample) {
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    firstShakeSample = false;
                } else {
                    float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / timeDiff * 10000;

                    if (speed > SHAKE_THRESHOLD) {
                        if (currentState == AppState.IDLE) {
                            Toast.makeText(this, "¡Shake detectado! Reproduciendo...", Toast.LENGTH_SHORT).show();
                            publishMessage(ConfigMQTT.topicState, "PlayAll");
                            lastSensorUpdateTime = currentTime + 1000;
                        }
                    }
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                }
            }
        }
    }

    // --- Helper methods for changing cell value by using the gyroscope ---
    private void incrementCellValue() {
        if (selectedRow == -1 || selectedCol == -1) return;

        int currentValue = matrixVals[selectedRow][selectedCol];
        int newValue = currentValue + 1;
        int maxValue = (selectedCol == 0) ? 15 : 127;

        if (newValue > maxValue) {
            newValue = maxValue;
        }
        updateCellValue(newValue);
    }

    private void decrementCellValue() {
        if (selectedRow == -1 || selectedCol == -1) return;

        int currentValue = matrixVals[selectedRow][selectedCol];
        int newValue = currentValue - 1;

        if (newValue < 0) {
            newValue = 0;
        }

        updateCellValue(newValue);
    }

    private void updateCellValue(int newValue) {
        matrixVals[selectedRow][selectedCol] = newValue;
        matrixAdapter.notifyDataSetChanged();
        txtJson.setText("Valor cambiado por giroscopio: " + newValue);
    }
    //---------------------------------------------------

    // --- Helper methods for saving songs ---
    private void showSaveSongDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Canción");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nombre de la canción");
        builder.setView(input);

        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String songName = input.getText().toString();
                if (!songName.isEmpty()) {
                    saveSong(songName);
                } else {
                    Toast.makeText(MainActivity.this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveSong(String name) {
        SharedPreferences prefs = getSharedPreferences(SavedSongsActivity.SONGS_PREFS_KEY, MODE_PRIVATE);
        String json = prefs.getString(SavedSongsActivity.SONGS_LIST_KEY, null);
        Gson gson = new Gson();
        Type type = new com.google.gson.reflect.TypeToken<ArrayList<Song>>() {}.getType();
        ArrayList<Song> songList = gson.fromJson(json, type);
        if (songList == null) {
            songList = new ArrayList<>();
        }

        // Create a deep copy of the matrix to save
        int[][] matrixToSave = new int[ROWS][COLS];
        for(int i=0; i<ROWS; i++) {
            for(int j=0; j<COLS; j++) {
                matrixToSave[i][j] = matrixVals[i][j];
            }
        }

        songList.add(new Song(name, matrixToSave));

        // Save the updated list back to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        String updatedJson = gson.toJson(songList);
        editor.putString(SavedSongsActivity.SONGS_LIST_KEY, updatedJson);
        editor.apply();

        Toast.makeText(this, "Canción '" + name + "' guardada.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOAD_SONG_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("loadedSong")) {
                Song loadedSong = (Song) data.getSerializableExtra("loadedSong");
                if (loadedSong != null) {
                    // Load the song's matrix into our current matrix
                    this.matrixVals = loadedSong.getMatrix();
                    // IMPORTANT: Tell the adapter that the underlying data has completely changed
                    matrixAdapter = new MatrixAdapter(this, matrixVals, this);
                    matrixGridView.setAdapter(matrixAdapter);

                    Toast.makeText(this, "Canción '" + loadedSong.getName() + "' cargada.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    //---------------------------------------------------

    // --- Helper methods for synchronizing the matrix ---
    private void sendMatrixAsString() {
        StringBuilder matrixString = new StringBuilder();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                matrixString.append(matrixVals[i][j]);
                if (i < ROWS - 1 || j < COLS - 1) {
                    matrixString.append(" ");
                }
            }
        }
        publishMessage(ConfigMQTT.topicSendMatrix, matrixString.toString());
        Toast.makeText(this, "Matriz enviada!", Toast.LENGTH_SHORT).show();
    }

    private void updateMatrixFromString(String matrixString) {
        String[] values = matrixString.trim().split("\\s+");
        int valueIndex = 0;

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (valueIndex < values.length) {
                    try {
                        matrixVals[i][j] = Integer.parseInt(values[valueIndex]);
                        valueIndex++;
                    } catch (NumberFormatException e) {
                        matrixVals[i][j] = 0;
                    }
                } else {
                    matrixVals[i][j] = 0;
                }
            }
        }
        matrixAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Matriz recibida y actualizada!", Toast.LENGTH_SHORT).show();
    }
    //---------------------------------------------------
}
