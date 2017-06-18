package lostpotatofoundation.hentaigalleryviewer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GalleryDownloadThread extends Thread {
    private static volatile int downloaderID = 0;

    private String title = "";
    private int pageNumber = 0, imageID = 1, pages = 0, imagesCompressed = 0;
    private boolean done;
    private File imageFile;

    static final String GALLERY_PATTERN = "(?:https?://(ex|g\\.e-)hentai\\.org/g/)([^\"<>]+)";
    static final String SLIDE_PATTERN = "(?:https?://(ex|g\\.e-)hentai\\.org/s/)([^\"<>]+)";
    static final String PAGES_PATTERN = "(?:\\d+) pages";
    static final String ROWS_PATTERN = "(?:\\d+) rows";
    static final String TITLE_PATTERN = "(?:<h1 id=\"gn\")([^<]+)";
    static final String PARODY_PATTERN = "(?:ta_parody:)(?:[a-z,A-Z,0-9,_,-]+[^\"])";

    static final String IMAGE_PATTERN = "(?:http://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?/)[^\"]+";

    static final String TITLE_PARSE_PATTERN = "(\\[.*?\\]|\\{.*?\\}|\\(.*?\\))|(=.*=|~.*~)|([^a-z,A-Z,\\s,\\-,\\~,\\|,\\d,\\_])|(\\s{2,}|\\s+\\.)";

    public static File downloadDir = new File(System.getProperty("user.dir"), "DirtyDownloads");
    
    private final String pageURLString;

    public GalleryDownloadThread(String pageURLString) {
        super("Gallery downloader #" + downloaderID++);
        this.pageURLString = pageURLString;
    }

    @Override
    public void run() {
        System.out.println(pageURLString);
        long nano = System.nanoTime();

        parsePage(pageURLString);
        imageFile = null;

        if (Configuration.preCompressCommands.size() > 0) {
            File looseFileDirectory = new File(downloadDir, title);
            for (String command : Configuration.preCompressCommands) {
                File[] innerFiles = looseFileDirectory.listFiles();
                assert innerFiles != null;

                final String[] c = command.split(" ");

                int index = 0;
                for (int i = 0; i < c.length; i++) {
                    if (c[i].contains("%IMAGE%")) {
                        index = i;
                        break;
                    }

                }
                if (Configuration.debug)
                    System.out.println(Arrays.toString(innerFiles));

                for (File innerFile : innerFiles) {
                    String[] clone = c.clone();
                    clone[index] = "\"" + innerFile.getPath() + "\"";
                    if (Configuration.debug)
                        System.out.println(Arrays.toString(clone));
                    runCommands(clone);/* ? */
                }
            }
        }

        if (Configuration.compress) {
            if (Configuration.compressionType.equalsIgnoreCase("cb7") || Configuration.compressionType.equalsIgnoreCase("7z")) compressGallery_cb7();
            else if (Configuration.compressionType.equalsIgnoreCase("cbz") || Configuration.compressionType.equalsIgnoreCase("zip")) compressGallery_cbz();
            if (Configuration.deleteLoseFiles) deleteLoseArchive();
        }

        nano = System.nanoTime() - nano;
        System.out.println((double) nano / 1000000000.0);

        done = true;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteLoseArchive() {
        File looseFileDirectory = new File(downloadDir, title);
        File[] innerFiles = looseFileDirectory.listFiles();
        if (innerFiles != null) for (File file : innerFiles) file.delete();
        looseFileDirectory.delete();
    }

    private void runCommands(String[] command) {
        try {
            Runtime runtime = Runtime.getRuntime();

            final Process p = runtime.exec(command);

            if (Configuration.debug) new Thread(() -> {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;

                try {
                    while ((line = input.readLine()) != null)
                        System.out.println(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            p.waitFor();
        } catch (Exception i) {
            System.out.println(i.getMessage());
            if (Configuration.debug) i.printStackTrace();
        }
    }

    private void compressGallery_cb7() {
        if (!new File(Configuration.program7zPath).exists()) throw new RuntimeException("7z.exe path is invalid.");

        try {
            String[] command = new String[]{
                    Configuration.program7zPath,
                    "a",
                    "-t7z",
                    "\"" + downloadDir.getPath().replace("\\", "/") + "/" + title + ".cb7\"",
                    "\"" + downloadDir.getPath().replace("\\", "/") + "/" + title + "/*\""
            };
            runCommands(command);


            imagesCompressed = pages;
        } catch (Exception e) {
            if (Configuration.debug) e.printStackTrace();
        }
    }

    private void compressGallery_cbz() {
        try {
            File galleryFolder = new File(downloadDir, title);

            File[] images = galleryFolder.listFiles();
            if (images == null) return;

            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(new File(downloadDir, title + ".cbz")));
            for (File image : images) {
                imagesCompressed++;
                ZipEntry zipEntry = new ZipEntry(image.getName());

                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(Files.readAllBytes(image.toPath()));
                zipOutputStream.closeEntry();
            }
            zipOutputStream.close();
        } catch (Exception e) {
            if (Configuration.debug) e.printStackTrace();
        }
    }

    private void parsePage(String pageURLString) {
        if (Configuration.debug) System.out.println(pageURLString);
        try {
            URL url = new URL(pageURLString);

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


            for (String line : lineList) {
                Matcher slideMatcher = Pattern.compile(SLIDE_PATTERN).matcher(line), rowMatcher = Pattern.compile(ROWS_PATTERN).matcher(line), pageMatcher = Pattern.compile(PAGES_PATTERN).matcher(line), titleMatcher = Pattern.compile(TITLE_PATTERN).matcher(line);
                boolean pageNumberFound = pageMatcher.find(), rowNumberFound = rowMatcher.find();
                pages = pageNumberFound ? Integer.parseInt(pageMatcher.group().split(" ")[0]) : pages;
                int rows = rowNumberFound ? Integer.parseInt(rowMatcher.group().split(" ")[0]) : 0;
                title = title.isEmpty() && titleMatcher.find() ? titleMatcher.group().split(">")[1].replaceAll(TITLE_PARSE_PATTERN, " ").trim() : title;
                if (title.contains("|")) title = Configuration.attemptEnglish ? title.split("\\|")[1].trim() : title.replaceAll("\\|", "").trim();
                if (title.isEmpty()) continue;

                while (slideMatcher.find()) {
                    parseSlide(new File(downloadDir, title), slideMatcher.group());
                    imageID++;
                }

                if (!pageNumberFound || !rowNumberFound) continue;

                if (imageID > rows * 10 * (pageNumber + 1) && imageID <= pages) {
                    parsePage(pageURLString.replaceAll("\\?p=\\d+", "") + "?p=" + ++pageNumber);
                }
            }
        } catch (Exception e) {
            if (Configuration.debug) e.printStackTrace();
        }
    }

    private void parseSlide(File galleryDir, String urlString) {
        if (Configuration.debug) System.out.println(urlString);
        try {
            if (!galleryDir.exists() && !galleryDir.mkdirs()) throw new RuntimeException("Couldn't create download directory.");

            URL url = new URL(urlString);
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

            for (String line : lineList) {
                Matcher matcher = Pattern.compile(IMAGE_PATTERN).matcher(line);

                while (matcher.find()) {
                    downloadImage(galleryDir, matcher.group());
                }
            }

        } catch (Exception e) {
            if (Configuration.debug) e.printStackTrace();
        }
    }

    private void downloadImage(File galleryDir, String urlString) throws Exception {
        //System.out.println(urlString);

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Cookie", Configuration.getCookies());

            imageFile = new File(galleryDir, imageID +".png");
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
            System.out.println(e.getMessage());
            File f = new File(galleryDir, imageID + ".png");
            if (f.exists()) f.delete();
            downloadImage(galleryDir, urlString);
        }
        File f = new File(galleryDir, imageID + ".png");
        if (!f.canRead() || !f.exists() || f.getTotalSpace() == 0) {
            f.delete();
            downloadImage(galleryDir, urlString);
        }
    }

    public synchronized boolean isDone() {
        return done;
    }

    public synchronized double getDownloadProgress() {
        if (pages != 0) return (double) Math.min(imageID, pages) / (double) pages;
        return 0.0D;
    }

    public synchronized double getCompressionProgress() {
        if (pages != 0) return (double) Math.min(imagesCompressed, pages) / (double) pages;
        return 0.0D;
    }

    public synchronized String getTitle() {
        return title;
    }

    public synchronized File getImageFile() {
        return imageFile;
    }
}
