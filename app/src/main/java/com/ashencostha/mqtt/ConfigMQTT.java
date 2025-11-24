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
    public static final String TOPIC_PLAY_ROW_EMQX= "/simulator/playRow";
    public static final String TOPIC_SEND_MATRIX_EMQX= "/simulator/getcell";
    public static final String TOPIC_RECEIVE_MATRIX_EMQX= "/simulator/cellval";


    // Servidor Ubidots
    public static final String MQTT_SERVER_UBIDOTS = "tcp://industrial.api.ubidots.com:1883";
    public static final String USER_NAME_UBIDOTS = "BBUS-Z1MN1sGYdyPI8Mut0NahTzna5JVrBn";
    public static final String USER_PASS_UBIDOTS = "BBUS-Z1MN1sGYdyPI8Mut0NahTzna5JVrBn";
    public static final String TOPIC_STATUS_UBIDOTS = "/v1.6/devices/simulator/status";
    public static final String TOPIC_STATE_UBIDOTS = "/v1.6/devices/simulator/state";
    public static final String TOPIC_TEMPO_UBIDOTS = "/v1.6/devices/simulator/tempo";
    public static final String TOPIC_EDIT_UBIDOTS = "/v1.6/devices/simulator/edit";
    public static final String TOPIC_PLAY_ROW_UBIDOTS= "/v1.6/devices/simulator/playRow";
    public static final String TOPIC_SEND_MATRIX_UBIDOTS= "/v1.6/devices/simulator/getcell";
    public static final String TOPIC_RECEIVE_MATRIX_UBIDOTS= "/v1.6/devices/simulator/cellval";

    // Variables globales modificables
    public static String mqttServer;
    public static String userName;
    public static String userPass;
    public static String topicStatus;
    public static String topicState;
    public static String topicTempo;
    public static String topicEdit;
    public static String topicPlayRow;
    public static String topicSendMatrix;
    public static String topicReceiveMatrix;

    public static void useServerEMQX() {
        mqttServer = MQTT_SERVER_EMQX;
        userName = USER_NAME_EMQX;
        userPass = USER_PASS_EMQX;
        topicStatus = TOPIC_STATUS_EMQX;
        topicState = TOPIC_STATE_EMQX;
        topicTempo = TOPIC_TEMPO_EMQX;
        topicEdit = TOPIC_EDIT_EMQX;
        topicPlayRow = TOPIC_PLAY_ROW_EMQX;
        topicSendMatrix = TOPIC_SEND_MATRIX_EMQX;
        topicReceiveMatrix = TOPIC_RECEIVE_MATRIX_EMQX;
    }
    public static void useServerUBIDOTS() {
        mqttServer = MQTT_SERVER_UBIDOTS;
        userName = USER_NAME_UBIDOTS;
        userPass = USER_PASS_UBIDOTS;
        topicStatus = TOPIC_STATUS_UBIDOTS;
        topicState = TOPIC_STATE_UBIDOTS;
        topicTempo = TOPIC_TEMPO_UBIDOTS;
        topicEdit = TOPIC_EDIT_UBIDOTS;
        topicPlayRow = TOPIC_PLAY_ROW_UBIDOTS;
        topicSendMatrix = TOPIC_SEND_MATRIX_UBIDOTS;
        topicReceiveMatrix = TOPIC_RECEIVE_MATRIX_UBIDOTS;
    }
}
