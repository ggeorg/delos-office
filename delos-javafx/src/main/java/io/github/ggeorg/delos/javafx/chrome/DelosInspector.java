package io.github.ggeorg.delos.javafx.chrome;

import io.github.ggeorg.delos.javafx.icon.DelosIcons;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reusable Pages-style inspector host for Delos Office apps.
 * <p>
 * Apps own the concrete inspector content. This class only owns the shared
 * right-sidebar shell: tabs, selection, sizing, and stable style classes.
 */
public class DelosInspector extends BorderPane {
    private final TabPane tabPane = new TabPane();
    private final Map<String, Tab> tabsById = new LinkedHashMap<>();

    public DelosInspector() {
        getStyleClass().add("delos-inspector");
        setMinWidth(240.0);
        setPrefWidth(280.0);
        setMaxWidth(420.0);

        tabPane.getStyleClass().add("delos-inspector-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setFocusTraversable(false);
        setCenter(tabPane);
    }

    public void setInspectorTabs(Collection<InspectorTab> inspectorTabs) {
        Objects.requireNonNull(inspectorTabs, "inspectorTabs");
        tabsById.clear();
        ObservableList<Tab> tabs = FXCollections.observableArrayList();
        for (InspectorTab inspectorTab : inspectorTabs) {
            Tab tab = createTab(inspectorTab);
            if (tabsById.putIfAbsent(inspectorTab.id(), tab) != null) {
                throw new IllegalArgumentException("Duplicate inspector tab id: " + inspectorTab.id());
            }
            tabs.add(tab);
        }
        tabPane.getTabs().setAll(tabs);
        if (!tabs.isEmpty()) {
            tabPane.getSelectionModel().selectFirst();
        }
    }

    public Optional<String> selectedTabId() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUserData() == null) {
            return Optional.empty();
        }
        return Optional.of(selected.getUserData().toString());
    }

    public void selectTab(String id) {
        Tab tab = tabsById.get(Objects.requireNonNull(id, "id"));
        if (tab == null) {
            throw new IllegalArgumentException("Unknown inspector tab: " + id);
        }
        tabPane.getSelectionModel().select(tab);
    }

    public int tabCount() {
        return tabPane.getTabs().size();
    }

    private static Tab createTab(InspectorTab inspectorTab) {
        Tab tab = new Tab();
        tab.setUserData(inspectorTab.id());
        tab.setGraphic(tabHeader(inspectorTab));
        tab.setContent(inspectorTab.content());
        return tab;
    }

    private static Node tabHeader(InspectorTab inspectorTab) {
        HBox header = new HBox(6.0);
        header.getStyleClass().add("delos-inspector-tab-header");
        if (inspectorTab.iconId() != null) {
            header.getChildren().add(DelosIcons.toolbarIcon(inspectorTab.iconId()));
        }
        Label label = new Label(inspectorTab.title());
        label.getStyleClass().add("delos-inspector-tab-title");
        header.getChildren().add(label);
        return header;
    }
}
