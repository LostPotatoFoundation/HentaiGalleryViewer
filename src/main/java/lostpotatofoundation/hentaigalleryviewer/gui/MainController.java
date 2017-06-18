package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import lostpotatofoundation.hentaigalleryviewer.Configuration;
import lostpotatofoundation.hentaigalleryviewer.GalleryDownloadThread;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MainController {
    public ImageView galleryView;
    public Pane pane;
    public Button downloadGalleryButton;
    public Button viewGalleryButton;
    public TextField searchBox;
    public ProgressBar progressBar;

    private Stack<String> linkStack = new Stack<>();
    private static File cacheDir = new File(System.getProperty("user.dir"), "cache");

    private static volatile int listOffset = 0, pagesIndexed = 0;
    private GalleryDownloadThread downloader;
    private static boolean running = false;
    private static volatile HashMap<String, Pane> panes = new HashMap<>();
    private static volatile HashMap<Pane, ImageView> views = new HashMap<>();

    private boolean searchPerformed = false;

    private void start() {
        running = true;
        Thread main = new Thread(() -> {
            while (running) {
                try {
                    if (searchPerformed && galleryIndex.size() > 0) {
                        panes.forEach((id, pane) -> {
                            galleryData d = galleryIndex.get(listOffset + getPaneNumericalId(id));
//                            System.out.println("Setting pane " + id + "  " + pane.getId() + " with " + d.imageName);
                            try {
                                File fimage = new File(cacheDir, d.imageName + ".png");
                                views.get(pane).setImage(SwingFXUtils.toFXImage(ImageIO.read(fimage), null));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (downloader != null && !downloader.isDone()) {
                    if (progressBar == null) continue;
                    progressBar.setProgress((downloader.getDownloadProgress() + downloader.getCompressionProgress()) / 2.0D);
                }

                if (progressBar != null) {
                    progressBar.setProgress(0.0D);
                }
                if (!linkStack.empty())
                    startDownload(linkStack.pop());
            }
        });
        main.setDaemon(true);
        main.start();
    }

    private void startDownload(String link) {
        downloader = new GalleryDownloadThread(link);
        downloader.start();
    }

    private Integer getPaneNumericalId(String str) {
        return Integer.parseInt(str.split(":")[0]) + Integer.parseInt(str.split(":")[1]);
    }

    public void clicked() {
        int galleryClicked = getPaneNumericalId(pane.getId()) + listOffset;
        System.out.println(galleryIndex.get(galleryClicked).title);
        linkStack.add(galleryIndex.get(galleryClicked).url);
    }

    private String searchURL = Configuration.defaultSearchURL;
    public void keyPressEvent(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            if (!running) start();
            searchPerformed = true;
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

    private void doSearch() {
        try {
            URL url = new URL(searchURL.concat("&page=" + pagesIndexed));
            System.out.println(searchURL);
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
                Matcher galleryLinkMatcher = Pattern.compile("https?://exhentai\\.org/g/[^\"]+").matcher(line),
                        galleryTitleMatcher = Pattern.compile("(?=https?://exhentai\\.org/g/[^\"]+)[^<]+").matcher(line),
                        galleryPreviewMatcher = Pattern.compile("https?://exhentai\\.org/t/[^\"]+").matcher(line);
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
                    String g = galleryPreviewMatcher.group();
                    previewImage.add(g);
                    downloadImage(g);
                }
                if (previewImage.size() > 0)
                    System.out.println(previewImage.toString());
            }
            for (int i = 0; i < link.size(); i++) {
                galleryIndex.add(new galleryData(link.get(i), previewImage.get(i), title.get(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadImage(String urlString) throws Exception {
        if (urlString.length() < 12) return;
        System.out.println("Downloading from " + urlString);
        Matcher m = Pattern.compile("(?!/)[^./]{9,}").matcher(urlString);
        m.find();
        System.out.println(m.group());
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Cookie", Configuration.getCookies());


            File imageFile = new File(cacheDir, m.group() +".png");
            if (!imageFile.createNewFile()) return;

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            byte[] b = new byte[16384];
            int length;
            while ((length = inputStream.read(b)) != -1) {
                outputStream.write(b, 0, length);
            }
            inputStream.close();
            outputStream.close();
        } catch (SocketException e) {
            System.out.println(m.group(0));
            System.out.println(e.getMessage());
            File f = new File(cacheDir, m.group(0) + ".png");
            if (f.exists()) f.delete();
            downloadImage(urlString);
        }
        File f = new File(cacheDir, m.group(0) + ".png");
        if (!f.canRead() || !f.exists() || f.getTotalSpace() == 0) {
            f.delete();
            downloadImage(urlString);
        }
    }


    public void mouseScrollEvent(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0) {
            listOffset += 1;
            if (galleryIndex.size() - (listOffset + panes.size()) < Configuration.buffer) {
                doSearch();
                if (galleryIndex.size() - (listOffset + panes.size()) == 0)
                    listOffset -= 1;
            }
            //scroll down
        } else if (scrollEvent.getDeltaY() > 0) {
            if (listOffset > 0) listOffset -= 1;
            //scroll up
        }
    }

    private static volatile LinkedList<galleryData> galleryIndex = new LinkedList<>();

    public void mouseMovedEvent() {
        if (!panes.containsKey(pane.getId())) {
            panes.put(pane.getId(), pane);
            views.put(pane, galleryView);
            System.out.println("Pane ID = " + getPaneNumericalId(pane.getId()) + " added to list.");
        }
    }

    public void downloadGallery() {
        linkStack.add(galleryIndex.get(getPaneNumericalId(pane.getId())).url);
    }

    public void viewGallery() {

    }

    private class galleryData {
        String url;
        String image;
        String title;
        String imageName;

        galleryData(String u, String i, String t) {
            url = u;
            image = i;
            title = t;
            Matcher m = Pattern.compile("(?!/)[^./]{9,}").matcher(i);
            m.find();
//                System.out.println(m.group(0) + " from " + i);
            imageName = m.group();
        }
    }
}
