package org.example.virtual_tour;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        TourInterface tour = new TourInterface();
        Scene scene = new Scene(tour.createInterface(primaryStage));

        primaryStage.setTitle("Maseru Virtual Tour Guide");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}