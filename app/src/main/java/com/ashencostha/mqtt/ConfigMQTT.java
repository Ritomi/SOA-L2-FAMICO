package com.ashencostha.mqtt;
public class ConfigMQTT {

    public static final String CLIENT_ID = "AndroidClient";
    // Servidor EMQX
    public static final String MQTT_SERVER_EMQX = "tcp://broker.emqx.io:1883";
    public static final String USER_NAME_EMQX = "";
    public static final String USER_PASS_EMQX = "";
    public static final String TOPIC_STATUS_EMQX = "/simulator/status";
    public static final String TOPIC_STATE_EMQX = "/simulator/state";
    public static final String TOPIC_TEMPO_EMQX = "/simulator/tempo";
    public static final String TOPIC_EDIT_EMQX = "/simulator/edit";

    // Servidor Ubidots
    public static final String MQTT_SERVER_UBIDOTS = "tcp://industrial.api.ubidots.com:1883";
    public static final String USER_NAME_UBIDOTS = "BBUS-Z1MN1sGYdyPI8Mut0NahTzna5JVrBn";
    public static final String USER_PASS_UBIDOTS = "BBUS-Z1MN1sGYdyPI8Mut0NahTzna5JVrBn";
    public static final String TOPIC_STATUS_UBIDOTS = "/v1.6/devices/simulator/status";
    public static final String TOPIC_STATE_UBIDOTS = "/v1.6/devices/simulator/state";
    public static final String TOPIC_TEMPO_UBIDOTS = "/v1.6/devices/simulator/tempo";
    public static final String TOPIC_EDIT_UBIDOTS = "/v1.6/devices/simulator/edit";

    // Variables globales modificables
    public static String mqttServer;
    public static String userName;
    public static String userPass;
    public static String topicStatus;
    public static String topicState;
    public static String topicTempo;
    public static String topicEdit;


    // Métodos para seleccionar EMQX
    public static void useServerEMQX() {
        mqttServer = MQTT_SERVER_EMQX;
        userName = USER_NAME_EMQX;
        userPass = USER_PASS_EMQX;
        topicStatus = TOPIC_STATUS_EMQX;
        topicState = TOPIC_STATE_EMQX;
        topicTempo = TOPIC_TEMPO_EMQX;
        topicEdit = TOPIC_EDIT_EMQX;
    }

    // Métodos para seleccionar Ubidots
    public static void useServerUBIDOTS() {
        mqttServer = MQTT_SERVER_UBIDOTS;
        userName = USER_NAME_UBIDOTS;
        userPass = USER_PASS_UBIDOTS;
        topicStatus = TOPIC_STATUS_UBIDOTS;
        topicState = TOPIC_STATE_UBIDOTS;
        topicTempo = TOPIC_TEMPO_UBIDOTS;
        topicEdit = TOPIC_EDIT_UBIDOTS;
    }
}
