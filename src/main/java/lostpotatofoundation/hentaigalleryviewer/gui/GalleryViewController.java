package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lostpotatofoundation.hentaigalleryviewer.Configuration;
import lostpotatofoundation.hentaigalleryviewer.GalleryDownloadThread;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;

@SuppressWarnings("all")
public class GalleryViewController {
    public ProgressBar progress;
    public AnchorPane pane;
    public ImageView imagePort;
    public Button saveButton;

    Stage stage = null;

    LinkedList<File> images = new LinkedList<>();
    static int offset = 0;
    static GalleryDownloadThread thread = null;

    public void display() {
        try {
            if (!thread.isDone()) {
                Thread t = new Thread(() -> {
                    while (!thread.isDone()) {
                        if (!images.contains(thread.getImageFile()) && thread.getImageFile() != null)
                            images.add(thread.getImageFile());
                        progress.setProgress(thread.getDownloadProgress());
                    }
                });
                t.setDaemon(true);
                t.start();
                thread.getGalleryForViewing();
            }

            if (thread.isDone() && images.size() > 0) {
                Collections.sort(images);
                imagePort.setImage(SwingFXUtils.toFXImage(ImageIO.read(images.get(offset)), null));
                progress.setProgress((double)offset/(double)images.size());
            }
        } catch (Exception e) {
            System.out.println("display " + e.getMessage());
            if (Configuration.debug)
                e.printStackTrace();
        }
    }

    public void saveGallery(ActionEvent actionEvent) {
        offset = 0;
        thread.processAndSaveGallery();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.RIGHT)) {
            if (images.size() > offset)
                offset += 1;
        } else if (keyEvent.getCode().equals(KeyCode.RIGHT)) {
            if (offset > 0)
                offset -= 1;
        }
        display();
    }

    public void onClicked(MouseEvent mouseEvent) {
        if (images.size() > offset)
            offset += 1;
        display();
    }

    public void onScroll(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0 && offset < images.size() - 1) //scroll down
            offset += 1;
        else if (scrollEvent.getDeltaY() > 0 && offset > 0) //scroll up
            offset -= 1;
        display();
    }

    public void onSwipeL() {
        if (images.size() > offset)
            offset += 1;
        display();
    }

    public void onSwipeR() {
        if (offset > 0)
            offset -= 1;
        display();
    }
}
