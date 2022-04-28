package com.dhuynh;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;


import java.awt.Robot;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.InputEvent;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.glassfish.tyrus.server.Server;


/**
 * JavaFX App that displays a simple GUI with a button and a text field to update
 * sensitivity of the artificial mouse.
 */
public class App extends Application {
    
    private static Scene scene;

    private static Thread robotThread;
    private static Robot robot = null;

    // Default Sensitivity Values for X and Y
    private static int xSensitivity = 10;
    private static int ySensitivity = 10;
    // Helper function to parse sensitivity values from user input
    public static int testSensitivity(TextField sensitivity) {
        int testSensitivity = Integer.parseInt(sensitivity.getText());
        if (testSensitivity <= 0 || testSensitivity > 100) {
            throw new NumberFormatException();
        }
        return testSensitivity;
    }

    static JmDNS jmdns = null;

    @Override
    public void start(Stage stage) throws IOException {

        VBox mainContainer = new VBox();

        // Create a text field for the user to enter sensitivity values
        HBox sensitivityBox = new HBox();
        Button setSensitivityButton = new Button("Set");
        TextField xSensitivityField = new TextField();
        TextField ySensitivityField = new TextField();
        xSensitivityField.setPromptText("X Sensitivity");
        ySensitivityField.setPromptText("Y Sensitivity");
        setSensitivityButton.setOnAction( e -> {
            try {
                // Set X and Y Sensitivity Values
                xSensitivity = testSensitivity(xSensitivityField);
                ySensitivity = testSensitivity(ySensitivityField);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid sensitivity value");
            }
        });
        // Add sensitivity text field and button to HBox
        sensitivityBox.getChildren().addAll(xSensitivityField, ySensitivityField, setSensitivityButton);
        sensitivityBox.setSpacing( 10.0d );
        sensitivityBox.setAlignment( Pos.CENTER );
        sensitivityBox.setPadding( new Insets(40) );

        // Creates a button to register the service with JmDNS for zeroConf
        HBox serviceBox = new HBox();
        TextField serviceText = new TextField();
        serviceText.setPromptText("Service Registration Status");
        serviceText.setEditable(false);
        serviceText.setPrefWidth(200);
        Button registerServiceButton = new Button("Register Service");
        // BUG: Hangs on exit if button is pressed

        registerServiceButton.setOnAction( evt -> {
            try {
                jmdns = JmDNS.create(InetAddress.getLocalHost());
                ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", "PhoneServer", 8025, "path=/websockets");
                jmdns.registerService(serviceInfo);
                serviceText.setText("Service Registered: " + InetAddress.getLocalHost().getHostAddress() + ":8025");
            } catch (UnknownHostException e) {
                System.err.println("Unknown Host");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("IO Exception");
                e.printStackTrace();
            }
        });

        serviceBox.getChildren().addAll(serviceText, registerServiceButton);
        serviceBox.setSpacing( 10.0d );
        serviceBox.setAlignment( Pos.CENTER );
        serviceBox.setPadding( new Insets(40) );


        // Add HBox to VBox
        mainContainer.getChildren().addAll(sensitivityBox, serviceBox);
        
        scene = new Scene(mainContainer, 640, 480);
        stage.setScene(scene);
        stage.show();
    }
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    public static void main(String[] args) {
        BlockingQueue<Integer[]> queue = (BlockingQueue<Integer[]>) SocketQueue.getInstance();

        Server server = new Server ("0.0.0.0", 8025, "/websockets", null, PhoneServerEndpoint.class);

        try {
            server.start();
            // Start robot thread to listen to web socket data and move mouse accordingly
            robotThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    while(true){
                        try {
                            if (Thread.interrupted()) {
                                break;
                            }
                            robot = new Robot();
                            Integer[] arr = queue.take();
                            if (arr != null) {
                                // Get current mouse position
                                Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                                // Move mouse to new position based on sensitivity values
                                if (arr[0] == -200) {
                                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                } else if (arr[0] == 200) {
                                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                } else {
                                    int x = mousePosition.x + arr[0] * xSensitivity;
                                    int y = mousePosition.y + arr[1] * ySensitivity;
                                    robot.mouseMove(x, y);
                                }
                            }
                        } catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }});
            // Start robot thread
            robotThread.start();
            // Start JavaFX application Blocks until application is closed
            launch();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Stop robot thread and server
            server.stop();
            robotThread.interrupt();
            jmdns.unregisterAllServices();
        }
    }
}