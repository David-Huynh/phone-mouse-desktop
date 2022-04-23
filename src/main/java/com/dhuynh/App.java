package com.dhuynh;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.glassfish.tyrus.server.Server;
import java.awt.Robot;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.InputEvent;

/**
 * JavaFX App that displays a simple GUI with a button and a text field to update
 * sensitivity of the artificial mouse.
 */
public class App extends Application {
    private static Robot robot = null;
    private static Scene scene;
    private static Thread robotThread;

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

    
        // Add HBox to VBox
        mainContainer.getChildren().addAll(sensitivityBox);
        
        scene = new Scene(mainContainer, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        BlockingQueue<Integer[]> queue = (BlockingQueue<Integer[]>) SocketQueue.getInstance();

        Server server = new Server ("localhost", 8025, "/websockets", null, PhoneServerEndpoint.class);

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
        }
    }
}