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
import java.util.LinkedList;

@SuppressWarnings("all")
public class GalleryViewController {
    public ProgressBar progress;
    public AnchorPane pane;
    public ImageView imagePort;
    public Button saveButton;

    boolean dead;
    Stage stage = null;

    LinkedList<File> images = new LinkedList<>();
    LinkedList<String> links = new LinkedList<>();
    LinkedList<String> linksBuffer = new LinkedList<>();
    static int offset = 0;
    static GalleryDownloadThread thread = null;

    public void display() {
        try {
            if (!dead && links.size() == 0) {
//                SwingWorker w = new SwingWorker() {
//                    @Override
//                    protected Object doInBackground() throws Exception {
//                        return null;
//                    }
//                }
                Thread t = new Thread(() -> {
                    if (links.size() == 0)
                        links = thread.getGalleryForViewing();
                    while (!dead) {
                        if (links.size() != 0 && linksBuffer.size() == 0 && images.size() == 0)
                            linksBuffer = links;
                        try {
                            if (images.size() > offset)
                                imagePort.setImage(SwingFXUtils.toFXImage(ImageIO.read(images.get(offset)), null));
                            progress.setProgress((double) offset + 1D / (double) links.size());
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            if (Configuration.debug)
                                e.printStackTrace();
                        }

                        if (!images.contains(thread.getImageFile()) && thread.getImageFile() != null)
                            images.add(thread.getImageFile());
//                        progress.setProgress(thread.getDownloadProgress());
                        if (Configuration.preBuffer && offset + Configuration.imageBuffer > images.size() && links.size() > 0)
                            thread.getImage(links.get(images.size()), images.size());
                        else if (!Configuration.preBuffer && linksBuffer.size() != 0 && (images.contains(thread.getImageFile()) || images.size() == 0))
                            thread.getImage(linksBuffer.pop(), linksBuffer.size() + 1);
                    }
                });
                t.setDaemon(true);
                t.start();
            }
/*
            if (images.size() > 0) {
                Collections.sort(images);
            }*/
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
