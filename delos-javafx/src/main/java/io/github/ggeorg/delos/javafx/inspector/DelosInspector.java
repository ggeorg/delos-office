package io.github.ggeorg.delos.javafx.inspector;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared right-side inspector shell for Delos Office applications.
 *
 * <p>The shell owns only sidebar chrome: tab buttons, scrolling, fixed visual
 * width, and the selected panel. Application modules provide the actual
 * document-specific sections.</p>
 */
public class DelosInspector extends BorderPane {
    public static final double DEFAULT_WIDTH = 320.0;

    private final HBox tabBar = new HBox();
    private final ToggleGroup tabs = new ToggleGroup();
    private final StackPane contentHost = new StackPane();
    private final ScrollPane scroller = new ScrollPane(contentHost);
    private final Map<String, Node> contentById = new LinkedHashMap<>();
    private final ReadOnlyStringWrapper selectedTabId = new ReadOnlyStringWrapper(this, "selectedTabId", "");

    public DelosInspector() {
        getStyleClass().add("delos-inspector");
        setMinWidth(DEFAULT_WIDTH);
        setPrefWidth(DEFAULT_WIDTH);
        setMaxWidth(DEFAULT_WIDTH);

        tabBar.getStyleClass().add("delos-inspector-tabs");
        tabBar.setPadding(new Insets(10, 10, 8, 10));

        scroller.getStyleClass().add("delos-inspector-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        contentHost.getStyleClass().add("delos-inspector-content-host");
        setTop(tabBar);
        setCenter(scroller);
    }

    public void addTab(String id, String title, Node content) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (contentById.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate inspector tab: " + id);
        }

        ToggleButton button = new ToggleButton(title);
        button.getStyleClass().add("delos-inspector-tab");
        button.setFocusTraversable(false);
        button.setToggleGroup(tabs);
        button.setUserData(id);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, javafx.scene.layout.Priority.ALWAYS);
        button.setOnAction(event -> selectTab(id));

        content.getStyleClass().add("delos-inspector-tab-content");
        contentById.put(id, content);
        tabBar.getChildren().add(button);

        if (selectedTabId.get().isBlank()) {
            selectTab(id);
        }
    }

    public void selectTab(String id) {
        Node content = contentById.get(id);
        if (content == null) {
            throw new IllegalArgumentException("Unknown inspector tab: " + id);
        }
        selectedTabId.set(id);
        contentHost.getChildren().setAll(content);
        tabBar.getChildren().stream()
                .filter(ToggleButton.class::isInstance)
                .map(ToggleButton.class::cast)
                .forEach(button -> button.setSelected(id.equals(button.getUserData())));
    }


    public int tabCount() {
        return contentById.size();
    }

    public List<String> tabIds() {
        return List.copyOf(contentById.keySet());
    }

    public boolean hasTab(String id) {
        return contentById.containsKey(id);
    }

    public Node selectedContent() {
        return contentById.get(selectedTabId.get());
    }

    public Node contentForTab(String id) {
        Node content = contentById.get(id);
        if (content == null) {
            throw new IllegalArgumentException("Unknown inspector tab: " + id);
        }
        return content;
    }

    public String selectedTabId() {
        return selectedTabId.get();
    }

    public ReadOnlyStringProperty selectedTabIdProperty() {
        return selectedTabId.getReadOnlyProperty();
    }
}
