package ua.nupp.maintenance.client.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ua.nupp.maintenance.client.api.ApiClient;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Головне вікно JavaFX-клієнта. Клієнт не працює з MySQL напряму:
 * усі дані йдуть через ApiClient → HTTP/JSON → сервер.
 */
public class MainApp extends Application {
    private static final Logger ROOT = Logger.getLogger("ua.nupp.maintenance.client");
    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());
    private FileHandler fileHandler;

    @Override
    public void start(Stage stage) {
        setUpLog();
        ApiClient api = ApiClient.fromConfig();

        RequestPane requests = new RequestPane(api);
        EquipmentPane equipment = new EquipmentPane(api, requests::setEquipment);
        SpecialistPane specialists = new SpecialistPane(api, requests::setSpecialists);

        Tab reqTab = new Tab("Заявки", requests);
        TabPane tabs = new TabPane(
                reqTab,
                new Tab("Обладнання", equipment),
                new Tab("Фахівці", specialists));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, t) -> {
            if (t == reqTab) {
                requests.refresh();
            }
        });

        Label status = new Label("Сервер: " + api.getBaseUrl() + " — перевірка…");
        status.setPadding(new Insets(4, 10, 4, 10));
        Ui.async(api::isServerUp, up -> {
            status.setText("Сервер: " + api.getBaseUrl() + (up ? " — підключено" : " — НЕДОСТУПНИЙ"));
            status.setStyle(up ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            if (up) {
                requests.refresh();
            }
        });

        BorderPane root = new BorderPane(tabs);
        root.setBottom(status);
        stage.setTitle("ІС технічного обслуговування лабораторного обладнання");
        stage.setScene(new Scene(root, 1280, 760));
        stage.show();
        LOG.info("Клієнт запущено, сервер " + api.getBaseUrl());
    }

    @Override
    public void stop() {
        LOG.info("Клієнт закрито");
        if (fileHandler != null) {
            ROOT.removeHandler(fileHandler);
            fileHandler.close();
        }
    }

    /** Журнал client.log у робочому каталозі (відносний шлях), кодування UTF-8. */
    private void setUpLog() {
        try {
            fileHandler = new FileHandler(new File("client.log").getPath(), true);
            fileHandler.setEncoding("UTF-8");
            fileHandler.setFormatter(new SimpleFormatter());
            ROOT.addHandler(fileHandler);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Файловий журнал недоступний", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
