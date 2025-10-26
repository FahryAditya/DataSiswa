package com.example.datasiswautama;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SmartDataParser extends Application {

    private TextArea inputArea;
    private TreeTableView<Student> treeTable;
    private ObservableList<Student> dataList;
    private Set<String> existingNames;
    private TextField nameField;
    private ComboBox<String> groupSelect, jurusanSelect, tingkatSelect;
    private Label countLabel;
    private ProgressIndicator progressIndicator;

    private static final Set<String> JURUSAN_SMK_AIRLANGGA = Set.of("AKL", "DKV", "MPLB", "PPLG", "TJKT");
    private static final Set<String> JURUSAN_SMK_KESEHATAN = Set.of("AKC", "FKK", "TLM");
    private static final List<String> KELAS_ORDER = List.of("10", "11", "12");

    // ===== Singleton-ish safe reference (only assigned by JavaFX) =====
    private static SmartDataParser instance;

    public SmartDataParser() {
        // JavaFX will create the Application instance; store reference for external callers.
        instance = this;
    }

    public static SmartDataParser getInstance() {
        // Return current instance if Application is running, otherwise null.
        return instance;
    }

    // Allow external callers (AdminInputApp) to add student safely.
    public void addStudent(String nama, String kelas, String jurusan, String sekolah) {
        if (dataList == null) {
            dataList = FXCollections.observableArrayList();
        }
        if (existingNames == null) {
            existingNames = new HashSet<>();
        }

        if (nama == null || nama.trim().isEmpty()) return;
        String key = nama.trim().toLowerCase();
        if (!existingNames.contains(key)) {
            Student s = new Student(nama.trim(), kelas == null ? "" : kelas.trim(), jurusan == null ? "" : jurusan.trim(), sekolah == null ? "" : sekolah.trim());
            dataList.add(s);
            existingNames.add(key);
            updateCount();
            refreshTreeWithAnimation();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("🏫 Smart Data Parser Sekolah ");
        // initialize collections early to avoid NPEs
        dataList = FXCollections.observableArrayList();
        existingNames = new HashSet<>();

        buildUI(stage);
        refreshTreeWithAnimation();
    }

    private void buildUI(Stage stage) {
        // Input area
        inputArea = new TextArea();
        inputArea.setPromptText("Masukkan data siswa (satu entri per baris). Contoh:\n" +
                "Dimas Arif Nugraha – 10 – AKL – SMK Airlangga\natau\nRina Safitri kelas 11 PPLG");
        inputArea.setWrapText(true);
        inputArea.setPrefRowCount(4);

        Button btnProcess = new Button("⚙️ Proses Otomatis");
        btnProcess.setOnAction(e -> {
            showProgress(true);
            processText();
            refreshTreeWithAnimation();
            showProgress(false);
        });

        // Manual input controls
        nameField = new TextField();
        nameField.setPromptText("Nama siswa");

        groupSelect = new ComboBox<>();
        groupSelect.getItems().addAll("SMK Airlangga", "SMK Kesehatan Airlangga");
        groupSelect.setPromptText("Pilih Sekolah");

        jurusanSelect = new ComboBox<>();
        jurusanSelect.setPromptText("Pilih Jurusan");

        tingkatSelect = new ComboBox<>();
        tingkatSelect.getItems().addAll("10", "11", "12");
        tingkatSelect.setPromptText("Pilih Kelas");

        groupSelect.valueProperty().addListener((obs, oldV, v) -> {
            jurusanSelect.getItems().clear();
            if ("SMK Airlangga".equals(v)) jurusanSelect.getItems().addAll(JURUSAN_SMK_AIRLANGGA);
            else if ("SMK Kesehatan Airlangga".equals(v)) jurusanSelect.getItems().addAll(JURUSAN_SMK_KESEHATAN);
        });

        Button btnAdd = new Button("➕ Tambah Manual");
        btnAdd.setOnAction(e -> {
            addManual();
            refreshTreeWithAnimation();
        });

        HBox manualBox = new HBox(8, nameField, groupSelect, jurusanSelect, tingkatSelect, btnAdd);
        manualBox.setPadding(new Insets(8));

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(28,28);

        // TreeTable setup
        treeTable = new TreeTableView<>();
        treeTable.setShowRoot(false);
        treeTable.setEditable(true);
        treeTable.setEffect(new DropShadow(4, Color.GRAY));

        TreeTableColumn<Student,String> noCol = new TreeTableColumn<>("No");
        noCol.setPrefWidth(50);
        noCol.setCellValueFactory(param -> {
            TreeItem<Student> item = param.getValue();
            if (item != null && item.getParent() != null && item.getParent().getParent() != null) {
                int index = item.getParent().getChildren().indexOf(item) + 1;
                return new SimpleStringProperty(String.valueOf(index));
            }
            return new SimpleStringProperty("");
        });

        TreeTableColumn<Student,String> nameCol = new TreeTableColumn<>("Nama");
        nameCol.setPrefWidth(320);
        nameCol.setCellValueFactory(c -> c.getValue().getValue().namaProperty());
        nameCol.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        nameCol.setOnEditCommit(evt -> {
            TreeItem<Student> treeItem = evt.getRowValue();
            if (treeItem == null || treeItem.getValue() == null) return;
            Student s = treeItem.getValue();
            String old = s.getNama();
            String neu = evt.getNewValue().trim();
            if (neu.isEmpty()) {
                showAlert("Peringatan", "Nama tidak boleh kosong", Alert.AlertType.WARNING);
                treeTable.refresh();
                return;
            }
            if (!old.equalsIgnoreCase(neu) && existingNames.contains(neu.toLowerCase())) {
                showAlert("Duplikat", "Nama sudah ada", Alert.AlertType.WARNING);
                treeTable.refresh();
                return;
            }
            // update existingNames safely
            existingNames.remove(old.toLowerCase());
            s.namaProperty().set(neu);
            existingNames.add(neu.toLowerCase());
            refreshTreeWithAnimation();
        });

        TreeTableColumn<Student,String> kelasCol = new TreeTableColumn<>("Kelas");
        kelasCol.setPrefWidth(80);
        kelasCol.setCellValueFactory(c -> c.getValue().getValue().kelasProperty());

        TreeTableColumn<Student,String> jurusanCol = new TreeTableColumn<>("Jurusan");
        jurusanCol.setPrefWidth(110);
        jurusanCol.setCellValueFactory(c -> c.getValue().getValue().jurusanProperty());

        treeTable.getColumns().addAll(noCol, nameCol, kelasCol, jurusanCol);

        // small CSS (applied via data URL). Encode to avoid spaces/newlines issues.
        String css = """
                .tree-table-row-cell:filled:selected, .tree-table-cell:selected {
                   -fx-selection-bar-text: black;
                }
                .tree-table-cell {
                   -fx-text-fill: black;
                }
                """;
        try {
            String encoded = URLEncoder.encode(css, StandardCharsets.UTF_8.toString());
            Scene dummy = new Scene(new Pane()); // temp scene to accept stylesheet format
            // We'll apply to the final scene below; keep variable for use.
        } catch (Exception ignored) {
            // ignore encoding errors; we'll fallback and not crash app.
        }

        // Row coloring per school and context menu for student nodes
        treeTable.setRowFactory(tv -> {
            TreeTableRow<Student> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(Student item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        String kelompok = item.getKelompok();
                        String base = switch (kelompok) {
                            case "SMK Airlangga" -> "#E8F2FF";
                            case "SMK Kesehatan Airlangga" -> "#F0FFF0";
                            default -> "#FFFFFF";
                        };
                        setStyle("-fx-background-color: " + base + "; -fx-text-fill: black; "
                                + "-fx-selection-bar: derive(" + base + ", -12%); -fx-selection-bar-text: black;");
                    }
                }
            };

            // Context menu: only for leaf student nodes
            row.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    TreeItem<Student> ti = row.getTreeItem();
                    if (ti != null && ti.isLeaf() && ti.getValue() != null && ti.getValue().getNama().length() > 0) {
                        ContextMenu cm = new ContextMenu();
                        MenuItem edit = new MenuItem("Edit Nama");
                        edit.setOnAction(a -> {
                            // ensure selection is set to this row
                            treeTable.getSelectionModel().select(row.getIndex());
                            treeTable.edit(row.getIndex(), nameCol);
                        });
                        MenuItem delete = new MenuItem("Hapus Siswa");
                        delete.setOnAction(a -> {
                            Student s = ti.getValue();
                            dataList.remove(s);
                            existingNames.remove(s.getNama().toLowerCase());
                            refreshTreeWithAnimation();
                        });
                        cm.getItems().addAll(edit, delete);
                        cm.show(row, ev.getScreenX(), ev.getScreenY());
                    }
                }
            });

            return row;
        });

        // Buttons
        Button btnSave = new Button("💾 Simpan");
        btnSave.setOnAction(e -> saveDataToFile(stage));
        addButtonEffects(btnSave);

        Button btnLoad = new Button("📂 Muat");
        btnLoad.setOnAction(e -> loadFromFile(stage));
        addButtonEffects(btnLoad);

        Button btnClear = new Button("🧹 Hapus Semua");
        btnClear.setOnAction(e -> {
            Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Hapus semua data?", ButtonType.YES, ButtonType.NO);
            c.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    dataList.clear();
                    existingNames.clear();
                    refreshTreeWithAnimation();
                }
            });
        });
        addButtonEffects(btnClear);

        Button btnRefresh = new Button("🔄 Segarkan");
        btnRefresh.setOnAction(e -> refreshTreeWithAnimation());
        addButtonEffects(btnRefresh);

        Button btnLoadExample = new Button("📝 Contoh Data");
        btnLoadExample.setOnAction(e -> {
            loadExampleData();
            refreshTreeWithAnimation();
        });
        addButtonEffects(btnLoadExample);

        countLabel = new Label("👥 Total siswa: 0");
        countLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        HBox topButtons = new HBox(8, btnProcess, btnSave, btnLoad, btnClear, btnRefresh, btnLoadExample, progressIndicator);
        topButtons.setPadding(new Insets(8));

        VBox root = new VBox(8,
                new Label("📥 Input Otomatis:"), inputArea,
                manualBox,
                topButtons,
                new Label("📊 Daftar Data Siswa"),
                treeTable,
                countLabel);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F7FFF7);");

        Scene scene = new Scene(root, 980, 640);

        // apply CSS directly as small inline stylesheet; encode to data URL to avoid IllegalArgumentException on some platforms
        try {
            String encodedCss = URLEncoder.encode(css, StandardCharsets.UTF_8.toString());
            scene.getStylesheets().add("data:text/css," + encodedCss);
        } catch (Exception ex) {
            // fallback: do not crash; css is cosmetic
        }

        // keyboard support: Enter to add manual if focus on nameField
        nameField.setOnKeyPressed(k -> {
            if (k.getCode() == KeyCode.ENTER) { addManual(); refreshTreeWithAnimation(); }
        });

        stage.setScene(scene);
        stage.show();
    }

    private void addButtonEffects(Button btn) {
        DropShadow ds = new DropShadow(6, Color.rgb(0,0,0,0.15));
        btn.setOnMouseEntered(e -> btn.setEffect(ds));
        btn.setOnMouseExited(e -> btn.setEffect(null));
        btn.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), btn);
            st.setFromX(1); st.setFromY(1); st.setToX(0.95); st.setToY(0.95);
            st.setAutoReverse(true); st.setCycleCount(2); st.play();
        });
    }

    // ---- Core functions ----

    private void addManual() {
        String nama = nameField.getText().trim();
        String sekolah = groupSelect.getValue();
        String jurusan = jurusanSelect.getValue();
        String kelas = tingkatSelect.getValue();

        if (nama.isEmpty() || sekolah == null || jurusan == null || kelas == null) {
            showAlert("Peringatan", "Lengkapi semua field!", Alert.AlertType.WARNING);
            return;
        }

        if (existingNames.contains(nama.toLowerCase())) {
            showAlert("Duplikat", "Nama sudah ada!", Alert.AlertType.WARNING);
            return;
        }

        Student s = new Student(nama, kelas, jurusan, sekolah);
        dataList.add(s);
        existingNames.add(nama.toLowerCase());
        nameField.clear();
        groupSelect.setValue(null);
        jurusanSelect.getItems().clear();
        tingkatSelect.setValue(null);
        updateCount();
    }

    private void processText() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            showAlert("Info", "Teks input kosong.", Alert.AlertType.INFORMATION);
            return;
        }

        // Support dashes (hyphen, en dash, em dash) and both "kelas" phrasing or explicit sekolah
        // Examples supported:
        // "Nama – 10 – AKL – SMK Airlangga"
        // "Nama, kelas 11 PPLG"
        // "Nama kelas 11 PPLG"
        Pattern pLine = Pattern.compile("(.+)");
        String[] lines = input.split("\\r?\\n");
        int added = 0;
        List<String> skipped = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // Normalize different dash characters to simple hyphen for splitting
            String norm = line.replaceAll("[\\u2013\\u2014]", "-");

            // Try split by dash groups first: "name - kelas - jurusan - sekolah" (4 parts)
            String[] parts = Arrays.stream(norm.split("\\s*-\\s*")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);

            String nama = null, kelas = null, jurusan = null, sekolah = null;

            if (parts.length >= 3) {
                // If last part looks like school (contains "SMK" or "Sekolah") accept it
                if (parts.length >= 4) {
                    nama = parts[0];
                    kelas = parts[1];
                    jurusan = parts[2];
                    sekolah = parts[3];
                } else {
                    // 3 parts: likely name - kelas - jurusan
                    nama = parts[0];
                    kelas = parts[1];
                    jurusan = parts[2];
                    sekolah = determineSekolah(jurusan); // maybe jurusan is one of known ones
                }
            } else {
                // try pattern with optional "kelas" word: "Name, kelas 11 PPLG" or "Name kelas 11 PPLG"
                Pattern p = Pattern.compile("([A-Za-z.'\\- ]+?)\\s*(?:,|)\\s*(?:kelas\\s*)?(\\d{1,2})\\s*([A-Za-z0-9]+)(?:\\s*-\\s*(.+))?", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(line);
                if (m.find()) {
                    nama = m.group(1).trim();
                    kelas = m.group(2).trim();
                    jurusan = m.group(3).trim().toUpperCase();
                    if (m.groupCount() >= 4 && m.group(4) != null) {
                        sekolah = m.group(4).trim();
                    } else {
                        sekolah = determineSekolah(jurusan);
                    }
                }
            }

            if (nama == null || kelas == null || jurusan == null) {
                skipped.add(line);
                continue;
            }

            // Normalize jurusan (upper)
            jurusan = jurusan.toUpperCase();

            if (sekolah == null || sekolah.isBlank()) sekolah = determineSekolah(jurusan);
            if (sekolah == null) sekolah = "Tidak Diketahui";

            String key = nama.toLowerCase();
            if (!existingNames.contains(key)) {
                dataList.add(new Student(nama, kelas, jurusan, sekolah));
                existingNames.add(key);
                added++;
            } // else duplicate -> skip silently (or we could add to skipped list)
        }

        if (added == 0) {
            showAlert("Info", "Tidak ada entri baru yang valid ditemukan.", Alert.AlertType.INFORMATION);
            if (!skipped.isEmpty()) {
                // optional: log skipped lines to console for debugging
                System.out.println("Skipped lines:");
                skipped.forEach(s -> System.out.println(" - " + s));
            }
        } else {
            showAlert("Sukses", "Berhasil menambahkan " + added + " entri.", Alert.AlertType.INFORMATION);
        }
        updateCount();
    }

    private String determineSekolah(String jurusan) {
        if (jurusan == null) return null;
        jurusan = jurusan.toUpperCase();
        if (JURUSAN_SMK_AIRLANGGA.contains(jurusan)) return "SMK Airlangga";
        if (JURUSAN_SMK_KESEHATAN.contains(jurusan)) return "SMK Kesehatan Airlangga";
        return null;
    }

    private void loadExampleData() {
        dataList.clear();
        existingNames.clear();
        List<Student> examples = List.of(
                new Student("Rina Safitri", "11", "PPLG", "SMK Airlangga"),
                new Student("Ahmad Fauzi", "10", "AKL", "SMK Airlangga"),
                new Student("Siti Nurhaliza", "12", "TJKT", "SMK Airlangga"),
                new Student("Budi Santoso", "11", "AKC", "SMK Kesehatan Airlangga"),
                new Student("Maya Sari", "10", "FKK", "SMK Kesehatan Airlangga"),
                new Student("Dika Pratama", "12", "TLM", "SMK Kesehatan Airlangga")
        );
        for (Student s : examples) {
            dataList.add(s);
            existingNames.add(s.getNama().toLowerCase());
        }
        updateCount();
    }

    private void refreshTreeWithAnimation() {
        if (treeTable == null) return;
        // build grouped map: Sekolah -> Jurusan -> Kelas -> List<Student>
        Map<String, Map<String, Map<String, List<Student>>>> grouped = new TreeMap<>();

        for (Student s : dataList) {
            grouped
                    .computeIfAbsent(s.getKelompok(), k -> new TreeMap<>())
                    .computeIfAbsent(s.getJurusan(), j -> new TreeMap<>(this::kelasComparator))
                    .computeIfAbsent(s.getKelas(), c -> new ArrayList<>())
                    .add(s);
        }

        TreeItem<Student> root = new TreeItem<>(new Student("", "", "", ""));
        root.setExpanded(true);

        for (var sekolahEntry : grouped.entrySet()) {
            String sekolah = sekolahEntry.getKey();
            var jurusanMap = sekolahEntry.getValue();

            int totalSekolah = jurusanMap.values().stream().flatMap(m -> m.values().stream()).mapToInt(List::size).sum();
            TreeItem<Student> sekolahNode = new TreeItem<>(new Student("🏫 " + sekolah + " (" + totalSekolah + ")", "", "", sekolah));
            sekolahNode.setExpanded(true);

            for (var jurusanEntry : jurusanMap.entrySet()) {
                String jurusan = jurusanEntry.getKey();
                var kelasMap = jurusanEntry.getValue();
                int totalJurusan = kelasMap.values().stream().mapToInt(List::size).sum();
                TreeItem<Student> jurusanNode = new TreeItem<>(new Student("📘 Jurusan: " + jurusan + " (" + totalJurusan + ")", "", jurusan, sekolah));
                jurusanNode.setExpanded(false);

                for (var kelasEntry : kelasMap.entrySet()) {
                    String kelas = kelasEntry.getKey();
                    List<Student> siswaList = kelasEntry.getValue();

                    TreeItem<Student> kelasNode = new TreeItem<>(new Student("🧑‍🎓 Kelas " + kelas + " (" + siswaList.size() + ")", kelas, jurusan, sekolah));
                    kelasNode.setExpanded(false);

                    // sort students alphabetically
                    siswaList.sort(Comparator.comparing(Student::getNama, String.CASE_INSENSITIVE_ORDER));
                    for (Student s : siswaList) {
                        kelasNode.getChildren().add(new TreeItem<>(s));
                    }
                    jurusanNode.getChildren().add(kelasNode);
                }
                sekolahNode.getChildren().add(jurusanNode);
            }
            root.getChildren().add(sekolahNode);
        }

        treeTable.setRoot(root);
        treeTable.refresh();

        FadeTransition ft = new FadeTransition(Duration.millis(400), treeTable);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        updateCount();
    }

    // comparator to order kelas numeric (10,11,12) then lexicographic fallback
    private int kelasComparator(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        try {
            int ia = Integer.parseInt(a);
            int ib = Integer.parseInt(b);
            return Integer.compare(ia, ib);
        } catch (NumberFormatException ex) {
            // fallback to custom order if possible
            int pa = KELAS_ORDER.indexOf(a);
            int pb = KELAS_ORDER.indexOf(b);
            if (pa >= 0 && pb >= 0) return Integer.compare(pa, pb);
            if (pa >= 0) return -1;
            if (pb >= 0) return 1;
            return a.compareTo(b);
        }
    }

    private void updateCount() {
        if (countLabel == null) return;
        long totalAirlangga = dataList.stream().filter(s -> "SMK Airlangga".equals(s.getKelompok())).count();
        long totalKesehatan = dataList.stream().filter(s -> "SMK Kesehatan Airlangga".equals(s.getKelompok())).count();
        countLabel.setText(String.format("👥 Total siswa: %d (SMK Airlangga: %d | SMK Kesehatan: %d)",
                dataList.size(), totalAirlangga, totalKesehatan));
        ScaleTransition st = new ScaleTransition(Duration.millis(250), countLabel);
        st.setFromX(1); st.setToX(1.06); st.setFromY(1); st.setToY(1.06);
        st.setCycleCount(2); st.setAutoReverse(true); st.play();
    }

    private void saveDataToFile(Stage stage) {
        if (dataList.isEmpty()) {
            showAlert("Peringatan", "Tidak ada data untuk disimpan", Alert.AlertType.WARNING);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan data siswa");
        chooser.setInitialFileName("data_siswa.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (Student s : dataList) {
                // CSV: nama,kelas,jurusan,kelompok
                bw.write(escapeCsv(s.getNama()) + "," + escapeCsv(s.getKelas()) + "," + escapeCsv(s.getJurusan()) + "," + escapeCsv(s.getKelompok()));
                bw.newLine();
            }
            showAlert("Sukses", "Data tersimpan: " + file.getName(), Alert.AlertType.INFORMATION);
        } catch (IOException ex) {
            showAlert("Error", "Gagal menyimpan: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadFromFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Muat data siswa");
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int added = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsv(line);
                if (parts == null || parts.length < 4) continue;
                String nama = parts[0].trim();
                String kelas = parts[1].trim();
                String jurusan = parts[2].trim();
                String kelompok = parts[3].trim();
                if (!existingNames.contains(nama.toLowerCase())) {
                    dataList.add(new Student(nama, kelas, jurusan, kelompok));
                    existingNames.add(nama.toLowerCase());
                    added++;
                }
            }
            if (added == 0) showAlert("Info", "Tidak ada entri baru yang dimuat.", Alert.AlertType.INFORMATION);
            else showAlert("Sukses", "Berhasil memuat " + added + " entri.", Alert.AlertType.INFORMATION);
            refreshTreeWithAnimation();
        } catch (IOException ex) {
            showAlert("Error", "Gagal memuat file: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) s = "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String[] splitCsv(String line) {
        if (line == null) return new String[0];
        // simple CSV split honoring quotes
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' ) {
                if (inQuote && i+1 < line.length() && line.charAt(i+1) == '"') {
                    cur.append('"'); i++; // escaped quote
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private void showProgress(boolean show) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ---- Student class ----
    public static class Student {
        private final StringProperty nama;
        private final StringProperty kelas;
        private final StringProperty jurusan;
        private final StringProperty kelompok;

        public Student(String nama, String kelas, String jurusan, String kelompok) {
            this.nama = new SimpleStringProperty(nama == null ? "" : nama);
            this.kelas = new SimpleStringProperty(kelas == null ? "" : kelas);
            this.jurusan = new SimpleStringProperty(jurusan == null ? "" : jurusan);
            this.kelompok = new SimpleStringProperty(kelompok == null ? "" : kelompok);
        }

        public String getNama() { return nama.get(); }
        public String getKelas() { return kelas.get(); }
        public String getJurusan() { return jurusan.get(); }
        public String getKelompok() { return kelompok.get(); }

        public StringProperty namaProperty() { return nama; }
        public StringProperty kelasProperty() { return kelas; }
        public StringProperty jurusanProperty() { return jurusan; }
        public StringProperty kelompokProperty() { return kelompok; }
    }
}
