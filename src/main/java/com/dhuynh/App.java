package com.dhuynh;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.glassfish.tyrus.server.Server;
import java.awt.Robot;

/**
 * JavaFX App
 */
public class App extends Application {
    private static Robot robot = null;
    private static Scene scene;
    private static Thread robotThread;


    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
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

        Server server = new Server ("localhost", 53532, "/websockets", null, PhoneServerEndpoint.class);
        
        try {
            server.start();
            robotThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    while(true){
                        try {
                            robot = new Robot();
                            Integer[] arr = queue.take();
                            if (arr != null) {
                                robot.mouseMove(arr[0], arr[1]);

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }});
            robotThread.start();
            launch();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
            robotThread.interrupt();
        }
    }

}