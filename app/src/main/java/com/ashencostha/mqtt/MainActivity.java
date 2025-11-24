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
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MatrixAdapter.OnCellEditListener, SensorEventListener {

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
    private Button cmdStop;
    private Button cmdReproducir;
    private Button cmdSaveSong;
    private Button cmdOpenLibrary;

    private Button cmdGoHome; // botón que vuelve a la SplashActivity

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

    // --- Sensor (Giroscopio) ---
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private long lastSensorUpdateTime = 0; // To limit update rate
    // ----------------------------

    // --- Shake Detection ---
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 800; // Adjust this for sensitivity
    private boolean firstShakeSample = true;
    // ----------------------------

    private static final int LOAD_SONG_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // muestra back arrow
        }


        // --- Vinculación de Vistas ---
        txtJson = findViewById(R.id.txtJson);
        txtEspStatus = findViewById(R.id.txtEspStatus);
        txtPhoneState = findViewById(R.id.txtPhoneState);
        matrixGridView = findViewById(R.id.matrixGridView);

        // Vistas del menú principal
        cmdSaveSong = findViewById(R.id.cmdSaveSong);
        cmdOpenLibrary = findViewById(R.id.cmdOpenLibrary);
        cmdEditar = findViewById(R.id.cmdEditar);
        cmdStop = findViewById(R.id.cmdStop);
        cmdReproducir = findViewById(R.id.cmdReproducir);
        menuPrincipalLayout = findViewById(R.id.menuPrincipalLayout); // Asume que tienes un LinearLayout con este ID



        // Vistas del menú de edición
        cmdPlayRow = findViewById(R.id.cmdPlayRow); // Asume que tienes este botón en tu layout
        cmdSave = findViewById(R.id.cmdSave);         // Asume que tienes este botón en tu layout
        cmdBackToMenu = findViewById(R.id.cmdBackToMenu); // Asume que tienes este botón en tu layout
        menuEdicionLayout = findViewById(R.id.menuEdicionLayout); // Asume que tienes un LinearLayout con este ID
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
        // ----------------------------------

        // --- NEW: Inicialización del Sensor Manager ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // <-- GET THE SENSOR
            if (accelerometer == null) {
                Toast.makeText(this, "Acelerómetro no encontrado. No se podrá agitar para reproducir.", Toast.LENGTH_LONG).show();
            }
        }

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

    @Override
    public boolean onSupportNavigateUp() {
        // Mantenemos finish() para la flechita de la ActionBar
        onBackPressed(); // delegamos en onBackPressed para tener comportamiento consistente
        return true;
    }

    @Override
    public void onBackPressed() {
        // Si MainActivity es la raíz de la tarea, abrimos SplashActivity
        if (isTaskRoot()) {
            Intent intent = new Intent(MainActivity.this, SplashActivity.class);
            // Evita crear múltiples instancias y limpia la posible pila intermedia
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            // Si no es la raíz, mantenemos el comportamiento por defecto (volver a la actividad anterior)
            super.onBackPressed();
        }
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
            }

            // --- Lógica del Menú de Edición ---
            else if (view.getId() == R.id.cmdPlayRow) {
                if (selectedRow != -1) {
                    publishMessage(ConfigMQTT.topicPlayRow, String.valueOf(selectedRow));
                }
            } else if (view.getId() == R.id.cmdSave) {
                if (selectedRow != -1 && selectedCol != -1) {
                    int valueToSave = matrixVals[selectedRow][selectedCol];
                    try {
                        // Publicamos el mensaje en el tópico dedicado a la matriz
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
            // --- STOP listening to the gyroscope to save battery ---
            if (gyroscope != null) {
                sensorManager.unregisterListener(this, gyroscope);
            }
        } else { // EDITING
            txtPhoneState.setText("Estado App: Editando");
            // --- START listening to the gyroscope ---
            if (gyroscope != null) {
                // SENSOR_DELAY_UI is a good rate for UI updates
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
            } else {
                Toast.makeText(this, "Giroscopio no encontrado", Toast.LENGTH_SHORT).show();
            }
        }
        updateUIVisibility(); // This will handle showing/hiding menus
    }
    // Accelerometer Methods
    @Override
    protected void onResume() {
        super.onResume();
        // Register the accelerometer to listen for shakes
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Always unregister listeners when the activity is not visible
        sensorManager.unregisterListener(this);
    }
    //---------------------------------------------------

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // This method is part of the SensorEventListener interface.
        // You can leave it empty if you don't need to handle accuracy changes.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Nos aseguramos de que el evento es del giroscopio y estamos en modo EDITING
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && currentState == AppState.EDITING) {

            // --- Control para limitar la tasa de actualización ---
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastSensorUpdateTime) < 200) { // Actualiza cada 200ms
                return;
            }
            lastSensorUpdateTime = currentTime;
            // ---------------------------------------------------

            // El giroscopio mide la velocidad de rotación en rad/s en los ejes x, y, z.
            // Usaremos el eje Y (giro del teléfono a izquierda/derecha).
            float rotationY = event.values[1];

            // Definimos un umbral para ignorar pequeños movimientos involuntarios
            float threshold = 0.8f;

            if (rotationY > threshold) { // Giro a la derecha -> Aumentar valor
                incrementCellValue();
            } else if (rotationY < -threshold) { // Giro a la izquierda -> Disminuir valor
                decrementCellValue();
            }
        }

        // --- Shake detection logic ---
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            // Only check for shakes every 100ms
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
                        // --- SHAKE DETECTED! ---
                        // Only play if we are in IDLE mode to avoid conflicts
                        if (currentState == AppState.IDLE) {
                            Toast.makeText(this, "¡Shake detectado! Reproduciendo...", Toast.LENGTH_SHORT).show();
                            publishMessage(ConfigMQTT.topicState, "PlayAll");
                            // Reset the time to avoid multiple triggers from one shake
                            lastSensorUpdateTime = currentTime + 1000; // Add a 1-second cooldown
                        }
                    }
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                }
            }
        }
    }

    // --- Helper methods for changing cell value ---

    private void incrementCellValue() {
        if (selectedRow == -1 || selectedCol == -1) return;

        int currentValue = matrixVals[selectedRow][selectedCol];
        int newValue = currentValue + 1;

        // Validar segun la columna (0-15 para col 0, 0-127 para las demás)
        int maxValue = (selectedCol == 0) ? 15 : 127;

        if (newValue > maxValue) {
            newValue = maxValue; // No pasar del máximo
        }

        updateCellValue(newValue);
    }

    private void decrementCellValue() {
        if (selectedRow == -1 || selectedCol == -1) return;

        int currentValue = matrixVals[selectedRow][selectedCol];
        int newValue = currentValue - 1;

        if (newValue < 0) {
            newValue = 0; // No bajar de cero
        }

        updateCellValue(newValue);
    }

    private void updateCellValue(int newValue) {
        // Actualizamos el modelo de datos
        matrixVals[selectedRow][selectedCol] = newValue;
        // Notificamos al adapter para que refresque la vista de la matriz
        matrixAdapter.notifyDataSetChanged();
        // Opcional: Mostrar el nuevo valor en el txtJson
        txtJson.setText("Valor cambiado por giroscopio: " + newValue);
    }

    private void showSaveSongDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Canción");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nombre de la canción");
        builder.setView(input);

        // Set up the buttons
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

    // This method gets the result from SavedSongsActivity
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
}
