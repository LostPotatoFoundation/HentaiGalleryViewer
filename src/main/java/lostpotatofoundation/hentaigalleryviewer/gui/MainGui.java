package lostpotatofoundation.hentaigalleryviewer.gui;

import hxckdms.hxcconfig.HxCConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import lostpotatofoundation.hentaigalleryviewer.Configuration;
import lostpotatofoundation.hentaigalleryviewer.TwoDimensionalValueHashMap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;

public class MainGui extends Application {
    private double initialWidth, initialHeight;
    public static HxCConfig mainConfig;
    public volatile static Pane rootPane = null, topPane = null;
    public static void main(String[] args) {
        URLClassLoader classLoader = (URLClassLoader) MainGui.class.getClassLoader();
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);

            File file = new File("libraries/ConfigurationAPI-1.3.jar");

            method.invoke(classLoader, file.toURI().toURL());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | MalformedURLException e) {
            e.printStackTrace();
        }

        mainConfig = new HxCConfig(Configuration.class, "configuration", new File(System.getProperty("user.dir")), "cfg", "galleryDownloader");
        mainConfig.initConfiguration();
        Configuration.initCookies();

        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader topBar = new FXMLLoader(getClass().getResource("/lostpotatofoundation/hentaigalleryviewer/gallerySearchBar.fxml"));
        FXMLLoader initialLoader = new FXMLLoader(getClass().getResource("/lostpotatofoundation/hentaigalleryviewer/galleryPanel.fxml"));

        rootPane = new Pane();
        primaryStage.setTitle("Hentai viewer");
        topPane = topBar.load();
        topPane.setId("searchBox");
        Pane pane = initialLoader.load();
        pane.setId("0:0");
        pane.setTranslateY(60);
        rootPane.getChildren().add(topPane);
        rootPane.getChildren().add(pane);

        rootPane.heightProperty().addListener((observable, oldValue, newValue) -> onWindowResize_H(primaryStage, rootPane, newValue));
        rootPane.widthProperty().addListener(((observable, oldValue, newValue) -> onWindowResize_W(primaryStage, rootPane, newValue)));

        primaryStage.setScene(new Scene(rootPane));
        primaryStage.show();

        initialWidth = primaryStage.getWidth();
        initialHeight = primaryStage.getHeight();

        primaryStage.setMinHeight(primaryStage.getHeight()+60);
        primaryStage.setMinWidth(primaryStage.getWidth());
    }

    private HashMap<Pane, FXMLLoader> loaders = new HashMap<>();

    private TwoDimensionalValueHashMap<Integer, Pane> panes2d = new TwoDimensionalValueHashMap<>();
    private TwoDimensionalValueHashMap<Integer, Line> verticalLines2d = new TwoDimensionalValueHashMap<>();
    private TwoDimensionalValueHashMap<Integer, Line> horizontalLines2d = new TwoDimensionalValueHashMap<>();

    private int extraDownloaders_H, extraDownloaders_W;

    @SuppressWarnings("SuspiciousMethodCalls")
    private void updateClientWindows(Stage stage, Pane root) {
        double minSizeX = initialWidth, minSizeY = initialHeight;

        horizontalLines2d.clear(); verticalLines2d.clear();

        for (Integer key1 : new HashSet<>(panes2d.keySet())) {
            for (Integer key2 : new HashSet<>(panes2d.get(key1).keySet())) {
                if (loaders.containsKey(panes2d.get(key2, key1))) {
                    panes2d.remove2D(key2, key1);
                } else if (loaders.containsKey(panes2d.get(key2, key1))) {
                    minSizeX = Math.max(panes2d.get(key2, key1).getTranslateX() + minSizeX, minSizeX);
                    minSizeY = Math.max(panes2d.get(key2, key1).getTranslateY() + minSizeY, minSizeY);
                }
            }
        }


        for (int w = 0; w <= extraDownloaders_W; w++) {
            for (int h = 0; h <= extraDownloaders_H; h++) {
                if (h <= 0 && w <= 0) continue;

                if (h > 0) root.getChildren().add(horizontalLines2d.put(w, h, new Line(256 * w, 360 * h, 256 * (w + 1), 360 * h)));
                if (w > 0) root.getChildren().add(verticalLines2d.put(w, h, new Line(256 * w, 360 * h, 256 * w, 360 * (h + 1))));

                if (panes2d.containsKey(w, h)) continue;

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/lostpotatofoundation/hentaigalleryviewer/galleryPanel.fxml"));
                    Pane newPane = loader.load();

                    newPane.setTranslateX(newPane.getPrefWidth() * w);
                    newPane.setTranslateY(newPane.getPrefHeight() * h+60);

                    newPane.setId(w + ":" + h);

                    loaders.put(newPane, loader);
                    root.getChildren().add(panes2d.put(w, h, newPane));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        stage.setMinWidth(minSizeX); stage.setMinHeight(minSizeY);
        root.getChildren().removeIf(node -> node instanceof Line && !verticalLines2d.containsValue(node) && !horizontalLines2d.containsValue(node));
        root.getChildren().removeIf(node -> node instanceof Pane && !node.getId().equalsIgnoreCase("0:0") && !node.getId().equalsIgnoreCase("searchBox") && !panes2d.containsValue(node));
    }

    private void onWindowResize_W(Stage stage, Pane root, Number newValue) {
        extraDownloaders_W = Math.max((int) Math.floor((newValue.doubleValue() - 256D) / 256D), 0);
        updateClientWindows(stage, root);
    }

    private void onWindowResize_H(Stage stage, Pane root, Number newValue) {
        extraDownloaders_H = Math.max((int) Math.floor((newValue.doubleValue() - 360D) / 360D), 0);
        updateClientWindows(stage, root);
    }
}
