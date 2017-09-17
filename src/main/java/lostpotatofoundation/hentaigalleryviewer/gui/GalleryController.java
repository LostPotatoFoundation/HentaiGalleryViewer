package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lostpotatofoundation.hentaigalleryviewer.Configuration;
import lostpotatofoundation.hentaigalleryviewer.GalleryDownloadThread;

import javax.imageio.ImageIO;
import java.io.File;

import static lostpotatofoundation.hentaigalleryviewer.gui.MainController.*;

@SuppressWarnings("all")
public class GalleryController {
    public ImageView galleryView;
    public TextField galleryTitle;
    public Pane pane;
    byte id = -1;
    private String image = "";

    void update() {
//        System.out.println("Updated id " + id);
        try {
            if (galleryIndex.size() > id + listOffset && id > -1) {
                MainController.galleryData d = galleryIndex.get(listOffset + id);
                if (!image.equals(d.title)) {
                    galleryTitle.setText(d.title);
                    File fimage = new File(cacheDir, d.imageName + ".png");
                    galleryView.setImage(SwingFXUtils.toFXImage(ImageIO.read(fimage), null));
                }
            }
        } catch (Exception e) {
            System.out.println("start " + id + " " + e.getMessage());
            if (Configuration.debug)
                e.printStackTrace();
        }
    }

    public void clicked() {
        int galleryClicked = id + MainController.listOffset;
        linkStack.add(galleryIndex.get(galleryClicked).url);
    }

    public void mouseScrollEvent(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0 && MainController.galleryIndex.size() > 3 + (MainController.listOffset + ((MainGui.extraDownloaders_W + 1) * (MainGui.extraDownloaders_H + 1)))) {
            MainController.listOffset += (MainGui.extraDownloaders_W + 1);
            MainGui.instance.updateAll();
        } else if (scrollEvent.getDeltaY() > 0 && MainController.listOffset > 0) {
            MainController.listOffset -= (MainGui.extraDownloaders_W + 1);
            if (MainController.listOffset < 0)
                MainController.listOffset = 0;
            MainGui.instance.updateAll();
        }

    }

    public void downloadGallery() {
        if (!linkStack.contains(galleryIndex.get((id + MainController.listOffset)).url))
            linkStack.add(galleryIndex.get((id + MainController.listOffset)).url);
    }

    public void viewGallery() {
        FXMLLoader viewLoader = new FXMLLoader(getClass().getResource("/lostpotatofoundation/hentaigalleryviewer/galleryViewPanel.fxml"));

        Stage primaryStage = new Stage();
        primaryStage.setTitle(galleryTitle.getText());

        try {
            Pane pane = viewLoader.load();
            pane.setId(galleryTitle.getText());

            primaryStage.setScene(new Scene(pane));
            primaryStage.show();

            GalleryViewController c = viewLoader.getController();
            String url = galleryIndex.get((id + MainController.listOffset)).url;
            GalleryDownloadThread thread = new GalleryDownloadThread(url);
            c.thread = thread;
            c.stage = primaryStage;
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    c.dead = true;
                    thread.deleteLoseArchive();
                }
            });
            c.display();
        } catch (Exception e) {
            System.out.println("viewGallery " + e.getMessage());
            if (Configuration.debug) e.printStackTrace();
        }
    }
}
