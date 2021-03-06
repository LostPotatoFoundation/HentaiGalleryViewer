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
    public TextField pageCount;
    public TextField downloadQueue;
    public TextField pageIndex;

    public static MainController instance;

    private static final String TITLE_PARSE_PATTERN = "(\\[.*?]|\\{.*?}|\\(.*?\\))|(=.*=|~.*~)|([^a-z,A-Z\\s\\-~|\\d_])|(\\s{2,}|\\s+\\.)";

    static volatile Stack<String> linkStack = new Stack<>();
    static volatile int listOffset = 0;
    private static volatile int pagesIndexed = 0;

    private static volatile GalleryDownloadThread downloader;
    private static volatile boolean running;

    static final File cacheDir = new File(System.getProperty("user.dir"), "cache");

    private synchronized void start() {
        instance = this;
        running = true;

        Thread main = new Thread(() -> {
            Thread background = new Thread(() -> {
                while (running) {
                    if (downloader != null && !downloader.isDone() && progressBar != null && progressBar.getProgress() != (downloader.getDownloadProgress() + downloader.getCompressionProgress()) / 2.0D)
                        progressBar.setProgress(downloader.getProgress());

                    if (galleryIndex.size() <= (MainGui.panes2d.size() + listOffset + Configuration.buffer))
                        MainController.instance.doSearch();
                }
            });

            background.start();
            while (running) {
                if (pageCount != null && pageIndex != null && !pageCount.getText().equalsIgnoreCase("" + listOffset)) {
                    pageCount.clear();
                    pageCount.setText("" + listOffset);
                    pageIndex.clear();
                    pageIndex.setText("/" + galleryIndex.size());
                }

                if (downloadQueue != null && !downloadQueue.getText().equalsIgnoreCase("" + linkStack.size())) {
                    downloadQueue.clear();
                    downloadQueue.setText("" + linkStack.size());
                }

                if (progressBar != null && progressBar.getProgress() != 0.0D)
                    progressBar.setProgress(0.0D);

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

    private static volatile String searchURL = Configuration.defaultSearchURL;
    public void keyPressEvent(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            fail = false;
            pagesIndexed = 0;
            searchURL = Configuration.defaultSearchURL;
            listOffset = 0;
            galleryIndex = new LinkedList<>();

            cacheDir.delete();
            cacheDir.mkdirs();

            String[] searchArgs = searchBox.getText().split(" ");
            searchURL = searchURL.concat("&f_search=");
            for (String arg : searchArgs)
                searchURL = searchURL.concat(arg + "+");
            searchURL = searchURL.concat("&f_apply=Apply+Filter");
            doSearch();
        }
    }

    private boolean fail;

    public void doSearch() {
        if (fail) return;
        try {
            int galSize = galleryIndex.size();
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

            lineList.forEach(System.out::println);

            LinkedList<String> link = new LinkedList<>(), title = new LinkedList<>(), previewImage = new LinkedList<>();
            boolean firstTime = true;
            for (String line : lineList) {
                Matcher galleryLinkMatcher = Pattern.compile("https?://exhentai\\.org/g/[^\"]+").matcher(line),
                        galleryTitleMatcher = Pattern.compile("(?:alt=\")[^\"]{2,}").matcher(line),
                        galleryPreviewMatcher = Pattern.compile("https?://exhentai\\.org/t/[a-zA-Z0-9_\\-/.]+").matcher(line),
                        galleryPreviewMatcher2 = Pattern.compile("inits~exhentai\\.org~t/[a-zA-Z0-9_\\-/.]+").matcher(line);

                while (galleryLinkMatcher.find()) {
                    String g = galleryLinkMatcher.group();
                    if (!link.contains(g))
                        link.add(g);
                }

                while (galleryTitleMatcher.find()) {
                    String g = galleryTitleMatcher.group();
                    if (g.split("\"").length > 1)
                        title.add(g.split("\"")[1]);
                }

                while (galleryPreviewMatcher.find()) {
                    String g = galleryPreviewMatcher.group();
                    previewImage.add(g);
                    downloadImage(g);
                }

                while (galleryPreviewMatcher2.find()) {
                    String g = galleryPreviewMatcher2.group();
                    g = g.replace("inits~exhentai.org~", "http://exhentai.org/");
                    previewImage.add(g);
                    downloadImage(g);
                }
            }
            for (int i = 0; i < link.size(); i++) {
                System.out.println(title.size() + "  Titles found");
                System.out.println(previewImage.size() + "  Pictures found");
                System.out.println(link.size() + "  Galleries found");
                if (link.size() == previewImage.size() && previewImage.size() == title.size())
                    galleryIndex.add(new galleryData(link.get(i), previewImage.get(i), title.get(i)));
                else
                    System.out.println("Sizes do not match up!!");
            }
            if (galSize == galleryIndex.size()) fail = true;
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
            if (Configuration.tagCensored && t.contains("Uncensored") || t.contains("Decensored")) title += " [Uncensored]";
            Matcher m = Pattern.compile("(?!/)[^./]{9,}").matcher(i);
            m.find();
//                System.out.println(m.group(0) + " from " + i);
            imageName = m.group();
        }
    }
}
