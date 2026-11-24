package ua.nupp.maintenance.client.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ua.nupp.maintenance.client.api.ApiClient;
import ua.nupp.maintenance.client.model.Specialist;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Вкладка «Фахівці»: таблиця і форма CRUD. */
public class SpecialistPane extends BorderPane {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ApiClient api;
    private final Consumer<List<Specialist>> onChanged;
    private final TableView<Specialist> table = new TableView<>();
    private final TextField nameField = new TextField();
    private final TextField specField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final CheckBox activeBox = new CheckBox("Активний");

    public SpecialistPane(ApiClient api, Consumer<List<Specialist>> onChanged) {
        this.api = api;
        this.onChanged = onChanged;
        buildTable();
        setCenter(table);
        setRight(buildForm());
        setPadding(new Insets(10));
        refresh();
    }

    private void buildTable() {
        TableColumn<Specialist, Object> id = new TableColumn<>("ID");
        id.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        TableColumn<Specialist, String> name = new TableColumn<>("ПІБ");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        name.setPrefWidth(220);
        TableColumn<Specialist, String> spec = new TableColumn<>("Спеціалізація");
        spec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpecialization()));
        spec.setPrefWidth(160);
        TableColumn<Specialist, String> phone = new TableColumn<>("Телефон");
        phone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        TableColumn<Specialist, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        TableColumn<Specialist, String> active = new TableColumn<>("Стан");
        active.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "активний" : "неактивний"));
        table.getColumns().add(id);
        table.getColumns().add(name);
        table.getColumns().add(spec);
        table.getColumns().add(phone);
        table.getColumns().add(email);
        table.getColumns().add(active);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, s) -> fillForm(s));
    }

    private VBox buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.addRow(0, new Label("ПІБ*"), nameField);
        g.addRow(1, new Label("Спеціалізація"), specField);
        g.addRow(2, new Label("Телефон"), phoneField);
        g.addRow(3, new Label("Email"), emailField);
        g.add(activeBox, 1, 4);
        activeBox.setSelected(true);

        Button newBtn = new Button("Новий");
        newBtn.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            fillForm(null);
        });
        Button saveBtn = new Button("Зберегти");
        saveBtn.setOnAction(e -> save());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> delete());
        Button refreshBtn = new Button("Оновити");
        refreshBtn.setOnAction(e -> refresh());

        VBox box = new VBox(10, new Label("Картка фахівця"), g, new HBox(8, newBtn, saveBtn, deleteBtn, refreshBtn),
                new Label("Фахівця із заявками не видаляють —\nйого позначають неактивним."));
        box.setPadding(new Insets(0, 0, 0, 12));
        box.setPrefWidth(380);
        return box;
    }

    void refresh() {
        Ui.async(api::getSpecialists, list -> {
            table.setItems(FXCollections.observableArrayList(list));
            onChanged.accept(list);
        });
    }

    private void fillForm(Specialist s) {
        nameField.setText(s == null ? "" : s.getFullName());
        specField.setText(s == null ? "" : Ui.nvl(s.getSpecialization()));
        phoneField.setText(s == null ? "" : Ui.nvl(s.getPhone()));
        emailField.setText(s == null ? "" : Ui.nvl(s.getEmail()));
        activeBox.setSelected(s == null || s.isActive());
    }

    private void save() {
        String name = Ui.text(nameField);
        if (name == null) {
            Ui.error("Вкажіть ПІБ фахівця");
            return;
        }
        String email = Ui.text(emailField);
        if (email != null && !EMAIL.matcher(email).matches()) {
            Ui.error("Некоректний email");
            return;
        }
        Specialist s = new Specialist();
        s.setFullName(name);
        s.setSpecialization(Ui.text(specField));
        s.setPhone(Ui.text(phoneField));
        s.setEmail(email);
        s.setActive(activeBox.isSelected());
        Specialist selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.async(() -> api.createSpecialist(s), saved -> refresh());
        } else {
            s.setId(selected.getId());
            Ui.async(() -> api.updateSpecialist(s), saved -> refresh());
        }
    }

    private void delete() {
        Specialist selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.error("Оберіть фахівця в таблиці");
            return;
        }
        if (Ui.confirm("Видалити фахівця «" + selected.getFullName() + "»?")) {
            Ui.async(() -> api.deleteSpecialist(selected.getId()), this::refresh);
        }
    }
}
