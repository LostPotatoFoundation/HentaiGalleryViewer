package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import lostpotatofoundation.hentaigalleryviewer.Configuration;

import javax.imageio.ImageIO;
import java.io.File;

import static lostpotatofoundation.hentaigalleryviewer.gui.MainController.*;

public class GalleryController {
    public ImageView galleryView;
    public TextField galleryTitle;
    public Pane pane;
    byte id = -1;
    String image = "";

    void update() {
        System.out.println("Updated id " + id);
        try {
            if (galleryIndex.size() > id) {
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

    void start() {
//        Thread main = new Thread(() -> {
//            while (id > -1) {
//            }
//        });
//        main.setDaemon(true);
//        main.start();
    }

    public void clicked() {
        int galleryClicked = id + MainController.listOffset;
        linkStack.add(galleryIndex.get(galleryClicked).url);
    }
    
    public void mouseScrollEvent(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0) //scroll down
            MainController.listOffset += 1;
        else if (scrollEvent.getDeltaY() > 0 && MainController.listOffset > 0) //scroll up
            MainController.listOffset -= 1;
        MainGui.instance.updateAll();
    }

    public void downloadGallery() {
        if (!linkStack.contains(galleryIndex.get((id + MainController.listOffset)).url))
            linkStack.add(galleryIndex.get((id + MainController.listOffset)).url);
    }

    public void viewGallery() {

    }
}
