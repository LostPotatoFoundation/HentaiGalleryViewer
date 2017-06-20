package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import lostpotatofoundation.hentaigalleryviewer.Configuration;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

import static lostpotatofoundation.hentaigalleryviewer.gui.MainController.*;

public class GalleryController {
    public ImageView galleryView;
    public TextField galleryTitle;
    public Pane pane;
    byte id = -1;

    void start() {
        Thread main = new Thread(() -> {
            while (id > -1) {
                try {
                    if (galleryIndex.size() > id) {
                        try {
                            MainController.galleryData d = galleryIndex.get(listOffset + id);
                            galleryTitle.setText(d.title);
                            File fimage = new File(cacheDir, d.imageName + ".png");
                            galleryView.setImage(SwingFXUtils.toFXImage(ImageIO.read(fimage), null));
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                            if (Configuration.debug)
                                e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    if (Configuration.debug)
                        e.printStackTrace();
                }
            }
        });
        main.setDaemon(true);
        main.start();
    }

    public void clicked() {
        int galleryClicked = id + MainController.listOffset;
        System.out.println(galleryIndex.get(galleryClicked).title);
        linkStack.add(galleryIndex.get(galleryClicked).url);
    }
    
    public void mouseScrollEvent(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0) {
            MainController.listOffset += 1;
            //scroll down
        } else if (scrollEvent.getDeltaY() > 0) {
            if (MainController.listOffset > 0) MainController.listOffset -= 1;
            //scroll up
        }
        if (MainController.listOffset < 0) MainController.listOffset = 0;

        if (MainController.listOffset + id >= MainController.galleryIndex.size())
            MainController.listOffset = MainController.listOffset + id;
    }

    public void downloadGallery() {
        if (!linkStack.contains(galleryIndex.get((id + MainController.listOffset)).url))
            linkStack.add(galleryIndex.get((id + MainController.listOffset)).url);
    }

    public void viewGallery() {

    }
}
