package de.oberdorf_itc.acs.mqtt;

import de.oberdorf_itc.acs.model.AccessEvent;
import de.oberdorf_itc.acs.model.StatusEvent;
import io.prometheus.metrics.core.metrics.Counter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Map;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublisher {
    private static final Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.acs.mqtt.MqttPublisher.class);
    private Map<String, Object> configuration = new HashMap<String, Object>();
    private Map<String, Counter> metrics = new HashMap<String, Counter>();
    private MqttClient mqttClient;
    private String mqttBroker;

    /**
     * Constructor for MqttPublisher class. Initializes the MQTT publisher with the provided configuration.
     * @param configuration A map containing the configuration parameters for the MQTT publisher, such as broker URL, client ID, topic prefix, and TLS settings.
     * @return None
     */
    public MqttPublisher(Map<String, Object> configuration, Map<String, Counter> metrics) {
        logger.trace("Constructor: MqttPublisher(Map<String, Object> configuration = {}, Map<String, Counter> metrics = {})", configuration, metrics);
        this.metrics = metrics;
        this.configuration = configuration;
        }

    /**
     * Connect to the MQTT broker using the configuration parameters provided in the constructor. Set up the connection options, including TLS settings if enabled, and establish a connection to the broker. Log the connection status and handle any exceptions that may occur during the connection process.
     * @param None
     * @return None
     * @throws MqttException if an error occurs while connecting to the MQTT broker
     * @throws CertificateException if an error occurs while processing the certificate for TLS connection
     * @throws KeyStoreException if an error occurs while loading the key store
     * @throws IOException if an error occurs while reading the certificate file
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     * @throws KeyManagementException if an error occurs while managing the key store
     */
    public void connect() throws MqttException, CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException {
        logger.trace("Method: connect()");

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setKeepAliveInterval(60);

        String mqttBroker = (String) this.configuration.get("MQTT_SERVER") + ":" + this.configuration.get("MQTT_PORT");
        if ((boolean) this.configuration.get("MQTT_TLS")) {
            mqttBroker = "ssl://" + mqttBroker;

            if ((boolean) this.configuration.get("MQTT_TLS_INSECURE")) {
                System.setProperty("com.ibm.ssl.disableHostnameVerification", "true");
            }
            if (this.configuration.get("MQTT_CACERT_FILE") != null) {
                System.setProperty("javax.net.ssl.trustStore", (String) this.configuration.get("MQTT_CACERT_FILE"));
                InputStream certInput = new FileInputStream((String) this.configuration.get("MQTT_CACERT_FILE"));
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Collection<? extends Certificate> certs = certFactory.generateCertificates(certInput);
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                int index = 0;
                for (Certificate cert : certs) {
                    String alias = "serverr_ca_" + index++;
                    trustStore.setCertificateEntry(alias, cert);
                }
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
                SSLContext.setDefault(sslContext);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                connOpts.setSocketFactory(sslSocketFactory);
            }
        } else {
            mqttBroker = "tcp://" + mqttBroker;
        }
        this.mqttBroker = mqttBroker;
        logger.debug("MQTT Broker: {}", mqttBroker);

        String mqttClientId = (String) this.configuration.get("MQTT_CLIENT_ID");
        if (mqttClientId == null) {
            mqttClientId = MqttClient.generateClientId();
        }
        logger.debug("MQTT Client ID: {}", mqttClientId);

        if (this.configuration.get("MQTT_USERNAME") != null && this.configuration.get("MQTT_PASSWORD") != null) {
            logger.debug("MQTT Username: {}", this.configuration.get("MQTT_USERNAME"));
            logger.debug("MQTT Password: set");
            connOpts.setUserName((String) this.configuration.get("MQTT_USERNAME"));
            String password = (String) this.configuration.get("MQTT_PASSWORD");
            connOpts.setPassword(password.toCharArray());
        } else {
            logger.debug("MQTT Username: not set");
        }

        // Specify directory for storing messages
        logger.debug("Define MQTT persistence directory to: {}", this.configuration.get("java.io.tmpdir"));
        MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence((String) this.configuration.get("java.io.tmpdir"));

        @SuppressWarnings("resource")
        MqttClient mqttClient = new MqttClient(mqttBroker, mqttClientId, persistence);
        try {
            mqttClient.connect(connOpts);
        } catch (MqttException mqttException) {
            logger.error("Error connecting to MQTT server: {}", mqttException.getMessage());
            mqttException.printStackTrace();
            System.exit(1);
        }
        logger.debug("Successfully connected to MQTT server.");
    }

    /**
     * Disconnect from the MQTT broker if the client is currently connected. Log the disconnection event.
     * @param None
     * @return None
     * @throws MqttException if an error occurs while disconnecting from the MQTT broker
     */
    public void disconnect() throws MqttException {
        logger.trace("Method: disconnect()");
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            logger.info("Disconnected from MQTT broker at {}", mqttBroker);
        }
    }

    /**
     * Publishes a RFID readers keepalive status event.
     *
     * Topic: [..]/oitc-acs/entrypoints/<readerIp>/status
     *
     * @param readerIp  IPv4 address of the RFID reader
     * @param event     Keepalive status event data
     * @throws MqttException if an error occurs while publishing the message to the MQTT broker
     */
    public void publishStatus(String readerIp, StatusEvent event) throws MqttException {
        logger.trace("Method: publishStatus(String readerIp = {}, StatusEvent event = {})", readerIp, event);
        String topic   = this.configuration.get("MQTT_TOPIC_PREFIX") + "/" + readerIp + "/status";
        String payload = JsonUtil.toJson(event);
        metrics.get("reader_notification_events").labelValues(readerIp).inc();
        publish(topic, payload);
    }

    /**
     * Publishes an access event (granted or denied).
     *
     * Topic: [..]/oitc-acs/entrypoints/<readerIp>/access
     *
     * @param readerIp  IPv4 address of the RFID reader
     * @param event     Access event data
     * @throws MqttException if an error occurs while publishing the message to the MQTT broker
     */
    public void publishAccess(String readerIp, AccessEvent event) throws MqttException {
        logger.trace("Method: publishAccess(String readerIp = {}, AccessEvent event = {})", readerIp, event);
        String topic   = this.configuration.get("MQTT_TOPIC_PREFIX") + "/" + readerIp + "/access";
        String payload = JsonUtil.toJson(event);
        if (event.getStatus() != null && event.getStatus().equals("granted")) {
            metrics.get("mqtt_messages_sent_access_granted").labelValues(readerIp).inc();
        } else {
            metrics.get("mqtt_messages_sent_access_denied").labelValues(readerIp).inc();
        }
        publish(topic, payload);
    }

    /**
     * Publishes a message to the specified MQTT topic with the given JSON payload. The method checks if the MQTT client is connected before attempting to publish the message. It sets the Quality of Service (QoS) and retained flag based on the configuration parameters, and logs the published message. If the MQTT client is not connected, it throws an IllegalStateException.
     * @param topic The MQTT topic to which the message should be published
     * @param jsonPayload The JSON payload to be published to the MQTT topic
     * @throws MqttException if an error occurs while publishing the message to the MQTT broker
     */
    private void publish(String topic, String jsonPayload) throws MqttException {
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new IllegalStateException("MQTT client is not connected");
        }
        MqttMessage msg = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
        msg.setQos(this.configuration.get("MQTT_QOS") != null ? (int) this.configuration.get("MQTT_QOS") : 1);
        msg.setRetained(this.configuration.get("MQTT_RETAIN") != null ? (boolean) this.configuration.get("MQTT_RETAIN") : false);
        mqttClient.publish(topic, msg);
        metrics.get("mqtt_messages_sent").inc();
        logger.debug("Published message to topic {}: {}", topic, jsonPayload);
    }
}
