package de.emgeem.daplayer;

import org.bytedeco.ffmpeg.global.avutil;

import de.emgeem.daplayer.ui.DAPlayerView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    private DAPlayerView view;

    @Override
    public void start(Stage stage) {

        try {
            avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        } catch (Throwable t) {
            System.err.println("FFmpeg nicht verfügbar - WMA/FLAC/OGG/M4A/Streams sind deaktiviert: " + t.getMessage());
        }
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.WARNING);

        view = new DAPlayerView();

        Scene scene = new Scene(view, 1200, 675);
        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("DAPlayer");
        var icon = Main.class.getResourceAsStream("/icons/DDPlayer_icon16.png");
        if (icon != null) {stage.getIcons().add(new Image(icon));}
        var logo = Main.class.getResourceAsStream("/img/DAP_logo_01.png");
        if (logo != null) {stage.getIcons().add(new Image(logo));}
        stage.setOnCloseRequest(e -> view.shutdown());
        stage.setScene(scene);
        stage.show();
        view.requestFocus();
    }

    @Override
    public void stop() {
        if (view != null) {view.shutdown();}
    }

    public static void main(String[] args) {launch(args);}
}
