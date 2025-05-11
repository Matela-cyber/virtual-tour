package org.example.virtual_tour;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TourInterface {
    private ImageView mapView;
    private ImageView landmarkView;
    private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private MediaPlayer audioPlayer;
    private Button playBtn;
    private Button playAudioBtn;
    private Button viewImagesBtn;
    private Button restartBtn;
    private Button stopVideoBtn;
    private Button endVideoBtn;
    private Button goToQuizBtn;
    private VBox quizBox;
    private Pane mapPane;
    private HBox titleBox;
    private Label titleLabel;
    private Stage primaryStage;
    private int currentLandmark = -1;
    private boolean landmarkSelected = false;
    private boolean audioPlaying = false;
    private boolean videoPlaying = false;
    private StackPane contentPane; // Added to reference the content pane

    private final String[] landmarks = {
            "Mokorotlong", "Royal Palace",
            "Setsoto Stadium", "Parliament"
    };

    private final double[][] videoSegments = {
            {46, 110}, {240, 280}, {367, 410}, {484, 542}
    };

    private final String[][] quizData = {
            {"What is Mokorotlong known for?", "Cultural center", "Mining area", "Government offices", "1"},
            {"When was the Royal Palace built?", "1800s", "1900s", "2000s", "2"},
            {"What is Setsoto Stadium's capacity?", "10,000", "20,000", "30,000", "2"},
            {"How many Parliament seats?", "80", "120", "150", "2"}
    };

    private List<Circle> hotspotCircles = new ArrayList<>();
    private List<HBox> hotspotLabels = new ArrayList<>();

    public Pane createInterface(Stage stage) {
        this.primaryStage = stage;

        // Setup title label and restart button (initially hidden)
        titleLabel = new Label("MASERU");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        restartBtn = new Button("BacK to Map");
        styleButton(restartBtn, "#e74c3c");
        restartBtn.setVisible(false);
        restartBtn.setOnAction(e -> resetView());

        titleBox = new HBox(10, titleLabel, restartBtn);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 10, 20));

        setupMap();
        setupLandmarkView();
        setupMediaView();
        setupButtons();
        setupQuizBox();
        createHotspots();

        // Main layout
        contentPane = new StackPane(mapPane, landmarkView, mediaView);
        contentPane.setAlignment(Pos.TOP_CENTER);

        // Reordered buttons to group related ones together
        HBox buttonBox = new HBox(10, playAudioBtn, viewImagesBtn, goToQuizBtn, playBtn, stopVideoBtn, endVideoBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox mainBox = new VBox(5, titleBox, contentPane, buttonBox, quizBox); // Add quizBox here
        mainBox.setAlignment(Pos.TOP_CENTER);
        mainBox.setPadding(new Insets(10));
        mainBox.setStyle("-fx-background-color: #ecf0f1;");

        return mainBox;
    }

    private void setupMap() {
        try {
            URL mapUrl = getClass().getResource("/images/map/maseru_map.jpg");
            if (mapUrl != null) {
                mapView = new ImageView(new Image(mapUrl.toString()));
                mapView.setPreserveRatio(true);
                mapView.setFitWidth(primaryStage.getWidth());
                mapPane = new Pane();
                mapPane.getChildren().add(mapView);
            } else {
                throw new RuntimeException("Map image not found");
            }
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            mapPane = new Pane(new Label("Map image not found"));
        }
    }

    private void setupLandmarkView() {
        landmarkView = new ImageView();
        landmarkView.setPreserveRatio(true);
        landmarkView.setFitWidth(700);
        landmarkView.setVisible(false);
    }

    private void setupMediaView() {
        mediaView = new MediaView();
        mediaView.setFitWidth(700);
        mediaView.setPreserveRatio(true);
        mediaView.setVisible(false);
    }

    private void setupButtons() {
        playBtn = new Button("PLAY VIDEO");
        styleButton(playBtn, "#2c3e50");
        playBtn.setVisible(false);
        playBtn.setOnAction(e -> playVideo());

        playAudioBtn = new Button("PLAY AUDIO");
        styleButton(playAudioBtn, "#3498db");
        playAudioBtn.setVisible(false);
        playAudioBtn.setOnAction(e -> toggleAudio());

        viewImagesBtn = new Button("VIEW IMAGES");
        styleButton(viewImagesBtn, "#9b59b6");
        viewImagesBtn.setVisible(false);
        viewImagesBtn.setOnAction(e -> showImageGallery());

        goToQuizBtn = new Button("GO TO QUIZ");
        styleButton(goToQuizBtn, "#27ae60");
        goToQuizBtn.setVisible(false);
        goToQuizBtn.setOnAction(e -> showQuiz());

        stopVideoBtn = new Button("PAUSE");
        styleButton(stopVideoBtn, "#e74c3c");
        stopVideoBtn.setVisible(false);
        stopVideoBtn.setOnAction(e -> stopVideo());

        endVideoBtn = new Button("END VIDEO");
        styleButton(endVideoBtn, "#e67e22");
        endVideoBtn.setVisible(false);
        endVideoBtn.setOnAction(e -> {
            stopVideo();
            showQuiz();
        });
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-font-size: 16px; -fx-padding: 8 15; -fx-background-color: " + color +
                "; -fx-text-fill: white; -fx-background-radius: 5;");
    }

    private void setupQuizBox() {
        quizBox = new VBox(10);
        quizBox.setAlignment(Pos.CENTER);
        quizBox.setPadding(new Insets(15));
        quizBox.setVisible(false);
        quizBox.setMaxWidth(600);
        quizBox.setStyle("-fx-background-color: rgba(255,255,255,0.9); " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #3498db; " +
                "-fx-border-radius: 10; " +
                "-fx-border-width: 2;");
    }

    private void createHotspots() {
        double[][] positions = {{485, 355}, {520, 300}, {722, 330}, {630, 460}};
        String[] colors = {"#e74c3c", "#3498db", "#2ecc71", "#f39c12"};

        for (int i = 0; i < landmarks.length; i++) {
            Circle spot = new Circle(10, Color.web(colors[i]));
            spot.setOpacity(0.7);
            spot.setCenterX(positions[i][0]);
            spot.setCenterY(positions[i][1]);
            hotspotCircles.add(spot);

            HBox hotspotLabel = createHotspotLabel(landmarks[i], i);
            hotspotLabel.setLayoutX(positions[i][0] + 20);
            hotspotLabel.setLayoutY(positions[i][1] - 10);
            hotspotLabels.add(hotspotLabel);

            final int index = i;
            spot.setOnMouseClicked(e -> toggleLandmark(index));
            hotspotLabel.setOnMouseClicked(e -> toggleLandmark(index));

            setHoverEffects(spot, hotspotLabel, colors[i]);

            mapPane.getChildren().addAll(spot, hotspotLabel);
        }
    }

    private HBox createHotspotLabel(String name, int index) {
        try {
            String firstImagePath = "/images/" + name.toLowerCase().replace(" ", "_") + "/1.jpg";
            URL imageUrl = getClass().getResource(firstImagePath);
            ImageView icon = new ImageView(imageUrl != null ? new Image(imageUrl.toString()) : null);
            icon.setFitWidth(20);
            icon.setFitHeight(20);

            Label label = new Label(name);
            label.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");

            return new HBox(5, icon, label);
        } catch (Exception e) {
            return new HBox(new Label(name));
        }
    }

    private void setHoverEffects(Circle spot, HBox label, String color) {
        spot.setOnMouseEntered(e -> {
            spot.setOpacity(1);
            label.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 5;");
        });

        spot.setOnMouseExited(e -> {
            if (!landmarkSelected || currentLandmark != hotspotCircles.indexOf(spot)) {
                spot.setOpacity(0.7);
                label.setStyle("");
            }
        });
    }

    private void toggleLandmark(int index) {
        if (landmarkSelected && currentLandmark == index) {
            resetView();
        } else {
            selectLandmark(index);
        }
    }

    private void selectLandmark(int index) {
        currentLandmark = index;
        landmarkSelected = true;

        // Reset media states
        videoPlaying = false;
        audioPlaying = false;

        primaryStage.setTitle("Maseru - " + landmarks[index]);
        titleLabel.setText(landmarks[index].toUpperCase());
        restartBtn.setVisible(true);

        for (int i = 0; i < hotspotCircles.size(); i++) {
            Circle spot = hotspotCircles.get(i);
            if (i == index) {
                spot.setOpacity(1);
                spot.setFill(Color.GREEN);
            } else {
                String[] colors = {"#e74c3c", "#3498db", "#2ecc71", "#f39c12"};
                spot.setFill(Color.web(colors[i]));
                spot.setOpacity(0.7);
            }
        }

        try {
            String imagePath = "/images/" + landmarks[index].toLowerCase().replace(" ", "_") + "/main.jpg";
            URL imageUrl = getClass().getResource(imagePath);
            if (imageUrl != null) {
                landmarkView.setImage(new Image(imageUrl.toString()));
                landmarkView.setVisible(true);
            } else {
                landmarkView.setVisible(false);
                System.err.println("Image not found: " + imagePath);
            }
        } catch (Exception e) {
            landmarkView.setVisible(false);
            System.err.println("Error loading image: " + e.getMessage());
        }

        mediaView.setVisible(false);
        stopVideo();
        stopAudio();

        playBtn.setVisible(true);
        playAudioBtn.setVisible(true);
        playAudioBtn.setText("PLAY AUDIO");
        viewImagesBtn.setVisible(true);
        goToQuizBtn.setVisible(true);
        stopVideoBtn.setVisible(false);
        stopVideoBtn.setText("STOP VIDEO");
        endVideoBtn.setVisible(false);
        quizBox.setVisible(false);
    }



    private void toggleAudio() {
        if (audioPlaying) {
            stopAudio();
        } else {
            playAudio();
        }
    }

    private void playAudio() {
        try {
            stopAudio();

            String audioPath = "/audios/" + landmarks[currentLandmark].toLowerCase().replace(" ", "_") + ".mp3";
            URL audioUrl = getClass().getResource(audioPath);
            if (audioUrl != null) {
                Media audio = new Media(audioUrl.toString());
                audioPlayer = new MediaPlayer(audio);
                audioPlayer.setOnEndOfMedia(() -> {
                    audioPlaying = false;
                    playAudioBtn.setText("PLAY AUDIO");
                });
                audioPlayer.play();
                audioPlaying = true;
                playAudioBtn.setText("STOP AUDIO");
            } else {
                showAlert("Audio Error", "Audio file not found: " + audioPath);
            }
        } catch (Exception e) {
            showAlert("Audio Error", "Could not play audio: " + e.getMessage());
        }
    }

    private void stopAudio() {
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer.dispose();
            audioPlayer = null;
        }
        audioPlaying = false;
        playAudioBtn.setText("PLAY AUDIO");
    }

    private void playVideo() {
        try {
            stopVideo();

            URL videoUrl = getClass().getResource("/videos/maseru_video.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toString());
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);

                mediaPlayer.setStartTime(Duration.seconds(videoSegments[currentLandmark][0]));
                mediaPlayer.setStopTime(Duration.seconds(videoSegments[currentLandmark][1]));
                mediaPlayer.setOnEndOfMedia(() -> {
                    videoPlaying = false;
                    showQuiz();
                });

                landmarkView.setVisible(false);
                mediaView.setVisible(true);
                playBtn.setVisible(false);
                playAudioBtn.setVisible(false);
                viewImagesBtn.setVisible(false);
                goToQuizBtn.setVisible(false);
                stopVideoBtn.setVisible(true);
                endVideoBtn.setVisible(true);
                quizBox.setVisible(false);

                videoPlaying = true;
                mediaPlayer.play();
            } else {
                showAlert("Video Error", "Video file not found");
                showQuiz();
            }
        } catch (Exception e) {
            showAlert("Video Error", "Could not play video: " + e.getMessage());
            showQuiz();
        }
    }
    private void stopVideo() {
        if (mediaPlayer != null) {
            if (videoPlaying) {
                // Pause the video
                mediaPlayer.pause();
                videoPlaying = false;
                stopVideoBtn.setText("CONTINUE VIDEO");
            } else {
                // Continue playing
                mediaPlayer.play();
                videoPlaying = true;
                stopVideoBtn.setText("PAUSE VIDEO");
            }
        }
    }

    private void showImageGallery() {
        Stage galleryStage = new Stage();
        galleryStage.setTitle(landmarks[currentLandmark] + " - Image Gallery");

        List<Image> images = new ArrayList<>();
        try {
            String dirPath = "/images/" + landmarks[currentLandmark].toLowerCase().replace(" ", "_");
            int imgCount = 1;
            while (true) {
                String imgPath = dirPath + "/" + imgCount + ".jpg";
                URL imgUrl = getClass().getResource(imgPath);
                if (imgUrl == null) break;
                images.add(new Image(imgUrl.toString()));
                imgCount++;
            }
        } catch (Exception e) {
            showAlert("Gallery Error", "Could not load images: " + e.getMessage());
            return;
        }

        if (images.isEmpty()) {
            showAlert("Gallery", "No images available for this location");
            return;
        }

        ImageView imageView = new ImageView(images.get(0));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);

        Button prevBtn = new Button("Previous");
        Button nextBtn = new Button("Next");
        Label countLabel = new Label("1/" + images.size());

        AtomicInteger currentIndex = new AtomicInteger(0);

        prevBtn.setOnAction(e -> {
            int newIndex = currentIndex.get() > 0 ? currentIndex.get() - 1 : images.size() - 1;
            currentIndex.set(newIndex);
            imageView.setImage(images.get(newIndex));
            countLabel.setText((newIndex+1) + "/" + images.size());
        });

        nextBtn.setOnAction(e -> {
            int newIndex = currentIndex.get() < images.size() - 1 ? currentIndex.get() + 1 : 0;
            currentIndex.set(newIndex);
            imageView.setImage(images.get(newIndex));
            countLabel.setText((newIndex+1) + "/" + images.size());
        });

        HBox controls = new HBox(20, prevBtn, countLabel, nextBtn);
        controls.setAlignment(Pos.CENTER);

        VBox galleryLayout = new VBox(20, imageView, controls);
        galleryLayout.setAlignment(Pos.CENTER);
        galleryLayout.setPadding(new Insets(20));

        galleryStage.setScene(new Scene(galleryLayout, 800, 600));
        galleryStage.show();
    }


    private void showQuiz() {
        // ===== SOUND SETTINGS ===== (Adjust these values as needed)
        // Correct answer sound
        final int CORRECT_FREQ = 880;    // Frequency in Hz (higher = higher pitch)
        final int CORRECT_DURATION = 200; // Duration in milliseconds
        final double CORRECT_VOL = 0.7;   // Volume (0.1 to 0.9)

        // Wrong answer sound
        final int WRONG_FREQ = 440;
        final int WRONG_DURATION = 400;
        final double WRONG_VOL = 0.5;

        // Simple beep player
        class BeepPlayer {
            void playBeep(int freq, int duration, double vol) {
                new Thread(() -> {
                    try {
                        byte[] buf = new byte[1];
                        AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                        sdl.open(af);
                        sdl.start();

                        for (int i = 0; i < duration * 44.1; i++) {
                            double angle = i / (44100.0 / freq) * 2.0 * Math.PI;
                            buf[0] = (byte)(Math.sin(angle) * 127.0 * vol);
                            sdl.write(buf, 0, 1);
                        }
                        sdl.drain();
                        sdl.close();
                    } catch (Exception ex) {
                        Toolkit.getDefaultToolkit().beep(); // Fallback
                    }
                }).start();
            }
        }

        BeepPlayer beeper = new BeepPlayer();

        // Show the map background
        try {
            URL mapUrl = getClass().getResource("/images/map/maseru_map.jpg");
            if (mapUrl != null) {
                Image mapImage = new Image(mapUrl.toString());
                mediaView.setMediaPlayer(null);
                mediaView.setVisible(true);
                mediaView.setFitWidth(700);
                mediaView.setPreserveRatio(true);
            }
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
        }

        // Setup quiz UI
        landmarkView.setVisible(false);
        quizBox.getChildren().clear();

        Label question = new Label(quizData[currentLandmark][0]);
        question.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        ToggleGroup options = new ToggleGroup();
        VBox optionsBox = new VBox(5);
        for (int i = 1; i <= 3; i++) {
            RadioButton option = new RadioButton(quizData[currentLandmark][i]);
            option.setToggleGroup(options);
            option.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50;");
            optionsBox.getChildren().add(option);
        }

        Button submit = new Button("SUBMIT ANSWER");
        styleButton(submit, "#27ae60");

        Label result = new Label();
        result.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        AtomicBoolean answered = new AtomicBoolean(false);

        // Handle answer submission
        submit.setOnAction(e -> {
            if (answered.get()) return;

            RadioButton selected = (RadioButton)options.getSelectedToggle();
            if (selected != null) {
                answered.set(true);
                submit.setDisable(true);

                int selectedIndex = options.getToggles().indexOf(selected) + 1;
                if (String.valueOf(selectedIndex).equals(quizData[currentLandmark][4])) {
                    result.setText("Correct! Well done.");
                    result.setStyle("-fx-text-fill: #27ae60;-fx-font-size: 46px; -fx-font-weight: bold;");
                    beeper.playBeep(CORRECT_FREQ, CORRECT_DURATION, CORRECT_VOL);
                } else {
                    int correctIndex = Integer.parseInt(quizData[currentLandmark][4]);
                    result.setText("Incorrect. Correct answer: " + quizData[currentLandmark][correctIndex]);
                    result.setStyle("-fx-text-fill: #e74c3c;-fx-font-size: 26px");
                    beeper.playBeep(WRONG_FREQ, WRONG_DURATION, WRONG_VOL);
                }

                // Disable all options after answering
                options.getToggles().forEach(toggle -> ((RadioButton)toggle).setDisable(true));

                // Return to map after delay
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> resetView());
                            }
                        },
                        5000
                );
            } else {
                result.setText("Please select an answer!");
                result.setStyle("-fx-text-fill: #e74c3c;-fx-font-size: 20px; -fx-font-weight: bold;");
            }
        });

        // Display quiz
        quizBox.getChildren().addAll(question, optionsBox, submit, result);
        quizBox.setVisible(true);

        // Position quiz box
        if (!contentPane.getChildren().contains(quizBox)) {
            contentPane.getChildren().add(quizBox);
        }
        StackPane.setAlignment(quizBox, Pos.CENTER);
        StackPane.setMargin(quizBox, new Insets(20));

        // Hide other buttons
        playBtn.setVisible(false);
        playAudioBtn.setVisible(false);
        viewImagesBtn.setVisible(false);
        goToQuizBtn.setVisible(false);
        stopVideoBtn.setVisible(false);
        endVideoBtn.setVisible(false);
    }
    private void resetView() {
        // Stop all media first
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer.dispose();
            audioPlayer = null;
        }

        landmarkSelected = false;
        currentLandmark = -1;
        videoPlaying = false;
        audioPlaying = false;

        primaryStage.setTitle("Maseru Virtual Tour Guide");
        titleLabel.setText("MASERU");
        restartBtn.setVisible(false);

        String[] colors = {"#e74c3c", "#3498db", "#2ecc71", "#f39c12"};
        for (int i = 0; i < hotspotCircles.size(); i++) {
            Circle spot = hotspotCircles.get(i);
            spot.setFill(Color.web(colors[i]));
            spot.setOpacity(0.7);
        }

        landmarkView.setVisible(false);
        mediaView.setVisible(false);
        playBtn.setVisible(false);
        playAudioBtn.setVisible(false);
        playAudioBtn.setText("PLAY AUDIO");
        viewImagesBtn.setVisible(false);
        goToQuizBtn.setVisible(false);
        stopVideoBtn.setVisible(false);
        stopVideoBtn.setText("STOP VIDEO");
        endVideoBtn.setVisible(false);
        quizBox.setVisible(false);

        // Simply remove quizBox from contentPane - don't add it back to main layout
        contentPane.getChildren().remove(quizBox);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}