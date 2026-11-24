package ua.nupp.maintenance.client.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ua.nupp.maintenance.client.api.ApiClient;
import ua.nupp.maintenance.client.importer.EquipmentImportTask;
import ua.nupp.maintenance.client.importer.ImportResult;
import ua.nupp.maintenance.client.model.Equipment;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/** Вкладка «Обладнання»: таблиця, форма CRUD і фоновий CSV-імпорт реєстру. */
public class EquipmentPane extends BorderPane {
    private final ApiClient api;
    private final Consumer<List<Equipment>> onChanged;   // повідомляє вкладку заявок про новий список

    private final TableView<Equipment> table = new TableView<>();
    private final TextField invField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField modelField = new TextField();
    private final TextField locationField = new TextField();
    private final Spinner<Integer> criticality = new Spinner<>(1, 5, 3);
    private final DatePicker datePicker = new DatePicker();

    private final ProgressBar progress = new ProgressBar(0);
    private final Label importStatus = new Label("CSV-імпорт не виконувався");
    private final Button importBtn = new Button("Імпорт CSV…");
    private final Button cancelBtn = new Button("Скасувати");
    private EquipmentImportTask importTask;

    public EquipmentPane(ApiClient api, Consumer<List<Equipment>> onChanged) {
        this.api = api;
        this.onChanged = onChanged;
        buildTable();
        setCenter(table);
        setRight(buildForm());
        setBottom(buildImportBar());
        setPadding(new Insets(10));
        refresh();
    }

