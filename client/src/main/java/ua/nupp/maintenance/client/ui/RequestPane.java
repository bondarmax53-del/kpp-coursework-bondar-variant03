package ua.nupp.maintenance.client.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.nupp.maintenance.client.api.ApiClient;
import ua.nupp.maintenance.client.model.Equipment;
import ua.nupp.maintenance.client.model.MaintenanceRequest;
import ua.nupp.maintenance.client.model.RequestStatus;
import ua.nupp.maintenance.client.model.ServiceRecord;
import ua.nupp.maintenance.client.model.Specialist;
import ua.nupp.maintenance.client.model.StatusChange;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Вкладка «Заявки»: черга заявок за пріоритетом I, CRUD заявок,
 * перехід за життєвим циклом і сервісні записи вибраної заявки.
 */
public class RequestPane extends BorderPane {
    private static final String ALL = "Усі статуси";

    private final ApiClient api;
    private final Map<Long, Equipment> equipmentById = new HashMap<>();
    private final Map<Long, Specialist> specialistById = new HashMap<>();

    // черга заявок
    private final ComboBox<Object> statusFilter = new ComboBox<>();
    private final TableView<MaintenanceRequest> table = new TableView<>();

    // форма заявки
    private final ComboBox<Equipment> equipmentBox = new ComboBox<>();
    private final TextArea descriptionArea = new TextArea();
    private final Spinner<Integer> severity = new Spinner<>(1, 5, 3);
    private final Spinner<Integer> downtime = new Spinner<>(0, 1000, 8, 1);

    // життєвий цикл
    private final Label statusLabel = new Label("—");
    private final Label assignedLabel = new Label("—");
    private final Label diagnosisLabel = new Label("—");
    private final Button nextStatusBtn = new Button("Наступний статус");

    // сервісні записи
    private final TableView<ServiceRecord> records = new TableView<>();
    private final TextField workField = new TextField();
    private final TextField hoursField = new TextField();
    private final TextField costField = new TextField();

