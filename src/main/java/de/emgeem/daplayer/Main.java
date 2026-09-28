package de.emgeem.daplayer;

import org.bytedeco.ffmpeg.global.avutil;

import de.emgeem.daplayer.ui.DAPlayerView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        avutil.av_log_set_level(avutil.AV_LOG_ERROR);

        DAPlayerView view = new DAPlayerView();

        Scene scene = new Scene(view, 1200, 675);
        scene.setFill(Color.TRANSPARENT);
        
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {launch(args);}
}