    private void buildTable() {
        TableColumn<Equipment, Object> id = new TableColumn<>("ID");
        id.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        TableColumn<Equipment, String> inv = new TableColumn<>("Інв. номер");
        inv.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInventoryNumber()));
        TableColumn<Equipment, String> name = new TableColumn<>("Назва");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        name.setPrefWidth(200);
        TableColumn<Equipment, String> model = new TableColumn<>("Модель");
        model.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModel()));
        model.setPrefWidth(140);
        TableColumn<Equipment, String> loc = new TableColumn<>("Розташування");
        loc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));
        TableColumn<Equipment, Object> crit = new TableColumn<>("Критичність");
        crit.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCriticality()));
        TableColumn<Equipment, String> date = new TableColumn<>("Введено в експл.");
        date.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCommissionedDate()));
        table.getColumns().add(id);
        table.getColumns().add(inv);
        table.getColumns().add(name);
        table.getColumns().add(model);
        table.getColumns().add(loc);
        table.getColumns().add(crit);
        table.getColumns().add(date);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, e) -> fillForm(e));
    }

    private VBox buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.addRow(0, new Label("Інв. номер*"), invField);
        g.addRow(1, new Label("Назва*"), nameField);
        g.addRow(2, new Label("Модель"), modelField);
        g.addRow(3, new Label("Розташування"), locationField);
        g.addRow(4, new Label("Критичність (1–5)*"), criticality);
        g.addRow(5, new Label("Дата введення"), datePicker);
        criticality.setEditable(true);

        Button newBtn = new Button("Нове");
        newBtn.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            fillForm(null);
        });
        Button saveBtn = new Button("Зберегти");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> save());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> delete());
        Button refreshBtn = new Button("Оновити");
        refreshBtn.setOnAction(e -> refresh());

        VBox box = new VBox(10, new Label("Картка обладнання"), g, new HBox(8, newBtn, saveBtn, deleteBtn, refreshBtn),
                new Label("* — обов'язкові поля"));
        box.setPadding(new Insets(0, 0, 0, 12));
        box.setPrefWidth(380);
        return box;
    }

    private HBox buildImportBar() {
        progress.setPrefWidth(260);
        cancelBtn.setDisable(true);
        importBtn.setOnAction(e -> startImport());
        cancelBtn.setOnAction(e -> {
            if (importTask != null) {
                importTask.cancel();
            }
        });
        HBox bar = new HBox(10, importBtn, progress, cancelBtn, importStatus);
        bar.setPadding(new Insets(10, 0, 0, 0));
        return bar;
    }

    void refresh() {
        Ui.async(api::getEquipment, list -> {
            table.setItems(FXCollections.observableArrayList(list));
            onChanged.accept(list);
        });
    }

    private void fillForm(Equipment e) {
        invField.setText(e == null ? "" : e.getInventoryNumber());
        nameField.setText(e == null ? "" : e.getName());
        modelField.setText(e == null ? "" : Ui.nvl(e.getModel()));
        locationField.setText(e == null ? "" : Ui.nvl(e.getLocation()));
        criticality.getValueFactory().setValue(e == null ? 3 : e.getCriticality());
        datePicker.setValue(e == null || e.getCommissionedDate() == null ? null : LocalDate.parse(e.getCommissionedDate()));
    }

    /** Клієнтська валідація — для швидкої підказки; остаточно правила перевіряє сервер. */
    private Equipment readForm() {
        String inv = Ui.text(invField);
        String name = Ui.text(nameField);
        if (inv == null || name == null) {
            Ui.error("Заповніть інвентарний номер і назву");
            return null;
        }
        Integer crit = criticality.getValue();
        if (crit == null || crit < 1 || crit > 5) {
            Ui.error("Критичність має бути від 1 до 5");
            return null;
        }
        LocalDate d = datePicker.getValue();
        if (d != null && d.isAfter(LocalDate.now())) {
            Ui.error("Дата введення в експлуатацію не може бути в майбутньому");
            return null;
        }
        return new Equipment(inv, name, Ui.text(modelField), Ui.text(locationField), crit,
                d == null ? null : d.toString());
    }

    private void save() {
        Equipment e = readForm();
        if (e == null) {
            return;
        }
        Equipment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.async(() -> api.createEquipment(e), saved -> refresh());
        } else {
            e.setId(selected.getId());
            Ui.async(() -> api.updateEquipment(e), saved -> refresh());
        }
    }

    private void delete() {
        Equipment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.error("Оберіть обладнання в таблиці");
            return;
        }
        if (Ui.confirm("Видалити «" + selected + "»?")) {
            Ui.async(() -> api.deleteEquipment(selected.getId()), this::refresh);
        }
    }

    // ---------------- CSV-імпорт ----------------

    private void startImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Оберіть CSV-файл реєстру обладнання");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File sample = new File("sample-data");          // відносний шлях — каталог client/sample-data
        if (sample.isDirectory()) {
            chooser.setInitialDirectory(sample);
        }
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }

        importTask = new EquipmentImportTask(file.toPath(), api);
        progress.progressProperty().bind(importTask.progressProperty());
        importStatus.textProperty().bind(importTask.messageProperty());
        importBtn.setDisable(true);
        cancelBtn.setDisable(false);

        importTask.setOnSucceeded(e -> finishImport(importTask.getValue(), null));
        importTask.setOnCancelled(e -> finishImport(null, "Імпорт скасовано користувачем"));
        importTask.setOnFailed(e -> finishImport(null, "Імпорт не виконано: " + importTask.getException().getMessage()));

        Thread t = new Thread(importTask, "csv-import");
        t.setDaemon(true);
        t.start();
    }

    private void finishImport(ImportResult result, String error) {
        importStatus.textProperty().unbind();
        progress.progressProperty().unbind();
        importBtn.setDisable(false);
        cancelBtn.setDisable(true);
        refresh();
        if (error != null) {
            importStatus.setText(error);
            Ui.error(error);
            return;
        }
        progress.setProgress(1);
        importStatus.setText(result.summary());
        String details = result.getErrors().isEmpty()
                ? "Усі рядки імпортовано успішно."
                : "Пропущені рядки:\n" + String.join("\n", result.getErrors());
        Ui.info(result.summary(), details);
    }
}