    public RequestPane(ApiClient api) {
        this.api = api;
        setTop(buildToolbar());
        SplitPane split = new SplitPane(buildTable(), buildDetails());
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.55);
        setCenter(split);
        setPadding(new Insets(10));
        showSelected(null);
    }

    // ---------------- довідники з інших вкладок ----------------

    void setEquipment(List<Equipment> list) {
        equipmentById.clear();
        list.forEach(e -> equipmentById.put(e.getId(), e));
        equipmentBox.setItems(FXCollections.observableArrayList(list));
        table.refresh();
    }

    void setSpecialists(List<Specialist> list) {
        specialistById.clear();
        list.forEach(s -> specialistById.put(s.getId(), s));
        table.refresh();
        records.refresh();
    }

    // ---------------- побудова інтерфейсу ----------------

    private HBox buildToolbar() {
        statusFilter.getItems().add(ALL);
        statusFilter.getItems().addAll((Object[]) RequestStatus.values());
        statusFilter.setValue(ALL);
        statusFilter.setOnAction(e -> refresh());
        Button refreshBtn = new Button("Оновити");
        refreshBtn.setOnAction(e -> refresh());
        Label hint = new Label("Заявки впорядковано за інтегральним показником I (спершу найтерміновіші)");
        HBox bar = new HBox(10, new Label("Фільтр:"), statusFilter, refreshBtn, hint);
        bar.setPadding(new Insets(0, 0, 8, 0));
        return bar;
    }

    private TableView<MaintenanceRequest> buildTable() {
        TableColumn<MaintenanceRequest, Object> id = new TableColumn<>("ID");
        id.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        TableColumn<MaintenanceRequest, String> eq = new TableColumn<>("Обладнання");
        eq.setCellValueFactory(c -> new SimpleStringProperty(equipmentName(c.getValue().getEquipmentId())));
        eq.setPrefWidth(200);
        TableColumn<MaintenanceRequest, String> desc = new TableColumn<>("Опис несправності");
        desc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        desc.setPrefWidth(260);
        TableColumn<MaintenanceRequest, Object> sev = new TableColumn<>("Тяжкість");
        sev.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSeverity()));
        TableColumn<MaintenanceRequest, Object> down = new TableColumn<>("Простій, год");
        down.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDowntimeHours()));
        TableColumn<MaintenanceRequest, String> prio = new TableColumn<>("I");
        prio.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "%.2f", c.getValue().getPriority())));
        prio.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
        TableColumn<MaintenanceRequest, String> st = new TableColumn<>("Статус");
        st.setCellValueFactory(c -> new SimpleStringProperty(Ui.nvl(c.getValue().getStatus())));
        TableColumn<MaintenanceRequest, String> spec = new TableColumn<>("Фахівець");
        spec.setCellValueFactory(c -> new SimpleStringProperty(specialistName(c.getValue().getSpecialistId())));
        spec.setPrefWidth(170);
        TableColumn<MaintenanceRequest, String> created = new TableColumn<>("Створено");
        created.setCellValueFactory(c -> new SimpleStringProperty(Ui.nvl(c.getValue().getCreatedAt()).replace('T', ' ')));
        table.getColumns().add(id);
        table.getColumns().add(prio);
        table.getColumns().add(st);
        table.getColumns().add(eq);
        table.getColumns().add(desc);
        table.getColumns().add(sev);
        table.getColumns().add(down);
        table.getColumns().add(spec);
        table.getColumns().add(created);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, r) -> showSelected(r));
        Ui.setUp(table); return table;
    }

    private HBox buildDetails() {
        // --- форма заявки ---
        GridPane form = Ui.grid();
        form.setHgap(8);
        form.setVgap(8);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        severity.setEditable(true);
        downtime.setEditable(true);
        form.addRow(0, new Label("Обладнання*"), equipmentBox);
        form.addRow(1, new Label("Опис*"), descriptionArea);
        form.addRow(2, new Label("Тяжкість (1–5)*"), severity);
        form.addRow(3, new Label("Очікуваний простій, год*"), downtime);
        Button newBtn = new Button("Нова");
        newBtn.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            showSelected(null);
        });
        Button saveBtn = new Button("Зберегти");
        saveBtn.setOnAction(e -> saveRequest());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> deleteRequest());
        VBox formBox = new VBox(8, title("Заявка"), form, new HBox(8, newBtn, saveBtn, deleteBtn),
                new Label("Пріоритет I розраховує сервер."));
        formBox.setPrefWidth(470);

        // --- життєвий цикл ---
        GridPane life = Ui.grid();
        life.setHgap(8);
        life.setVgap(8);
        diagnosisLabel.setWrapText(true);
        diagnosisLabel.setMaxWidth(220);
        life.addRow(0, new Label("Статус:"), statusLabel);
        life.addRow(1, new Label("Фахівець:"), assignedLabel);
        life.addRow(2, new Label("Діагноз:"), diagnosisLabel);
        statusLabel.setStyle("-fx-font-weight: bold;");
        nextStatusBtn.setOnAction(e -> moveToNextStatus());
        VBox lifeBox = new VBox(8, title("Життєвий цикл"),
                new Label("Нова → Діагностовано →\nОбслуговування → Виконано"), life, nextStatusBtn);
        lifeBox.setPrefWidth(320);

        // --- сервісні записи ---
        TableColumn<ServiceRecord, String> work = new TableColumn<>("Роботи");
        work.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWorkDescription()));
        work.setPrefWidth(200);
        TableColumn<ServiceRecord, Object> hours = new TableColumn<>("Год");
        hours.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getHoursSpent()));
        TableColumn<ServiceRecord, Object> cost = new TableColumn<>("Вартість, грн");
        cost.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCost()));
        TableColumn<ServiceRecord, String> who = new TableColumn<>("Виконавець");
        who.setCellValueFactory(c -> new SimpleStringProperty(specialistName(c.getValue().getSpecialistId())));
        records.getColumns().add(work);
        records.getColumns().add(hours);
        records.getColumns().add(cost);
        records.getColumns().add(who);
        records.setPrefHeight(130); Ui.setUp(records);
        records.getSelectionModel().selectedItemProperty().addListener((obs, old, r) -> {
            workField.setText(r == null ? "" : r.getWorkDescription());
            hoursField.setText(r == null ? "" : Ui.nvl(r.getHoursSpent()));
            costField.setText(r == null ? "" : Ui.nvl(r.getCost()));
        });
        workField.setPromptText("Опис виконаних робіт");
        hoursField.setPromptText("Год");
        hoursField.setPrefWidth(60);
        costField.setPromptText("Грн");
        costField.setPrefWidth(80);
        Button addRec = new Button("Додати");
        addRec.setOnAction(e -> saveRecord(false));
        Button updRec = new Button("Змінити");
        updRec.setOnAction(e -> saveRecord(true));
        Button delRec = new Button("Видалити");
        delRec.setOnAction(e -> deleteRecord());
        HBox.setHgrow(workField, Priority.ALWAYS);
        VBox recBox = new VBox(8, title("Сервісні записи"), records,
                new HBox(6, workField, hoursField, costField), new HBox(6, addRec, updRec, delRec),
                new Label("Записи змінюються лише у статусі «Обслуговування»."));
        HBox.setHgrow(recBox, Priority.ALWAYS);

        HBox box = new HBox(20, formBox, lifeBox, recBox);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private static Label title(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return l;
    }

    // ---------------- дані ----------------

    void refresh() {
        Object f = statusFilter.getValue();
        RequestStatus status = f instanceof RequestStatus rs ? rs : null;
        Long keep = selectedId();
        Ui.async(() -> api.getRequests(status), list -> {
            table.setItems(FXCollections.observableArrayList(list));
            if (keep != null) {
                list.stream().filter(r -> r.getId().equals(keep)).findFirst()
                        .ifPresent(r -> table.getSelectionModel().select(r));
            }
        });
    }

    private Long selectedId() {
        MaintenanceRequest r = table.getSelectionModel().getSelectedItem();
        return r == null ? null : r.getId();
    }

    private void showSelected(MaintenanceRequest r) {
        equipmentBox.setValue(r == null ? null : equipmentById.get(r.getEquipmentId()));
        descriptionArea.setText(r == null ? "" : r.getDescription());
        severity.getValueFactory().setValue(r == null ? 3 : r.getSeverity());
        downtime.getValueFactory().setValue(r == null ? 8 : r.getDowntimeHours());

        statusLabel.setText(r == null ? "—" : r.getStatus().getTitle());
        assignedLabel.setText(r == null ? "—" : specialistName(r.getSpecialistId()));
        diagnosisLabel.setText(r == null || r.getDiagnosis() == null ? "—" : r.getDiagnosis());
        RequestStatus next = r == null ? null : r.getStatus().next();
        nextStatusBtn.setDisable(next == null);
        nextStatusBtn.setText(next == null ? "Наступний статус" : "Перевести в «" + next.getTitle() + "»");

        records.getItems().clear();
        workField.clear();
        hoursField.clear();
        costField.clear();
        if (r != null) {
            Ui.async(() -> api.getRecords(r.getId()), list -> records.setItems(FXCollections.observableArrayList(list)));
        }
    }

    private void saveRequest() {
        Equipment eq = equipmentBox.getValue();
        String desc = Ui.text(descriptionArea);
        if (eq == null || desc == null) {
            Ui.error("Оберіть обладнання та опишіть несправність");
            return;
        }
        MaintenanceRequest r = new MaintenanceRequest();
        r.setEquipmentId(eq.getId());
        r.setDescription(desc);
        r.setSeverity(severity.getValue());
        r.setDowntimeHours(downtime.getValue());
        Long id = selectedId();
        if (id == null) {
            Ui.async(() -> api.createRequest(r), saved -> {
                Ui.info("Заявку створено", "Розрахований пріоритет I = "
                        + String.format(Locale.US, "%.2f", saved.getPriority()));
                refresh();
            });
        } else {
            r.setId(id);
            Ui.async(() -> api.updateRequest(r), saved -> refresh());
        }
    }

    private void deleteRequest() {
        Long id = selectedId();
        if (id == null) {
            Ui.error("Оберіть заявку в таблиці");
            return;
        }
        if (Ui.confirm("Видалити заявку №" + id + "?")) {
            Ui.async(() -> api.deleteRequest(id), this::refresh);
        }
    }

    /** Перехід на наступний статус. Для діагностики питаємо фахівця й діагноз. */
    private void moveToNextStatus() {
        MaintenanceRequest r = table.getSelectionModel().getSelectedItem();
        if (r == null || r.getStatus().next() == null) {
            return;
        }
        RequestStatus next = r.getStatus().next();
        StatusChange change;
        if (next == RequestStatus.DIAGNOSED) {
            change = askDiagnosis();
            if (change == null) {
                return;
            }
        } else {
            if (!Ui.confirm("Перевести заявку №" + r.getId() + " у статус «" + next.getTitle() + "»?")) {
                return;
            }
            change = new StatusChange(next, null, null);
        }
        Ui.async(() -> api.changeStatus(r.getId(), change), saved -> refresh());
    }

    private StatusChange askDiagnosis() {
        Dialog<StatusChange> dialog = new Dialog<>();
        dialog.setTitle("Діагностика");
        dialog.setHeaderText("Призначте фахівця та вкажіть діагноз");
        ComboBox<Specialist> who = new ComboBox<>(FXCollections.observableArrayList(
                specialistById.values().stream().filter(Specialist::isActive).toList()));
        TextArea diagnosis = new TextArea();
        diagnosis.setPrefRowCount(3);
        diagnosis.setWrapText(true);
        GridPane g = Ui.grid();
        g.setHgap(8);
        g.setVgap(8);
        g.addRow(0, new Label("Фахівець*"), who);
        g.addRow(1, new Label("Діагноз*"), diagnosis);
        dialog.getDialogPane().setContent(g);
        ButtonType ok = new ButtonType("Підтвердити", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        // кнопка недоступна, доки не заповнено обидва поля
        dialog.getDialogPane().lookupButton(ok).disableProperty().bind(
                who.valueProperty().isNull().or(diagnosis.textProperty().isEmpty()));
        dialog.setResultConverter(b -> b == ok
                ? new StatusChange(RequestStatus.DIAGNOSED, who.getValue().getId(), diagnosis.getText().trim())
                : null);
        return dialog.showAndWait().orElse(null);
    }

    private void saveRecord(boolean update) {
        Long id = selectedId();
        if (id == null) {
            Ui.error("Оберіть заявку");
            return;
        }
        ServiceRecord selected = records.getSelectionModel().getSelectedItem();
        if (update && selected == null) {
            Ui.error("Оберіть сервісний запис для зміни");
            return;
        }
        String work = Ui.text(workField);
        BigDecimal hours = decimal(hoursField.getText());
        BigDecimal cost = Ui.text(costField) == null ? BigDecimal.ZERO : decimal(costField.getText());
        if (work == null || hours == null || hours.signum() <= 0 || cost == null || cost.signum() < 0) {
            Ui.error("Вкажіть опис робіт, години (> 0) та вартість (≥ 0)");
            return;
        }
        ServiceRecord rec = new ServiceRecord();
        rec.setWorkDescription(work);
        rec.setHoursSpent(hours);
        rec.setCost(cost);
        rec.setRequestId(id);
        if (update) {
            rec.setId(selected.getId());
            rec.setSpecialistId(selected.getSpecialistId());
            Ui.async(() -> api.updateRecord(rec), saved -> refresh());
        } else {
            Ui.async(() -> api.addRecord(id, rec), saved -> refresh());
        }
    }

    private void deleteRecord() {
        Long id = selectedId();
        ServiceRecord selected = records.getSelectionModel().getSelectedItem();
        if (id == null || selected == null) {
            Ui.error("Оберіть сервісний запис");
            return;
        }
        if (Ui.confirm("Видалити сервісний запис?")) {
            Ui.async(() -> api.deleteRecord(id, selected.getId()), this::refresh);
        }
    }

    private static BigDecimal decimal(String s) {
        try {
            return s == null || s.isBlank() ? null : new BigDecimal(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String equipmentName(Long id) {
        Equipment e = id == null ? null : equipmentById.get(id);
        return e == null ? Ui.nvl(id) : e.toString();
    }

    private String specialistName(Long id) {
        Specialist s = id == null ? null : specialistById.get(id);
        return s == null ? (id == null ? "—" : "#" + id) : s.getFullName();
    }
}
