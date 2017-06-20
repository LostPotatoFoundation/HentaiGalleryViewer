package lostpotatofoundation.hentaigalleryviewer.gui;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lostpotatofoundation.hentaigalleryviewer.Configuration;
import lostpotatofoundation.hentaigalleryviewer.GalleryDownloadThread;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MainController {
    public TextField searchBox;
    public ProgressBar progressBar;

    private static final String TITLE_PARSE_PATTERN = "(\\[.*?]|\\{.*?}|\\(.*?\\))|(=.*=|~.*~)|([^a-z,A-Z\\s\\-~|\\d_])|(\\s{2,}|\\s+\\.)";

    static volatile Stack<String> linkStack = new Stack<>();
    static volatile int listOffset = 0;
    private static volatile int pagesIndexed = 0;

    private static volatile GalleryDownloadThread downloader;
    private static volatile boolean running;

    static final File cacheDir = new File(System.getProperty("user.dir"), "cache");

    private synchronized void start() {
        running = true;
        Thread main = new Thread(() -> {
            while (running) {
                while (downloader != null && !downloader.isDone()) {
                    if (progressBar == null) continue;
                    progressBar.setProgress((downloader.getDownloadProgress() + downloader.getCompressionProgress()) / 2.0D);
                }

                if (progressBar != null)
                    progressBar.setProgress(0.0D);

                if (!linkStack.empty())
                    startDownload(linkStack.pop());

                if (galleryIndex.size() <= (MainGui.panes2d.size() + listOffset + Configuration.buffer))
                    doSearch();
            }
        });
        main.setDaemon(true);
        main.start();
    }

    private void startDownload(String link) {
        downloader = new GalleryDownloadThread(link);
        downloader.start();
    }

    private static volatile String searchURL = Configuration.defaultSearchURL;
    public void keyPressEvent(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            pagesIndexed = 0;
            searchURL = Configuration.defaultSearchURL;
            listOffset = 0;
            galleryIndex = new LinkedList<>();

            String[] searchArgs = searchBox.getText().split(" ");
            searchURL = searchURL.concat("&f_search=");
            for (String arg : searchArgs)
                searchURL = searchURL.concat(arg + "+");
            searchURL = searchURL.concat("&f_apply=Apply+Filter");
            doSearch();
        }
    }

    private void doSearch() {
        try {
            URL url = new URL(searchURL.concat("&page=" + pagesIndexed));
            System.out.println("Searching up " + searchURL);
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
            }
            for (int i = 0; i < link.size(); i++) {
                galleryIndex.add(new galleryData(link.get(i), previewImage.get(i), title.get(i)));
            }
        } catch (Exception e) {
            System.out.println("doSearch = " + e.getMessage());
            if (Configuration.debug)
                e.printStackTrace();
        }
        if (!running) start();
        MainGui.instance.updateAll();
    }

    private void downloadImage(String urlString) throws Exception {
        if (urlString.length() < 12) return;
        Matcher m = Pattern.compile("(?!/)[^./]{9,}").matcher(urlString);
        m.find();
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
            System.out.println("downloadImage " + e.getMessage());
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

    static volatile LinkedList<galleryData> galleryIndex = new LinkedList<>();

    class galleryData {
        String url;
        String image;
        String title;
        String imageName;

        galleryData(String u, String i, String t) {
            url = u;
            image = i;
            title = t.replaceAll(TITLE_PARSE_PATTERN, "");
            Matcher m = Pattern.compile("(?!/)[^./]{9,}").matcher(i);
            m.find();
//                System.out.println(m.group(0) + " from " + i);
            imageName = m.group();
        }
    }
}
