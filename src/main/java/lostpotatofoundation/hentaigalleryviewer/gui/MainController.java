package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import lostpotatofoundation.hentaigalleryviewer.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {
    public ImageView galleryView;
    public Pane pane;
    public Button downloadGallery;
    public Button viewGallery;
    public TextField searchBox;

    private Stack<String> linkStack = new Stack<>();

    private int listOffset = 0, pagesIndexed = 0;

    public void clicked(MouseEvent mouseEvent) {
        int galleryClicked = Integer.parseInt(pane.getId().split(":")[0]) + Integer.parseInt(pane.getId().split(":")[1]);
        System.out.println(galleryIndex.get(galleryClicked).title);
        linkStack.add(galleryIndex.get(galleryClicked).url.getPath());
    }

    String searchURL = Configuration.defaultSearchURL;
    public void keyPressEvent(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            //TODO search
            pagesIndexed = 0;
            searchURL = Configuration.defaultSearchURL;
            listOffset = 0;

            String[] searchArgs = searchBox.getText().split(" ");
            //https://exhentai.org/?f_doujinshi=1&f_manga=1&f_artistcg=0&f_gamecg=0&f_western=0&f_non-h=0&f_imageset=0&f_cosplay=0&f_asianporn=0&f_misc=0&f_search=english+-yaoi+tentacle+loli&f_apply=Apply+Filter
            searchURL = searchURL.concat("&f_search=");
            for (String arg : searchArgs) {
                searchURL = searchURL.concat(arg + "+");
            }
            searchURL = searchURL.concat("&f_apply=Apply+Filter");
            doSearch();
        }
    }

    public void doSearch() {
        try {
            URL url = new URL(searchURL.concat("&page=" + pagesIndexed));
            pagesIndexed += 1;

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Cookie", Configuration.getCookies());

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));


            LinkedList<String> lineList = new LinkedList<>();
            Stream<String> lines = reader.lines();
            lineList.addAll(lines.collect(Collectors.toList()));
            reader.close();
            inputStream.close();
            connection.disconnect();

            LinkedList<String> link = new LinkedList<>(), title = new LinkedList<>(), previewImage = new LinkedList<>();
            for (String line : lineList) {
                Matcher galleryLinkMatcher = Pattern.compile("https?:\\/\\/exhentai\\.org\\/g\\/[^\"]+").matcher(line),
                        galleryTitleMatcher = Pattern.compile("(?=https?:\\/\\/exhentai\\.org\\/g\\/[^\"]+)[^<]+").matcher(line),
                        galleryPreviewMatcher = Pattern.compile("https?:\\/\\/exhentai\\.org\\/t\\/[^\"]+").matcher(line);
                while (galleryLinkMatcher.find()) {
                    String g = galleryLinkMatcher.group();
                    if (!link.contains(g))
                        link.add(g);
                }
                while (galleryTitleMatcher.find()) {
                    String g = galleryTitleMatcher.group();
                    if (g.split(">").length > 1)
                        title.add(g.split(">")[1]);
                }
                while (galleryPreviewMatcher.find()) {
                    previewImage.add(galleryPreviewMatcher.group());
                }
            }
            for (int i = 0; i < link.size(); i++) {
                galleryIndex.add(new galleryData(link.get(i), previewImage.get(i), title.get(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mouseScrollEvent(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0) {
            listOffset += 1;
            if (galleryIndex.size() - listOffset < Configuration.buffer) {

            }
            //scroll down
        } else if (scrollEvent.getDeltaY() > 0) {
            if (listOffset > 0) listOffset -= 1;
            //scroll up
        }
    }

    public static volatile LinkedList<galleryData> galleryIndex = new LinkedList<>();
    private class galleryData {
        public URL url;
        public URL image;
        public String title;

        public galleryData(String u, String i, String t) {
            try {
                url = new URL(u);
                image = new URL(i);
                title = t;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
