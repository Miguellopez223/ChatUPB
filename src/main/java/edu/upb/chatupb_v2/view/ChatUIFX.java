package edu.upb.chatupb_v2.view;

import edu.upb.chatupb_v2.controller.ChatController;
import edu.upb.chatupb_v2.controller.ContactController;
import edu.upb.chatupb_v2.model.entities.User;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ChatUIFX extends Application implements IChatView {

    // ================================================================
    // CONTROLLERS
    // ================================================================
    private ChatController chatController;
    private ContactController contactController;
    private Stage stage;

    // ================================================================
    // UI COMPONENTS
    // ================================================================
    private TextField txtIpDestino;
    private TextField txtMensaje;
    private Label lblEstado;
    private Label lblEstadoIcon;
    private ComboBox<User> userComboBox;
    private ComboBox<String> comboTemas;
    private ListView<ContactInfo> listaContactos;
    private VBox chatMessagesBox;
    private ScrollPane scrollChat;
    private Label lblUserName;
    private Label lblChatTitle;
    private ToggleSwitch toggleNudge;
    private ToggleSwitch toggleSelfDestruct;
    private Button btnSend;
    private Button btnSendInvite;
    private VBox defaultChatView;
    private HBox statusBox;

    // Pinned message bar
    private HBox pinnedMessageBar;
    private Label pinnedMessageLabel;
    private String pinnedMessageId;

    // ================================================================
    // STATE
    // ================================================================
    private volatile List<ContactInfo> contactosEnMemoria = new ArrayList<>();
    private volatile String contactoActivo = null;
    private final HashMap<String, VBox> chatPanels = new HashMap<>();
    private final HashMap<String, Label> checkLabels = new HashMap<>();
    private final HashMap<String, Label> messageLabels = new HashMap<>();
    private final HashMap<String, VBox> bubblePanels = new HashMap<>();
    private final HashMap<String, Label> pinLabels = new HashMap<>();
    private final HashMap<String, Boolean> bubbleIsMine = new HashMap<>();
    private final HashSet<String> viewOnceIds = new HashSet<>();
    private final HashMap<String, String> temasContacto = new HashMap<>();
    private boolean suppressTemaEvent = false;

    // ================================================================
    // COLORS
    // ================================================================
    private static final String TEAL = "#3AAFA9";
    private static final String CORAL = "#E76F51";
    private static final String BG = "#ECEFF1";
    private static final String CARD = "#FFFFFF";
    private static final String TXT_DARK = "#2B2B2B";
    private static final String TXT_MED = "#555555";
    private static final String TXT_LIGHT = "#8E8E8E";
    private static final String GREEN = "#4CAF50";

    // ================================================================
    // LIFECYCLE
    // ================================================================

    @Override
    public void init() {
        chatController = new ChatController(this);
        contactController = new ContactController(this);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        buildUI(primaryStage);
        primaryStage.show();
        chatController.onAppStart();
    }

    public ChatController getChatController() {
        return chatController;
    }

    // ================================================================
    // UI CONSTRUCTION
    // ================================================================

    private void buildUI(Stage stage) {
        stage.setTitle("ChatUPB v2");
        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.out.println("[ChatUIFX] No se pudo cargar el icono de la app: " + e.getMessage());
        }

        VBox leftPanel = buildLeftPanel();
        leftPanel.setPrefWidth(280);
        leftPanel.setMinWidth(260);
        leftPanel.setMaxWidth(300);

        VBox rightPanel = buildRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        HBox root = new HBox(14, leftPanel, rightPanel);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + BG + ";");

        Scene scene = new Scene(root, 1060, 720);
        stage.setScene(scene);
    }

    private String cardStyle() {
        return "-fx-background-color: " + CARD + "; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 2);";
    }

    // --- LEFT PANEL ---

    private VBox buildLeftPanel() {
        VBox panel = new VBox(14);
        VBox profileCard = buildProfileCard();
        VBox contactsCard = buildContactsCard();
        VBox.setVgrow(contactsCard, Priority.ALWAYS);
        panel.getChildren().addAll(profileCard, contactsCard);
        return panel;
    }

    private void mostrarAcercaDe() {
        Stage aboutStage = new Stage();
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.initOwner(stage);
        aboutStage.setTitle("Acerca De - ChatUPB v2");
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            aboutStage.getIcons().add(icon);
        } catch (Exception ignored) {}

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30, 40, 30, 40));
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #FAFFFE, #E8F6F3);");

        // Logo
        try {
            Image logoImg = new Image(getClass().getResourceAsStream("/images/logo.png"));
            ImageView logoView = new ImageView(logoImg);
            logoView.setFitWidth(180);
            logoView.setFitHeight(180);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            content.getChildren().add(logoView);
        } catch (Exception e) {
            Label logoFallback = new Label("ChatUPB");
            logoFallback.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2B7A78;");
            content.getChildren().add(logoFallback);
        }

        // Titulo
        Label titulo = new Label("ChatUPB v2");
        titulo.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #17252A;");

        Label version = new Label("Version 1.0.0");
        version.setStyle("-fx-font-size: 13px; -fx-text-fill: #3AAFA9; -fx-font-weight: bold;");

        // Separador
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(280);
        sep.setStyle("-fx-background-color: #3AAFA9;");

        // Descripcion
        Label desc = new Label("Aplicación de Chat P2P");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #5C5C5C; -fx-text-alignment: center;");
        desc.setWrapText(true);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Info academica
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setStyle("-fx-background-color: rgba(58,175,169,0.08); -fx-background-radius: 10; -fx-padding: 14;");

        Label creador = new Label("\uD83D\uDC68\u200D\uD83D\uDCBB  Creado por: Miguel Angel Lopez Arispe");
        creador.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #17252A;");

        Label materia = new Label("\uD83D\uDCDA  Materia: Patrones de Diseño");
        materia.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        Label docente = new Label("\uD83D\uDC68\u200D\uD83C\uDFEB  Docente: Ing. Ricardo Laredo");
        docente.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        Label universidad = new Label("\uD83C\uDFEB  Universidad Privada Boliviana");
        universidad.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        Label semestre = new Label("\uD83D\uDCC5  5to Semestre - 2026");
        semestre.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        Label fecha = new Label("\u2705  Finalizado: 23 de Marzo 2026");
        fecha.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        infoBox.getChildren().addAll(creador, materia, docente, universidad, semestre, fecha);

        // Tecnologias
        Label techTitle = new Label("Tecnologías utilizadas");
        techTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2B7A78;");

        Label techs = new Label("Java 21  \u2022  JavaFX 21  \u2022  SQLite  \u2022  TCP Sockets  \u2022  Maven");
        techs.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");

        // Boton cerrar
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle("-fx-background-color: #3AAFA9; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 20; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> aboutStage.close());
        cerrarBtn.setOnMouseEntered(e -> cerrarBtn.setStyle("-fx-background-color: #2B7A78; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 20; -fx-cursor: hand;"));
        cerrarBtn.setOnMouseExited(e -> cerrarBtn.setStyle("-fx-background-color: #3AAFA9; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 20; -fx-cursor: hand;"));

        content.getChildren().addAll(titulo, version, sep, desc, infoBox, techTitle, techs, cerrarBtn);

        Scene aboutScene = new Scene(content, 420, 620);
        aboutStage.setScene(aboutScene);
        aboutStage.setResizable(false);
        aboutStage.showAndWait();
    }

    private VBox buildProfileCard() {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 16, 16, 16));
        card.setStyle(cardStyle());

        // Menu button
        MenuButton menuBtn = new MenuButton("\u22EE");
        menuBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: #888; -fx-padding: 0;");
        MenuItem newUserItem = new MenuItem("Nuevo Usuario");
        newUserItem.setOnAction(e -> chatController.crearNuevoUsuario());
        MenuItem acercaDeItem = new MenuItem("Acerca De");
        acercaDeItem.setOnAction(e -> mostrarAcercaDe());
        menuBtn.getItems().addAll(newUserItem, new SeparatorMenuItem(), acercaDeItem);

        HBox topRow = new HBox();
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        topRow.getChildren().addAll(sp, menuBtn);

        // Avatar
        StackPane avatarStack = new StackPane();
        Circle outerRing = new Circle(40);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web(TEAL));
        outerRing.setStrokeWidth(2.5);
        Circle inner = new Circle(37);
        inner.setFill(Color.web("#DEF2F1"));
        Label avatarIcon = new Label("\uD83D\uDC64");
        avatarIcon.setStyle("-fx-font-size: 28px;");
        avatarStack.getChildren().addAll(outerRing, inner, avatarIcon);

        // Green status dot at bottom-right of avatar
        StackPane avatarWrapper = new StackPane();
        avatarWrapper.setMaxSize(90, 90);
        Circle statusDot = new Circle(7, Color.web(GREEN));
        statusDot.setStroke(Color.WHITE);
        statusDot.setStrokeWidth(2.5);
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        statusDot.setTranslateX(-8);
        statusDot.setTranslateY(-4);
        avatarWrapper.getChildren().addAll(avatarStack, statusDot);

        // Name
        lblUserName = new Label("Usuario");
        lblUserName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");
        Label subtitle = new Label("\uD83D\uDC65 Nuevo Usuario");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TXT_LIGHT + ";");

        // User combo
        userComboBox = new ComboBox<>();
        userComboBox.setMaxWidth(200);
        userComboBox.setPrefWidth(200);
        userComboBox.setStyle("-fx-font-size: 11px;");
        userComboBox.setOnAction(e -> {
            User sel = userComboBox.getValue();
            if (sel != null) {
                chatController.cambiarUsuario(sel);
                lblUserName.setText(sel.getName());
            }
        });

        card.getChildren().addAll(topRow, avatarWrapper, lblUserName, subtitle, userComboBox);
        return card;
    }

    private VBox buildContactsCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 0, 10, 0));
        card.setStyle(cardStyle());

        Label title = new Label("Contactos");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");
        title.setPadding(new Insets(0, 0, 0, 16));

        listaContactos = new ListView<>();
        listaContactos.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        listaContactos.setFixedCellSize(55);

        listaContactos.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ContactInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox cell = new HBox(8);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    cell.setPadding(new Insets(6, 12, 6, 0));

                    Region accent = new Region();
                    accent.setMinWidth(4);
                    accent.setMaxWidth(4);
                    accent.setMinHeight(38);
                    boolean conn = chatController.isConectado(item.getIp());
                    accent.setStyle("-fx-background-color: " + (conn ? GREEN : "#E53935") + "; -fx-background-radius: 2;");

                    VBox textBox = new VBox(2);
                    Label nameL = new Label(item.getName());
                    nameL.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");
                    Label ipL = new Label(item.getIp());
                    ipL.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TXT_LIGHT + ";");
                    textBox.getChildren().addAll(nameL, ipL);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    cell.getChildren().addAll(accent, textBox, spacer);

                    // Punto verde solo si hay mensaje pendiente (007 recibido, sin 008 enviado)
                    if (chatController.tieneMensajePendiente(item.getIp())) {
                        Circle dot = new Circle(5);
                        dot.setFill(Color.web(GREEN));
                        cell.getChildren().add(dot);
                    }
                    setGraphic(cell);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                }
            }
        });

        // Double click to open chat
        listaContactos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ContactInfo selected = listaContactos.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    chatController.abrirChat(selected);
                }
            }
        });

        // Single click sets IP
        listaContactos.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                txtIpDestino.setText(newVal.getIp());
            }
        });

        // Context menu
        ContextMenu contactMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Eliminar contacto");
        deleteItem.setOnAction(e -> {
            ContactInfo sel = listaContactos.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "\u00BFEliminar a " + sel.getName() + "?", ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Confirmar");
                confirm.initOwner(stage);
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        chatController.eliminarContacto(sel.getId(), sel.getIp());
                    }
                });
            }
        });
        contactMenu.getItems().add(deleteItem);
        listaContactos.setContextMenu(contactMenu);

        VBox.setVgrow(listaContactos, Priority.ALWAYS);
        card.getChildren().addAll(title, listaContactos);
        return card;
    }

    // --- RIGHT PANEL ---

    private VBox buildRightPanel() {
        VBox panel = new VBox(14);
        VBox networkCard = buildNetworkCard();
        VBox chatCard = buildChatCard();
        VBox.setVgrow(chatCard, Priority.ALWAYS);
        VBox composerCard = buildComposerCard();
        panel.getChildren().addAll(networkCard, chatCard, composerCard);
        return panel;
    }

    private VBox buildNetworkCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle(cardStyle());

        Label title = new Label("Conexi\u00F3n de Red");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");

        // Controls row
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox ipSection = new VBox(2);
        Label ipLabel = new Label("IP Destino");
        ipLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + TXT_LIGHT + ";");
        txtIpDestino = new TextField("192.168.100.8");
        txtIpDestino.setPrefWidth(200);
        txtIpDestino.setStyle("-fx-font-size: 13px; -fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #CCC; -fx-border-width: 1; -fx-padding: 8 12;");
        ipSection.getChildren().addAll(ipLabel, txtIpDestino);

        btnSendInvite = new Button("Enviar Invitaci\u00F3n");
        btnSendInvite.setStyle("-fx-background-color: " + TEAL + "; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        btnSendInvite.setOnAction(e -> enviarInvitacion());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(8, 16, 8, 16));
        statusBox.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 8;");

        lblEstadoIcon = new Label("\u26A0");
        lblEstadoIcon.setStyle("-fx-font-size: 14px;");
        lblEstado = new Label("Sin conexiones");
        lblEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TXT_MED + "; -fx-font-weight: bold;");
        statusBox.getChildren().addAll(lblEstadoIcon, lblEstado);

        row.getChildren().addAll(ipSection, btnSendInvite, spacer, statusBox);
        card.getChildren().addAll(title, row);
        return card;
    }

    private VBox buildChatCard() {
        VBox card = new VBox(0);
        card.setStyle(cardStyle());

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 18, 8, 18));

        lblChatTitle = new Label("Historial de Chat");
        lblChatTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label temaLabel = new Label("Tema:");
        temaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TXT_MED + ";");
        comboTemas = new ComboBox<>();
        comboTemas.getItems().addAll("1-Defecto", "2-Azul", "3-Rojo", "4-Amarillo", "5-Violeta");
        comboTemas.setValue("1-Defecto");
        comboTemas.setDisable(true);
        comboTemas.setStyle("-fx-font-size: 11px;");
        comboTemas.setOnAction(e -> {
            if (suppressTemaEvent || contactoActivo == null) return;
            String sel = comboTemas.getValue();
            if (sel == null) return;
            String idTema = sel.substring(0, 1);
            chatController.enviarCambioTema(contactoActivo, idTema);
        });

        header.getChildren().addAll(lblChatTitle, sp, temaLabel, comboTemas);

        // Pinned message bar
        pinnedMessageBar = new HBox(8);
        pinnedMessageBar.setAlignment(Pos.CENTER_LEFT);
        pinnedMessageBar.setPadding(new Insets(8, 14, 8, 14));
        pinnedMessageBar.setStyle("-fx-background-color: #E0F2F1; -fx-border-color: #80CBC4; -fx-border-width: 0 0 1 0;");
        pinnedMessageBar.setVisible(false);
        pinnedMessageBar.setManaged(false);

        Label pinIcon = new Label("\uD83D\uDCCC");
        pinIcon.setStyle("-fx-font-size: 13px;");
        pinnedMessageLabel = new Label("");
        pinnedMessageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        HBox.setHgrow(pinnedMessageLabel, Priority.ALWAYS);
        Button unpinBtn = new Button("\u2715");
        unpinBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 12px; -fx-text-fill: #888; -fx-cursor: hand;");
        unpinBtn.setOnAction(e -> {
            if (contactoActivo != null && pinnedMessageId != null) {
                chatController.desfijarMensaje(contactoActivo, pinnedMessageId);
            }
        });
        pinnedMessageBar.getChildren().addAll(pinIcon, pinnedMessageLabel, unpinBtn);

        // Default view
        defaultChatView = new VBox(4);
        defaultChatView.setAlignment(Pos.TOP_CENTER);
        defaultChatView.setPadding(new Insets(20));
        Label defaultLabel = new Label("Selecciona un contacto para chatear");
        defaultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TXT_LIGHT + ";");
        defaultChatView.getChildren().add(defaultLabel);

        // Scroll
        scrollChat = new ScrollPane(defaultChatView);
        scrollChat.setFitToWidth(true);
        scrollChat.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollChat.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollChat.setStyle("-fx-background-color: transparent; -fx-background: #F0F0F0;");
        VBox.setVgrow(scrollChat, Priority.ALWAYS);

        card.getChildren().addAll(header, pinnedMessageBar, scrollChat);
        return card;
    }

    private VBox buildComposerCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.setStyle(cardStyle());

        Label title = new Label("Escribir Mensaje");
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");

        HBox controls = new HBox(14);
        controls.setAlignment(Pos.CENTER);

        txtMensaje = new TextField();
        txtMensaje.setPromptText("Escribe un mensaje...");
        txtMensaje.setStyle("-fx-font-size: 13px; -fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #CCC; -fx-border-width: 1; -fx-padding: 10 14;");
        txtMensaje.setOnAction(e -> enviarAccion());
        HBox.setHgrow(txtMensaje, Priority.ALWAYS);

        // Nudge
        VBox nudgeBox = new VBox(2);
        nudgeBox.setAlignment(Pos.CENTER);
        Label nudgeL = new Label("Zumbido");
        nudgeL.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");
        toggleNudge = new ToggleSwitch(false);
        Label nudgeSub = new Label("Enviar alerta");
        nudgeSub.setStyle("-fx-font-size: 9px; -fx-text-fill: " + TXT_LIGHT + ";");
        nudgeBox.getChildren().addAll(nudgeL, toggleNudge, nudgeSub);

        // Self-Destruct
        VBox sdBox = new VBox(2);
        sdBox.setAlignment(Pos.CENTER);
        Label sdL = new Label("Autodestruir");
        sdL.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TXT_DARK + ";");
        toggleSelfDestruct = new ToggleSwitch(false);
        Label sdSub = new Label("Ver 1 Vez");
        sdSub.setStyle("-fx-font-size: 9px; -fx-text-fill: " + TXT_LIGHT + ";");
        sdBox.getChildren().addAll(sdL, toggleSelfDestruct, sdSub);

        // Send button
        btnSend = new Button("Enviar");
        btnSend.setDisable(true);
        btnSend.setStyle("-fx-background-color: " + CORAL + "; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 30; -fx-cursor: hand;");
        btnSend.setOnAction(e -> enviarAccion());

        controls.getChildren().addAll(txtMensaje, nudgeBox, sdBox, btnSend);
        card.getChildren().addAll(title, controls);
        return card;
    }

    // ================================================================
    // EVENT HANDLERS
    // ================================================================

    private void enviarAccion() {
        if (contactoActivo == null) return;

        if (toggleNudge.isOn()) {
            chatController.enviarZumbido(contactoActivo);
            return;
        }

        String msg = txtMensaje.getText().trim();
        if (msg.isEmpty()) return;

        if (toggleSelfDestruct.isOn()) {
            chatController.enviarMensajeUnico(contactoActivo, msg);
        } else {
            chatController.enviarMensaje(contactoActivo, msg);
        }
    }

    private void enviarInvitacion() {
        String ip = txtIpDestino.getText().trim();
        User currentUser = userComboBox.getValue();
        if (currentUser == null) {
            mostrarError("Por favor, selecciona un usuario.");
            return;
        }
        chatController.enviarInvitacion(ip, currentUser.getName());
    }

    // ================================================================
    // THREAD HELPERS
    // ================================================================

    private void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private <T> T runOnFxBlocking(Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            result.set(supplier.get());
            latch.countDown();
        });
        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result.get();
    }

    private void runOnFxBlockingVoid(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                r.run();
                latch.countDown();
            });
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // ================================================================
    // IChatView IMPLEMENTATION
    // ================================================================

    @Override
    public void setUserList(List<User> users) {
        runOnFx(() -> {
            userComboBox.getItems().clear();
            userComboBox.getItems().addAll(users);
            if (!users.isEmpty()) {
                userComboBox.setValue(users.get(0));
                lblUserName.setText(users.get(0).getName());
            }
        });
    }

    @Override
    public void showNewUserDialog() {
        runOnFxBlockingVoid(() -> {
            String name = null;
            while (name == null || name.trim().isEmpty() || name.length() > 60) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Configuraci\u00F3n Inicial");
                dialog.setHeaderText("Bienvenido a ChatUPB v2.\nPor favor ingresa tu nombre (max 60 caracteres):");
                dialog.setContentText("Nombre:");
                dialog.initOwner(stage);

                Optional<String> result = dialog.showAndWait();
                if (result.isEmpty()) {
                    if (userComboBox.getItems().isEmpty()) {
                        Platform.exit();
                        System.exit(0);
                    }
                    return;
                }
                name = result.get();
                if (name.length() > 60) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "El nombre es muy largo (max 60).");
                    alert.initOwner(stage);
                    alert.showAndWait();
                    name = null;
                }
            }
            chatController.guardarNuevoUsuario(name.trim());
        });
    }

    @Override
    public void setScreenTitle(String title) {
        runOnFx(() -> stage.setTitle(title));
    }

    @Override
    public void onLoad(List<ContactInfo> contactos) {
        runOnFx(() -> {
            contactosEnMemoria = contactos;
            listaContactos.getItems().setAll(contactos);
        });
    }

    @Override
    public void appendChat(String texto) {
        runOnFx(() -> {
            String clean = texto.trim();
            if (clean.isEmpty()) return;
            Label lbl = new Label(clean);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: gray; -fx-font-style: italic;");
            lbl.setWrapText(true);
            defaultChatView.getChildren().add(lbl);
        });
    }

    @Override
    public void appendChatToContact(String ip, String texto) {
        runOnFx(() -> {
            String clean = texto.trim();
            if (clean.isEmpty()) return;
            VBox panel = getOrCreateChatPanel(ip);
            addSystemLabelFX(panel, clean);
            if (ip.equals(contactoActivo)) {
                scrollChat.setContent(panel);
                scrollToBottom();
            }
        });
    }

    @Override
    public void appendMensajeToContact(String ip, String content, boolean isMine, String idMensaje, boolean viewOnce) {
        runOnFx(() -> {
            VBox panel = getOrCreateChatPanel(ip);
            String time = new SimpleDateFormat("HH:mm").format(new Date());
            addBubbleFX(panel, content, time, isMine, false, idMensaje, false, viewOnce);
            if (ip.equals(contactoActivo)) {
                scrollChat.setContent(panel);
                scrollToBottom();
            }
        });
    }

    @Override
    public void actualizarCheckMensaje(String ip, String idMensaje) {
        runOnFx(() -> {
            Label checkLabel = checkLabels.get(idMensaje);
            if (checkLabel != null) {
                checkLabel.setText("\u2713\u2713");
                checkLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3593EA;");
            }
        });
    }

    @Override
    public void actualizarBurbujaMensajeEliminado(String ip, String idMensaje) {
        runOnFx(() -> {
            boolean esViewOnce = viewOnceIds.contains(idMensaje);
            String textoEliminado = esViewOnce
                    ? "\uD83D\uDEAB Mensaje \u00FAnico abierto"
                    : "\uD83D\uDEAB Este mensaje fue eliminado";

            // Update message label
            Label msgLabel = messageLabels.get(idMensaje);
            if (msgLabel != null) {
                msgLabel.setText(textoEliminado);
                msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8c8c8c; -fx-font-style: italic;");
                msgLabel.setContextMenu(null);
            }

            // Update bubble that may contain a ViewOnce button
            VBox bubble = bubblePanels.get(idMensaje);
            if (bubble != null) {
                // Check for ViewOnce button and replace it
                List<Node> children = new ArrayList<>(bubble.getChildren());
                for (Node child : children) {
                    if (child instanceof Button) {
                        bubble.getChildren().remove(child);
                        Label lbl = new Label("\uD83D\uDEAB Mensaje \u00FAnico abierto");
                        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #8c8c8c; -fx-font-style: italic; -fx-padding: 4 0 2 0;");
                        bubble.getChildren().add(0, lbl);
                    }
                }
                bubble.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 14; " +
                        "-fx-border-color: #E0E0E0; -fx-border-radius: 14; -fx-border-width: 0.5; -fx-padding: 8 12 4 12;");
                bubble.setOnContextMenuRequested(null);
            }

            // Remove check label
            Label checkLabel = checkLabels.remove(idMensaje);
            if (checkLabel != null && checkLabel.getParent() instanceof HBox parent) {
                parent.getChildren().remove(checkLabel);
            }
        });
    }

    @Override
    public void abrirChatConContacto(ContactInfo contacto, List<ChatMessageInfo> historial) {
        runOnFx(() -> {
            contactoActivo = contacto.getIp();

            VBox panel = getOrCreateChatPanel(contacto.getIp());
            panel.getChildren().clear();

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            for (ChatMessageInfo msg : historial) {
                String time;
                try {
                    time = sdf.format(new Date(Long.parseLong(msg.getTimestamp())));
                } catch (NumberFormatException e) {
                    time = msg.getTimestamp() != null ? msg.getTimestamp() : "";
                }
                addBubbleFX(panel, msg.getContent(), time, msg.isMine(), msg.isConfirmed(),
                        msg.getId(), msg.isPinned(), msg.isViewOnce());
            }

            scrollChat.setContent(panel);
            lblChatTitle.setText("Chat con " + contacto.getName());

            btnSend.setDisable(false);
            comboTemas.setDisable(false);

            // Set theme combo without firing event
            String temaActual = temasContacto.getOrDefault(contacto.getIp(), "1");
            suppressTemaEvent = true;
            comboTemas.setValue(comboTemas.getItems().get(Integer.parseInt(temaActual) - 1));
            suppressTemaEvent = false;

            // Update pinned bar colors
            String[] colores = getColoresTemaHex(temaActual);
            pinnedMessageBar.setStyle("-fx-background-color: " + colores[4] + "; -fx-border-color: " + colores[5] + "; -fx-border-width: 0 0 1 0;");

            scrollToBottom();
        });
    }

    @Override
    public void clearChatHistory() {
        runOnFx(() -> {
            chatPanels.clear();
            checkLabels.clear();
            messageLabels.clear();
            bubblePanels.clear();
            pinLabels.clear();
            bubbleIsMine.clear();
            viewOnceIds.clear();
            temasContacto.clear();
            pinnedMessageId = null;
            pinnedMessageBar.setVisible(false);
            pinnedMessageBar.setManaged(false);

            scrollChat.setContent(defaultChatView);
            lblChatTitle.setText("Historial de Chat");
            contactoActivo = null;
        });
    }

    @Override
    public void mostrarMensajeFijado(String ip, ChatMessageInfo mensaje) {
        runOnFx(() -> {
            if (!ip.equals(contactoActivo)) return;
            if (mensaje == null || mensaje.getContent() == null || mensaje.getContent().isEmpty()) {
                ocultarMensajeFijadoInternal(ip);
                return;
            }
            pinnedMessageId = mensaje.getId();
            String textoTruncado = mensaje.getContent();
            if (textoTruncado.length() > 80) {
                textoTruncado = textoTruncado.substring(0, 80) + "...";
            }
            pinnedMessageLabel.setText(textoTruncado);
            pinnedMessageBar.setVisible(true);
            pinnedMessageBar.setManaged(true);
        });
    }

    @Override
    public void ocultarMensajeFijado(String ip) {
        runOnFx(() -> ocultarMensajeFijadoInternal(ip));
    }

    private void ocultarMensajeFijadoInternal(String ip) {
        if (!ip.equals(contactoActivo)) return;
        pinnedMessageId = null;
        pinnedMessageBar.setVisible(false);
        pinnedMessageBar.setManaged(false);
    }

    @Override
    public void marcarBurbujaFijada(String ip, String idMensaje) {
        runOnFx(() -> {
            if (pinLabels.containsKey(idMensaje)) return;

            VBox bubble = bubblePanels.get(idMensaje);
            if (bubble != null && bubble.getChildren().size() > 0) {
                // Find the bottom HBox (last child or the time row)
                Node lastChild = bubble.getChildren().get(bubble.getChildren().size() - 1);
                if (lastChild instanceof HBox bottomRow) {
                    Label pinLabel = new Label("\uD83D\uDCCC");
                    pinLabel.setStyle("-fx-font-size: 10px;");
                    bottomRow.getChildren().add(0, pinLabel);
                    pinLabels.put(idMensaje, pinLabel);
                }
            }
        });
    }

    @Override
    public void desmarcarBurbujaFijada(String ip, String idMensaje) {
        runOnFx(() -> {
            Label pinLabel = pinLabels.remove(idMensaje);
            if (pinLabel != null && pinLabel.getParent() instanceof HBox parent) {
                parent.getChildren().remove(pinLabel);
            }
        });
    }

    @Override
    public boolean mostrarDialogoInvitacion(String nombre, String ip) {
        return runOnFxBlocking(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Invitaci\u00F3n Recibida (Trama 001)");
            alert.setHeaderText("Invitaci\u00F3n de " + nombre);
            alert.setContentText("El usuario '" + nombre + "' (" + ip + ") te ha enviado una invitaci\u00F3n.\n\u00BFAceptas conectarte?");
            alert.initOwner(stage);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        });
    }

    @Override
    public void mostrarError(String mensaje) {
        runOnFx(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
            alert.setTitle("Error");  // "Error" es igual en espa\u00F1ol
            alert.initOwner(stage);
            alert.showAndWait();
        });
    }

    @Override
    public void agregarConexionUI(String ip, String nombre) {
        runOnFx(() -> listaContactos.refresh());
    }

    @Override
    public void limpiarConexionesUI() {
        runOnFx(() -> {
            actualizarEstadoInternal(0);
            listaContactos.refresh();
        });
    }

    @Override
    public void actualizarEstado(int numConexiones) {
        runOnFx(() -> actualizarEstadoInternal(numConexiones));
    }

    private void actualizarEstadoInternal(int numConexiones) {
        if (numConexiones == 0) {
            lblEstado.setText("Sin conexiones activas");
            lblEstadoIcon.setText("\u26A0");
            statusBox.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 8;");
            if (contactoActivo == null) {
                btnSend.setDisable(true);
            }
        } else {
            lblEstado.setText("Conectado (" + numConexiones + " conexi\u00F3n" + (numConexiones > 1 ? "es" : "") + " activa" + (numConexiones > 1 ? "s" : "") + ")");
            lblEstadoIcon.setText("\u2705");
            statusBox.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 8;");
            btnSend.setDisable(false);
        }
    }

    @Override
    public void refrescarEstadoContactos() {
        runOnFx(() -> listaContactos.refresh());
    }

    @Override
    public void actualizarEstadoInvitacion(String ip) {
        runOnFx(() -> {
            lblEstado.setText("Invitaci\u00F3n enviada a " + ip + "...");
            lblEstadoIcon.setText("\u27A4");
            statusBox.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 8;");
        });
    }

    @Override
    public void limpiarMensaje() {
        runOnFx(() -> txtMensaje.setText(""));
    }

    @Override
    public void mostrarZumbido(String ip, String nombreContacto) {
        runOnFx(() -> {
            VBox panel = getOrCreateChatPanel(ip);
            addSystemLabelFX(panel, "\uD83D\uDCA5 " + nombreContacto + " ha enviado un zumbido!");
            if (ip.equals(contactoActivo)) {
                scrollChat.setContent(panel);
                scrollToBottom();
            }

            // Shake effect
            double origX = stage.getX();
            double origY = stage.getY();
            Random rand = new Random();
            Timeline timeline = new Timeline();
            for (int i = 0; i < 20; i++) {
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * 25), e -> {
                    stage.setX(origX + rand.nextInt(17) - 8);
                    stage.setY(origY + rand.nextInt(17) - 8);
                }));
            }
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(500), e -> {
                stage.setX(origX);
                stage.setY(origY);
            }));
            timeline.play();

            try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
        });
    }

    @Override
    public String getMiNombre() {
        if (Platform.isFxApplicationThread()) {
            User u = userComboBox.getValue();
            return u != null ? u.getName() : "";
        }
        return runOnFxBlocking(() -> {
            User u = userComboBox.getValue();
            return u != null ? u.getName() : "";
        });
    }

    @Override
    public void limpiarChatDeContacto(String ip) {
        runOnFx(() -> {
            chatPanels.remove(ip);
            // Limpiar referencias de burbujas de este contacto
            // Si el chat eliminado era el activo, volver a la vista por defecto
            if (ip.equals(contactoActivo)) {
                contactoActivo = null;
                scrollChat.setContent(defaultChatView);
                lblChatTitle.setText("Historial de Chat");
                btnSend.setDisable(true);
                comboTemas.setDisable(true);
                pinnedMessageBar.setVisible(false);
                pinnedMessageBar.setManaged(false);
                pinnedMessageId = null;
            }
            temasContacto.remove(ip);
        });
    }

    @Override
    public String getContactoActivo() {
        return contactoActivo;
    }

    @Override
    public void notificarDesconexion(String ip) {
        runOnFx(() -> listaContactos.refresh());
    }

    @Override
    public void mostrarIndicadorMensaje(String ip) {
        runOnFx(() -> listaContactos.refresh());
    }

    @Override
    public void ocultarIndicadorMensaje(String ip) {
        runOnFx(() -> listaContactos.refresh());
    }

    @Override
    public void aplicarTema(String ip, String idTema) {
        runOnFx(() -> {
            temasContacto.put(ip, idTema);

            // Update combo if active contact
            if (ip.equals(contactoActivo)) {
                suppressTemaEvent = true;
                comboTemas.setValue(comboTemas.getItems().get(Integer.parseInt(idTema) - 1));
                suppressTemaEvent = false;
            }

            String[] colores = getColoresTemaHex(idTema);

            // Update chat panel background
            VBox panel = chatPanels.get(ip);
            if (panel != null) {
                panel.setStyle("-fx-background-color: " + colores[0] + "; -fx-padding: 10;");
            }

            // Update all bubbles belonging to this contact's panel
            for (Map.Entry<String, VBox> entry : bubblePanels.entrySet()) {
                VBox bubble = entry.getValue();
                String id = entry.getKey();
                Boolean isMine = bubbleIsMine.get(id);
                if (isMine == null) continue;

                if (panel != null && isNodeInParent(bubble, panel)) {
                    Label msgLabel = messageLabels.get(id);
                    boolean isDeleted = (msgLabel != null && (msgLabel.getText().contains("eliminado") || msgLabel.getText().contains("\u00FAnico abierto")));
                    if (!isDeleted) {
                        String bgColor = isMine ? colores[1] : colores[2];
                        String textColor = isMine ? colores[6] : colores[7];
                        bubble.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 14; " +
                                "-fx-border-color: " + colores[3] + "; -fx-border-radius: 14; -fx-border-width: 0.5; -fx-padding: 8 12 4 12;");
                        if (msgLabel != null) {
                            msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textColor + "; -fx-wrap-text: true;");
                        }
                    }
                }
            }

            // Update pinned bar
            if (ip.equals(contactoActivo)) {
                pinnedMessageBar.setStyle("-fx-background-color: " + colores[4] + "; -fx-border-color: " + colores[5] + "; -fx-border-width: 0 0 1 0;");
            }
        });
    }

    // ================================================================
    // CHAT PANEL & BUBBLE HELPERS
    // ================================================================

    private VBox getOrCreateChatPanel(String ip) {
        return chatPanels.computeIfAbsent(ip, k -> {
            String[] colores = getColoresTemaHex(temasContacto.getOrDefault(ip, "1"));
            VBox p = new VBox(4);
            p.setPadding(new Insets(10));
            p.setStyle("-fx-background-color: " + colores[0] + "; -fx-padding: 10;");
            return p;
        });
    }

    private void addBubbleFX(VBox panel, String text, String time, boolean isMine,
                              boolean confirmed, String idMensaje, boolean pinned, boolean viewOnce) {
        boolean isDeleted = (text == null || text.isEmpty());

        String[] colores = getColoresTemaHex(getTemaActivo());

        String bgColor, textColor, borderColor;
        if (isDeleted) {
            bgColor = "#F5F5F5";
            textColor = "#8c8c8c";
            borderColor = "#E0E0E0";
        } else {
            bgColor = isMine ? colores[1] : colores[2];
            textColor = isMine ? colores[6] : colores[7];
            borderColor = colores[3];
        }

        VBox bubble = new VBox(2);
        bubble.setMaxWidth(350);
        bubble.setMinWidth(80);
        bubble.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 14; " +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 14; -fx-border-width: 0.5; -fx-padding: 8 12 4 12;");

        // Content
        Label messageLabel = null;
        Button btnViewOnce = null;

        if (isDeleted && viewOnce) {
            messageLabel = new Label("\uD83D\uDEAB Mensaje \u00FAnico abierto");
            messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8c8c8c; -fx-font-style: italic;");
            messageLabel.setWrapText(true);
        } else if (isDeleted) {
            messageLabel = new Label("\uD83D\uDEAB Este mensaje fue eliminado");
            messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8c8c8c; -fx-font-style: italic;");
            messageLabel.setWrapText(true);
        } else if (viewOnce) {
            String btnText = isMine ? "\uD83D\uDCA3 Mensaje \u00DAnico Enviado" : "\uD83D\uDCA3 Abrir Mensaje Oculto";
            btnViewOnce = new Button(btnText);
            btnViewOnce.setStyle("-fx-background-color: #FFDDDD; -fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");
            if (!isMine) {
                final String msgText = text;
                final String msgId = idMensaje;
                btnViewOnce.setOnAction(e -> mostrarPopUpMensajeUnico(msgText, msgId));
            } else {
                btnViewOnce.setDisable(true);
            }
        } else {
            messageLabel = new Label(text);
            messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textColor + ";");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(320);
        }

        // Store references
        if (idMensaje != null && viewOnce) {
            viewOnceIds.add(idMensaje);
        }
        if (idMensaje != null && messageLabel != null) {
            messageLabels.put(idMensaje, messageLabel);
        }
        if (idMensaje != null) {
            bubblePanels.put(idMensaje, bubble);
            bubbleIsMine.put(idMensaje, isMine);
        }

        // Bottom row: pin, time, check
        HBox bottomRow = new HBox(4);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);

        if (pinned && !isDeleted) {
            Label pinLabel = new Label("\uD83D\uDCCC");
            pinLabel.setStyle("-fx-font-size: 10px;");
            bottomRow.getChildren().add(pinLabel);
            if (idMensaje != null) pinLabels.put(idMensaje, pinLabel);
        }

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isMine && !isDeleted ? "rgba(255,255,255,0.7)" : "#8C8C8C") + ";");
        bottomRow.getChildren().add(timeLabel);

        if (isMine && !isDeleted) {
            Label checkLabel = new Label(confirmed ? "\u2713\u2713" : "\u2713");
            checkLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " +
                    (confirmed ? "#3593EA" : "rgba(255,255,255,0.6)") + ";");
            bottomRow.getChildren().add(checkLabel);
            if (idMensaje != null) checkLabels.put(idMensaje, checkLabel);
        }

        // Add content to bubble
        if (btnViewOnce != null) {
            bubble.getChildren().add(btnViewOnce);
        } else {
            bubble.getChildren().add(messageLabel);
        }
        bubble.getChildren().add(bottomRow);

        // Context menu (pin/delete)
        if (!isDeleted && !viewOnce && idMensaje != null) {
            ContextMenu popup = new ContextMenu();
            MenuItem fijarItem = new MenuItem("\uD83D\uDCCC Fijar mensaje");
            fijarItem.setOnAction(e -> chatController.fijarMensaje(contactoActivo, idMensaje));
            popup.getItems().add(fijarItem);

            if (isMine) {
                MenuItem eliminarItem = new MenuItem("Eliminar mensaje");
                eliminarItem.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "\u00BFEliminar este mensaje?", ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Eliminar mensaje");
                    confirm.initOwner(stage);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES && contactoActivo != null) {
                            chatController.eliminarMensaje(contactoActivo, idMensaje);
                        }
                    });
                });
                popup.getItems().add(eliminarItem);
            }
            bubble.setOnContextMenuRequested(ev -> popup.show(bubble, ev.getScreenX(), ev.getScreenY()));
        }

        // Alignment wrapper
        HBox row = new HBox();
        row.setPadding(new Insets(3, 8, 3, 8));
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getChildren().add(bubble);

        panel.getChildren().add(row);
    }

    private void addSystemLabelFX(VBox panel, String text) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(5, 0, 5, 0));

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: gray; -fx-font-style: italic;");
        lbl.setWrapText(true);
        wrapper.getChildren().add(lbl);
        panel.getChildren().add(wrapper);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (scrollChat != null) {
                scrollChat.setVvalue(1.0);
            }
        });
    }

    private void mostrarPopUpMensajeUnico(String texto, String idMensaje) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Mensaje de Visualizaci\u00F3n \u00DAnica");
        dialog.setWidth(380);
        dialog.setHeight(220);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: white;");

        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-font-size: 15px;");
        VBox.setVgrow(area, Priority.ALWAYS);

        Button btnLeido = new Button("Le\u00EDdo (Destruir mensaje)");
        btnLeido.setStyle("-fx-background-color: #FF6464; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        btnLeido.setMaxWidth(Double.MAX_VALUE);
        btnLeido.setOnAction(e -> {
            dialog.close();
            chatController.abrirMensajeUnico(contactoActivo, idMensaje);
        });

        dialog.setOnCloseRequest(e -> {
            chatController.abrirMensajeUnico(contactoActivo, idMensaje);
        });

        content.getChildren().addAll(area, btnLeido);
        dialog.setScene(new Scene(content));
        dialog.show();
    }

    // ================================================================
    // THEME COLORS
    // ================================================================
    // Returns: [0]=panelBg, [1]=myBubbleBg, [2]=otherBubbleBg, [3]=border,
    //          [4]=pinnedBg, [5]=pinnedBorder, [6]=myTextColor, [7]=otherTextColor

    private static String[] getColoresTemaHex(String idTema) {
        if (idTema == null) idTema = "1";
        switch (idTema) {
            case "2": // Azul
                return new String[]{"#E3F2FD", "#42A5F5", "#FFFFFF", "#90CAF9", "#BBDEFB", "#90CAF9", "white", "#333333"};
            case "3": // Rojo
                return new String[]{"#FFEBEE", "#EF5350", "#FFFFFF", "#EF9A9A", "#FFCDD2", "#EF9A9A", "white", "#333333"};
            case "4": // Amarillo
                return new String[]{"#FFFDE7", "#FDD835", "#FFFFFF", "#FFF176", "#FFF9C4", "#FFF176", "#333333", "#333333"};
            case "5": // Violeta
                return new String[]{"#F3E5F5", "#AB47BC", "#FFFFFF", "#CE93D8", "#E1BEE7", "#CE93D8", "white", "#333333"};
            default: // 1 - Defecto (Teal)
                return new String[]{"#ECEFF1", TEAL, "#E8E8E8", "#B0BEC5", "#E0F2F1", "#80CBC4", "white", "#333333"};
        }
    }

    private String getTemaActivo() {
        if (contactoActivo == null) return "1";
        return temasContacto.getOrDefault(contactoActivo, "1");
    }

    private boolean isNodeInParent(Node node, VBox parent) {
        Node current = node;
        while (current != null) {
            if (current == parent) return true;
            current = current.getParent();
        }
        return false;
    }

    // ================================================================
    // TOGGLE SWITCH COMPONENT
    // ================================================================

    private static class ToggleSwitch extends StackPane {
        private final BooleanProperty switchedOn = new SimpleBooleanProperty(false);
        private final Region track;
        private final Circle thumb;

        public ToggleSwitch(boolean initial) {
            track = new Region();
            track.setPrefSize(40, 20);
            track.setMinSize(40, 20);
            track.setMaxSize(40, 20);

            thumb = new Circle(8, Color.WHITE);
            thumb.setEffect(new javafx.scene.effect.DropShadow(3, Color.gray(0, 0.3)));

            getChildren().addAll(track, thumb);
            setPrefSize(40, 20);
            setMinSize(40, 20);
            setMaxSize(40, 20);
            setCursor(javafx.scene.Cursor.HAND);

            switchedOn.set(initial);
            updateVisual();

            setOnMouseClicked(e -> {
                switchedOn.set(!switchedOn.get());
                updateVisual();
            });
        }

        private void updateVisual() {
            if (switchedOn.get()) {
                track.setStyle("-fx-background-color: " + TEAL + "; -fx-background-radius: 10;");
                StackPane.setAlignment(thumb, Pos.CENTER_RIGHT);
                StackPane.setMargin(thumb, new Insets(0, 2, 0, 0));
            } else {
                track.setStyle("-fx-background-color: #CCC; -fx-background-radius: 10;");
                StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
                StackPane.setMargin(thumb, new Insets(0, 0, 0, 2));
            }
        }

        public boolean isOn() {
            return switchedOn.get();
        }
    }
}
