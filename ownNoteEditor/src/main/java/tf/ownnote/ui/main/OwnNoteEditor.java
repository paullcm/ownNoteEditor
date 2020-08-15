/*
 * Copyright (c) 2014 Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.ownnote.ui.main;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import tf.helper.javafx.AboutMenu;
import tf.ownnote.ui.helper.FormatHelper;
import tf.ownnote.ui.helper.GroupData;
import tf.ownnote.ui.helper.IFileChangeSubscriber;
import tf.ownnote.ui.helper.IGroupListContainer;
import tf.ownnote.ui.helper.NoteData;
import tf.ownnote.ui.helper.OwnNoteEditorParameters;
import tf.ownnote.ui.helper.OwnNoteEditorPreferences;
import tf.ownnote.ui.helper.OwnNoteFileManager;
import tf.ownnote.ui.helper.OwnNoteHTMLEditor;
import tf.ownnote.ui.helper.OwnNoteTabPane;
import tf.ownnote.ui.helper.OwnNoteTableColumn;
import tf.ownnote.ui.helper.OwnNoteTableView;
import tf.ownnote.ui.helper.TableSortHelper;
import tf.ownnote.ui.tasks.TaskData;
import tf.ownnote.ui.tasks.TaskList;
import tf.ownnote.ui.tasks.TaskManager;

/**
 *
 * @author Thomas Feuster <thomas@feuster.com>
 */
public class OwnNoteEditor implements Initializable, IFileChangeSubscriber {

    private final List<String> filesInProgress = new LinkedList<>();

    private final static OwnNoteEditorParameters parameters = OwnNoteEditorParameters.getInstance();
    
    private final static String NEWNOTENAME = "New Note";
    
    // TFE, 20200712: add search of unchecked boxes
    public final static String UNCHECKED_BOXES = "<input type=\"checkbox\" />";
    public final static String CHECKED_BOXES = "<input type=\"checkbox\" checked=\"checked\" />";
    public final static String WRONG_UNCHECKED_BOXES = "<input type=\"checkbox\">";
    public final static String WRONG_CHECKED_BOXES = "<input type=\"checkbox\" checked=\"checked\">";
    public final static String ANY_BOXES = "<input type=\"checkbox\"";
    
    private final static int TEXTFIELDWIDTH = 100;  
    
    private final List<String> realGroupNames = new LinkedList<> ();
    
    private ObservableList<NoteData> notesList = null;
    
    private final BooleanProperty inEditMode = new SimpleBooleanProperty();
    
    private boolean handleQuickSave = false;
    // should we show standard ownNote face or oneNotes?
    // TF, 20160630: refactored from "classicLook" to show its real meeaning
    private OwnNoteEditorParameters.LookAndFeel currentLookAndFeel;

    private Double classicGroupWidth;
    private Double oneNoteGroupWidth;
    private Double taskListWidth;
    private String groupsSortOrder;
    private String notesSortOrder;
    
    private IGroupListContainer myGroupList = null;
    
    // available colors for tabs to rotate through
    // issue #36 - have "All" without color
    // TF, 20170122: use colors similar to OneNote - a bit less bright
    //private static final String[] groupColors = { "lightgrey", "darkseagreen", "cornflowerblue", "lightsalmon", "gold", "orchid", "cadetblue", "goldenrod", "darkorange", "MediumVioletRed" };
    private static final String[] groupColors = { "#F3D275", "#F4A6A6", "#99D0DF", "#F1B87F", "#F2A8D1", "#9FB2E1", "#B4AFDF", "#D4B298", "#C6DA82", "#A2D07F", "#F1B5B5" };
    
    private OwnNoteTabPane groupsPane = null;
    @FXML
    private BorderPane borderPane;
    @FXML
    private GridPane gridPane;
    private SplitPane dividerPane;
    @FXML
    private TableView<Map<String, String>> notesTableFXML;
    private OwnNoteTableView notesTable = null;
    @FXML
    private TableView<Map<String, String>> groupsTableFXML;
    private OwnNoteTableView groupsTable = null;
    @FXML
    private Label ownCloudPath;
    @FXML
    private Button setOwnCloudPath;
    @FXML
    private TableColumn<Map, String> noteNameColFXML;
    private OwnNoteTableColumn noteNameCol = null;
    @FXML
    private TableColumn<Map, String> noteModifiedColFXML;
    private OwnNoteTableColumn noteModifiedCol = null;
    @FXML
    private TableColumn<Map, String> noteDeleteColFXML;
    private OwnNoteTableColumn noteDeleteCol = null;
    @FXML
    private TableColumn<Map, String> groupNameColFXML;
    private OwnNoteTableColumn groupNameCol = null;
    @FXML
    private TableColumn<Map, String> groupDeleteColFXML;
    private OwnNoteTableColumn groupDeleteCol = null;
    @FXML
    private TableColumn<Map, String> groupCountColFXML;
    private OwnNoteTableColumn groupCountCol = null;
    @FXML
    private TableColumn<Map, String> noteGroupColFXML;
    private OwnNoteTableColumn noteGroupCol = null;
    @FXML
    private Button newButton;
    @FXML
    private ComboBox<String> groupNameBox;
    @FXML
    private TextField groupNameText;
    @FXML
    private Button createButton;
    @FXML
    private Button cancelButton;
    @FXML
    private TextField noteNameText;
    @FXML
    private WebView noteEditorFXML;
    private OwnNoteHTMLEditor noteEditor = null;
    @FXML
    private Button quickSaveButton;
    @FXML
    private Button saveButton;
    @FXML
    private HBox buttonBox;
    @FXML
    private TabPane groupsPaneFXML;
    @FXML
    private MenuBar menuBar;
    @FXML
    private RadioMenuItem classicLookAndFeel;
    @FXML
    private RadioMenuItem oneNoteLookAndFeel;
    @FXML
    private ToggleGroup LookAndFeel;
    @FXML
    private Menu menuLookAndFeel;
    @FXML
    private StackPane noteEditorPaneFXML;
    @FXML
    private Label pathLabel;
    @FXML
    private TextField noteFilterText;
    @FXML
    private Label noteFilterMode;
    @FXML
    private MenuItem searchUnchecked;
    @FXML
    private MenuItem clearSearch;
    @FXML
    private StackPane taskPaneFXML;
    @FXML
    private Label taskFilterMode;
    @FXML
    private ListView<TaskData> taskListFXML;
    private TaskList taskList = null;

    final CheckBox noteFilterCheck = new CheckBox();
    final CheckBox taskFilterCheck = new CheckBox();
    @FXML
    private RadioMenuItem showTaskList;
    @FXML
    private HBox taskFilerBox;

    public OwnNoteEditor() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // defer initEditor since we need to know the value of the prameters...
    }
    
    public void stop() {
        OwnNoteFileManager.getInstance().stop();
        
        // store current percentage of group column width
        // if increment is passed as parameter, we need to remove it from the current value
        // otherwise, the percentage grows with each call :-)
        final String percentWidth = String.valueOf(gridPane.getColumnConstraints().get(0).getPercentWidth());
        // store in the preferences
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTCLASSICGROUPWIDTH, percentWidth);
        } else {
            OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTONENOTEGROUPWIDTH, percentWidth);
        }
        
        // issue #45 store sort order for tables
        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTGROUPSTABLESORTORDER, TableSortHelper.toString(groupsTable.getSortOrder()));
        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTNOTESTABLESORTORDER, TableSortHelper.toString(notesTable.getSortOrder()));
    }
    
    public void setParameters() {
        // set look & feel    
        // 1. use passed parameters
        if (OwnNoteEditor.parameters.getLookAndFeel().isPresent()) {
            currentLookAndFeel = OwnNoteEditor.parameters.getLookAndFeel().get();
        } else {
            // fix for issue #20
            // 2. try the preference settings - what was used last time?
            try {
                currentLookAndFeel = OwnNoteEditorParameters.LookAndFeel.valueOf(
                        OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.classic.name()));
                // System.out.println("Using preference for currentLookAndFeel: " + currentLookAndFeel);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // issue #30: get percentages for group column width for classic and onenote look & feel
        // issue #45 store sort order for tables
        try {
            classicGroupWidth = Double.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTCLASSICGROUPWIDTH, "18.3333333"));
            oneNoteGroupWidth = Double.valueOf(
                    OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTONENOTEGROUPWIDTH, "33.3333333"));
            
            taskListWidth = 15d;

            groupsSortOrder = OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTGROUPSTABLESORTORDER, "");
            notesSortOrder = OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTNOTESTABLESORTORDER, "");
        } catch (SecurityException ex) {
            Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        // paint the look
        initEditor();

        // init pathlabel to parameter or nothing
        String pathname = "";
        // 1. use any passed parameters
        if (OwnNoteEditor.parameters.getOwnCloudDir().isPresent()) {
            pathname = OwnNoteEditor.parameters.getOwnCloudDir().get();
        } else {
            // 2. try the preferences setting - most recent file that was opened
            try {
                pathname = OwnNoteEditorPreferences.get(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, "");
                // System.out.println("Using preference for ownCloudDir: " + pathname);
            } catch (SecurityException ex) {
                Logger.getLogger(OwnNoteEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // 3. set the value
        ownCloudPath.setText(pathname);
    }

    //
    // basic setup is a 32x2 gridpane with a 2 part splitpane in the lower row spanning 2 cols
    // TFE: 20181028: pathBox has moved to menu to make room for filterBox
    // TFE, 20200810: 3rd column added for task handling
    //
    // --------------------------------------------------------------------------------
    // |                          |                         |                         |
    // | both: noteFilterBox      | classic: buttonBox      | both: taskFilterBox     |
    // |                          | oneNote: groupsPaneFXML |                         |
    // |                          |                         |                         |
    // --------------------------------------------------------------------------------
    // |                          |                         |                         |
    // | dividerPane              |                         |                         |
    // |                          |                         |                         |
    // | classic: groupsTableFXML | both: noteEditorFXML    | both: taskListFXML      |
    // | oneNote: notesTableFXML  |                         |                         |
    // |                          |                         |                         |
    // --------------------------------------------------------------------------------
    //
    // to be able to do proper layout in scenebuilder everything except the dividerPane
    // are added to the fxml into the gridpane - code below does the re-arrangement based on 
    // value of currentLookAndFeel
    //
    @SuppressWarnings("unchecked")
    private void initEditor() {
        // init menu handling
        initMenus();
        
        // init our wrappers to FXML classes...
        noteNameCol = new OwnNoteTableColumn(noteNameColFXML, this);
        noteModifiedCol = new OwnNoteTableColumn(noteModifiedColFXML, this);
        noteDeleteCol = new OwnNoteTableColumn(noteDeleteColFXML, this);
        noteGroupCol = new OwnNoteTableColumn(noteGroupColFXML, this);
        notesTable = new OwnNoteTableView(notesTableFXML, this);
        notesTable.setSortOrder(TableSortHelper.fromString(notesSortOrder));

        groupNameCol = new OwnNoteTableColumn(groupNameColFXML, this);
        groupDeleteCol = new OwnNoteTableColumn(groupDeleteColFXML, this);
        groupCountCol = new OwnNoteTableColumn(groupCountColFXML, this);
        groupsTable = new OwnNoteTableView(groupsTableFXML, this);
        groupsTable.setSortOrder(TableSortHelper.fromString(groupsSortOrder));
        
        groupsPane = new OwnNoteTabPane(groupsPaneFXML, this);
        
        noteEditor = new OwnNoteHTMLEditor(noteEditorFXML, this);
        
        // hide borders
        borderPane.setBottom(null);
        borderPane.setLeft(null);
        borderPane.setRight(null);
        
        // fixed height top row in grid and set height & width of rest
        gridPane.getRowConstraints().clear();
        gridPane.getRowConstraints().add(new RowConstraints(40, 40, 40));
        RowConstraints row2 = new RowConstraints(160, 560, Double.MAX_VALUE);
        row2.setVgrow(Priority.ALWAYS);
        gridPane.getRowConstraints().add(row2);
        
        gridPane.getColumnConstraints().clear();
        ColumnConstraints column1 = new ColumnConstraints();
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            column1.setPercentWidth(classicGroupWidth);
        } else {
            column1.setPercentWidth(oneNoteGroupWidth);
        }
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth((100d - taskListWidth) - column1.getPercentWidth());
        column2.setHgrow(Priority.ALWAYS);
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setPercentWidth(taskListWidth);
        column3.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(column1, column2, column3);
        
        // set callback, width, value name, cursor type of columns
        noteNameCol.setTableColumnProperties(0.65, NoteData.getNoteDataName(0), OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel));
        noteModifiedCol.setTableColumnProperties(0.25, NoteData.getNoteDataName(1), false);
        // see issue #42
        noteModifiedCol.setComparator(FormatHelper.getInstance().getFileTimeComparator());
        noteDeleteCol.setTableColumnProperties(0.10, NoteData.getNoteDataName(2), false);
        noteGroupCol.setTableColumnProperties(0, NoteData.getNoteDataName(3), false);

        // only new button visible initially
        hideAndDisableAllCreateControls();
        hideAndDisableAllEditControls();
        
        // issue #59: support filtering of note names
        // https://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        noteFilterText.textProperty().addListener((observable, oldValue, newValue) -> {
            notesTable.setNoteFilterText(newValue);
        });
        noteFilterText.setOnKeyReleased((t) -> {
            if (KeyCode.ESCAPE.equals(t.getCode())) {
                noteFilterText.setText("");
            }
        });
        noteFilterCheck.getStyleClass().add("noteFilterCheck");
        noteFilterCheck.selectedProperty().addListener((o) -> {
            notesTable.setNoteFilterMode(noteFilterCheck.isSelected());
        });
        noteFilterMode.setGraphic(noteFilterCheck);
        noteFilterMode.setContentDisplay(ContentDisplay.RIGHT);

        // issue #30: store left and right regions of the second row in the grid - they are needed later on for the divider pane
        Region leftRegion;
        Region rightRegion;
        
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            myGroupList = groupsTable;
            
            hideNoteEditor();
            
            groupsPane.setDisable(true);
            groupsPane.setVisible(false);

            // set callback, width, value name, cursor type of columns
            groupNameCol.setTableColumnProperties(0.65, GroupData.getGroupDataName(0), false);
            groupDeleteCol.setTableColumnProperties(0.15, GroupData.getGroupDataName(1), false);
            groupCountCol.setTableColumnProperties(0.20, GroupData.getGroupDataName(2), false);

            // name can be changed - but not for all entries!
            groupsTable.setEditable(true);
            groupNameCol.setEditable(true);

            // in case the group name changes notes neeed to be renamed
            groupNameCol.setOnEditCommit((CellEditEvent<Map, String> t) -> {
                final GroupData curEntry =
                        new GroupData((Map<String, String>) t.getTableView().getItems().get(t.getTablePosition().getRow()));

                if (!t.getNewValue().equals(t.getOldValue())) {
                    // rename all notes of the group
                    if (!renameGroupWrapper(t.getOldValue(), t.getNewValue())) {
                        // TODO: revert changes to group name on UI
                        curEntry.setGroupName(t.getOldValue());

                        // workaround til TODO above resolved :-)
                        initFromDirectory(false);
                    } else {
                        // update group name in table
                        curEntry.setGroupName(t.getNewValue());
                    }
                }
            });
            
            // buttons and stuff should only impact layout when visible
            // https://stackoverflow.com/questions/12200195/javafx-hbox-hide-item
            for(Node child: buttonBox.getChildren()){
                child.managedProperty().bind(child.visibleProperty());
            }

            // keep track of the visibility of the editor
            inEditMode.bind(noteEditor.visibleProperty());

            // add listener for note name
            noteNameText.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.equals(oldValue)) {
                        if (this.inEditMode.get() && this.handleQuickSave) {
                            hideAndDisableControl(quickSaveButton);
                        }
                    }
                });

            // add listener for group name
            groupNameText.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.equals(oldValue)) {
                        if (this.inEditMode.get() && this.handleQuickSave) {
                            hideAndDisableControl(quickSaveButton);
                        }
                    }
                });

            // add listener for group combobox - but before changing values :-)
            groupNameBox.valueProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.equals(oldValue)) {
                        // only in case of "new group" selected we show the field to enter an new group name
                        if (newValue.equals(GroupData.NEW_GROUP)) {
                            showAndEnableControl(groupNameText);
                            groupNameText.setPromptText("group title");
                            groupNameText.clear();
                        } else {
                            hideAndDisableControl(groupNameText);
                        }
                        if (this.inEditMode.get() && this.handleQuickSave) {
                            hideAndDisableControl(quickSaveButton);
                        }
                    }
                });

            // add action to the button - show initial create controls and populate combo box
            newButton.setOnAction((ActionEvent event) -> {
                // 1. enable initial create controls
                showAndEnableInitialCreateControls();

                // 2. fill combo box
                initGroupNameBox();
                groupNameBox.setValue(GroupData.NOT_GROUPED);
                groupNameBox.requestFocus();
            });
            // issue #41
            newButton.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> {
                newButton.fire();
            });

            // cancel button simply hides all create controls
            cancelButton.setOnAction((ActionEvent event) -> {
                if (!inEditMode.get()) {
                    hideAndDisableAllCreateControls();
                } else {
                    hideAndDisableAllEditControls();
                    hideNoteEditor();
                }
            });

            // create button creates empty note if text fields filled
            createButton.setOnAction((ActionEvent event) -> {
                // check if name fields are filled correctly
                Boolean doCreate = true;

                String newNoteName = noteNameText.getText();
                if (newNoteName.isEmpty()) {
                    // no note name entered...
                    doCreate = false;

                    showAlert(AlertType.ERROR, "Error Dialog", "No note title given.", null);
                }
                String newGroupName = "";
                if (groupNameBox.getValue().equals(GroupData.NEW_GROUP)) {
                    newGroupName = groupNameText.getText();

                    if (newGroupName.isEmpty()) {
                        // "new group" selected and no group name entered...
                        doCreate = false;

                        showAlert(AlertType.ERROR, "Error Dialog", "No group title given.", null);
                    }
                } else {
                    newGroupName = groupNameBox.getValue();
                }

                if (doCreate) {
                    if (createNoteWrapper(newGroupName, newNoteName)) {
                        hideAndDisableAllCreateControls();
                        initFromDirectory(false);
                    }
                }
            });

            // quicksave button saves note but stays in editor
            quickSaveButton.setOnAction((ActionEvent event) -> {
                // quicksave = no changes to note name and group name allowed!
                final NoteData curNote = noteEditor.getUserData();

                if (OwnNoteFileManager.getInstance().saveNote(curNote.getGroupName(),
                        curNote.getNoteName(),
                        noteEditor.getNoteText())) {
                } else {
                    // error message - most likely note in "Not grouped" with same name already exists
                    showAlert(AlertType.ERROR, "Error Dialog", "Note couldn't be saved.", null);
                }
            });

            // save button saves note but stays in editor
            saveButton.setOnAction((ActionEvent event) -> {
                // save = you might have changed note name & group name
                // check if name fields are filled correctly
                Boolean doSave = true;

                String newNoteName = noteNameText.getText();
                if (newNoteName.isEmpty()) {
                    // no note name entered...
                    doSave = false;

                    showAlert(AlertType.ERROR, "Error Dialog", "No note title given.", null);
                }
                String newGroupName = "";
                if (groupNameBox.getValue().equals(GroupData.NEW_GROUP)) {
                    newGroupName = groupNameText.getText();

                    if (newGroupName.isEmpty()) {
                        // "new group" selected and no group name entered...
                        doSave = false;

                        showAlert(AlertType.ERROR, "Error Dialog", "No group title given.", null);
                    }
                } else {
                    newGroupName = groupNameBox.getValue();
                }

                if (doSave) {
                    // check against previous note and group name - might have changed!
                    final NoteData curNote = noteEditor.getUserData();
                    final String curNoteName = curNote.getNoteName();
                    final String curGroupName = curNote.getGroupName();

                    if (!curNoteName.equals(newNoteName) || !curGroupName.equals(newGroupName)) {
                        // a bit of save transactions: first create new then delete old...
                        if (!createNoteWrapper(newGroupName, newNoteName)) {
                            doSave = false;
                        } else {
                            if (!deleteNoteWrapper(curNote)) {
                                doSave = false;
                                // clean up: delete new empty note - ignore return values
                                OwnNoteFileManager.getInstance().deleteNote(newGroupName, newNoteName);
                            }
                        }
                    }
                }

                if (doSave) {
                    if (saveNoteWrapper(newGroupName, newNoteName, noteEditor.getNoteText())) {
                        noteEditor.hasBeenSaved();
                    }
                }
            });
        
            // classic look & feel: groups table to the right, notes table or editor to the left
            leftRegion = groupsTableFXML;
            final StackPane rightPane = new StackPane();
            rightPane.getChildren().addAll(notesTableFXML, noteEditorFXML);
            // issue #40: bind height & width to parent
            notesTableFXML.prefWidthProperty().bind(rightPane.widthProperty());
            notesTableFXML.prefHeightProperty().bind(rightPane.heightProperty());
            noteEditorFXML.prefWidthProperty().bind(rightPane.widthProperty());
            noteEditorFXML.prefHeightProperty().bind(rightPane.heightProperty());
            rightRegion = rightPane;
            
            // remove things not shown directly in the gridPane
            // groupsPaneFXML: only in oneNote
            gridPane.getChildren().remove(groupsPaneFXML);
            // groupsTableFXML: shown in dividerPane
            gridPane.getChildren().remove(groupsTableFXML);
            // notesTableFXML: shown in dividerPane
            gridPane.getChildren().remove(notesTableFXML);
            // noteEditorFXML: shown in dividerPane
            gridPane.getChildren().remove(noteEditorFXML);

        } else {

            myGroupList = groupsPane;
            
            // oneNote look and feel
            // 1. no groups table, no button list
            groupsTable.setDisable(true);
            groupsTable.setVisible(false);

            buttonBox.setDisable(true);
            buttonBox.setVisible(false);
            
            // 3. and can't be deleted with trashcan
            noteNameCol.setWidthPercentage(0.74);
            noteNameCol.setStyle("notename-font-weight: normal");
            noteModifiedCol.setWidthPercentage(0.24);
            noteDeleteCol.setVisible(false);
            
            // name can be changed - but not for all entries!
            noteNameCol.setEditable(true);
            notesTable.setEditable(true);
            
            // From documentation - The .root style class is applied to the root node of the Scene instance.
            notesTable.getScene().getRoot().setStyle("note-selected-background-color: white");
            notesTable.getScene().getRoot().setStyle("note-selected-font-color: black");
            
            // renaming note
            noteNameCol.setOnEditCommit((CellEditEvent<Map, String> t) -> {
                final NoteData curNote =
                        new NoteData((Map<String, String>) t.getTableView().getItems().get(t.getTablePosition().getRow()));

                if (!t.getNewValue().equals(t.getOldValue())) {
                    if (!renameNoteWrapper(curNote, t.getNewValue())) {
                        // TF, 20160815: restore old name in case of error

                        // https://stackoverflow.com/questions/20798634/restore-oldvalue-in-tableview-after-editing-the-cell-javafx
                        t.getTableView().getColumns().get(0).setVisible(false);
                        t.getTableView().getColumns().get(0).setVisible(true);
                    }
                }
            });
            
            groupsPane.setDisable(false);
            groupsPane.setVisible(true);
            
            // oneNote look & feel: notes table to the right, editor to the left
            leftRegion = notesTableFXML;
            rightRegion = noteEditorPaneFXML;
            
            // remove things not shown directly in the gridPane
            // buttonBox: only in classic
            gridPane.getChildren().remove(buttonBox);
            // groupsTableFXML: shown in dividerPane
            gridPane.getChildren().remove(groupsTableFXML);
            // notesTableFXML: shown in dividerPane
            gridPane.getChildren().remove(notesTableFXML);
            // noteEditorFXML: shown in dividerPane
            gridPane.getChildren().remove(noteEditorFXML);
        }
        
        // issue #30: add transparent split pane on top of the grid pane to have a moveable divider
        dividerPane = new SplitPane();
        // add content from second grid row to the split pane
        dividerPane.getItems().addAll(leftRegion, rightRegion);
        // issue #40: bind height to parent
        leftRegion.prefHeightProperty().bind(dividerPane.heightProperty());
        rightRegion.prefHeightProperty().bind(dividerPane.heightProperty());
        
        // needs to take 3 column into account
        dividerPane.setDividerPosition(0, gridPane.getColumnConstraints().get(0).getPercentWidth() / (100d - taskListWidth));
        // show split pane as second row
        gridPane.add(dividerPane, 0, 1);
        GridPane.setColumnSpan(dividerPane, 2);
        
        //Constrain max size of left & right pane:
        leftRegion.minWidthProperty().bind(dividerPane.widthProperty().multiply(0.15));
        leftRegion.maxWidthProperty().bind(dividerPane.widthProperty().multiply(0.5));
        rightRegion.minWidthProperty().bind(dividerPane.widthProperty().multiply(0.5));
        rightRegion.maxWidthProperty().bind(dividerPane.widthProperty().multiply(0.85));

        // move divider with gridpane on resize
        // https://stackoverflow.com/questions/15324321/javafx-splitpane-resize-proportions
        gridPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    // needs to take 3 column into account - if shown
                    dividerPane.setDividerPosition(0, 
                            gridPane.getColumnConstraints().get(0).getPercentWidth() / (100d - gridPane.getColumnConstraints().get(2).getPercentWidth()));
                });
            }
        });
        
        // change width of gridpane when moving divider
        dividerPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // change later to avoid loop calls when resizing scene
                Platform.runLater(() -> {
                    // needs to take 3 column into account - if shown
                    final double newPercentage = 
                            newValue.doubleValue() * (100d - gridPane.getColumnConstraints().get(2).getPercentWidth());
                    gridPane.getColumnConstraints().get(0).setPercentWidth(newPercentage);
                    gridPane.getColumnConstraints().get(1).setPercentWidth((100d - gridPane.getColumnConstraints().get(2).getPercentWidth()) - newPercentage);
                });
            }
        });
        
        // TFE, 20200810: adding third gridpane column for task handling
        taskList = new TaskList(taskListFXML, this);
        taskListFXML.prefWidthProperty().bind(taskPaneFXML.widthProperty());
        taskListFXML.prefHeightProperty().bind(taskPaneFXML.heightProperty());
        
        taskFilterCheck.getStyleClass().add("noteFilterCheck");
        taskFilterCheck.selectedProperty().addListener((o) -> {
            taskList.setTaskFilterMode(taskFilterCheck.isSelected());
        });
        taskFilterCheck.setSelected(false);
        taskFilterMode.setGraphic(taskFilterCheck);
        taskFilterMode.setContentDisplay(ContentDisplay.RIGHT);

        // all setup, lets spread the news
        OwnNoteFileManager.getInstance().setCallback(this);
        TaskManager.getInstance().setCallback(this);
    }
    
    private void initMenus() {
        showTaskList.setSelected(true);
        showTaskList.selectedProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue) {
                    // switch on controls
                    taskFilerBox.setVisible(true);
                    taskFilerBox.setDisable(false);
                    taskList.setVisible(true);
                    taskList.setDisable(false);
                    
                    // show third row gridpane
                    final double secondWidth = gridPane.getColumnConstraints().get(1).getPercentWidth();
                    gridPane.getColumnConstraints().get(1).setPercentWidth(secondWidth - taskListWidth);
                    gridPane.getColumnConstraints().get(2).setPercentWidth(taskListWidth);
                } else {
                    // switch off controls
                    taskFilerBox.setVisible(false);
                    taskFilerBox.setDisable(true);
                    taskList.setVisible(false);
                    taskList.setDisable(true);
                    
                    // hide third row gridpane
                    gridPane.getColumnConstraints().get(2).setPercentWidth(0.0);
                    final double secondWidth = gridPane.getColumnConstraints().get(1).getPercentWidth();
                    gridPane.getColumnConstraints().get(1).setPercentWidth(secondWidth + taskListWidth);
                }
            }
        });
        
        // select entry based on value of currentLookAndFeel
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            classicLookAndFeel.setSelected(true);
        } else {
            oneNoteLookAndFeel.setSelected(true);
        }
        // 2. add listener to track changes of layout - only after setting it initially
        classicLookAndFeel.getToggleGroup().selectedToggleProperty().addListener(
            (ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2) -> {
                // when starting up things might not be initialized properly
                if (arg2 != null) {
                    assert (arg2 instanceof RadioMenuItem);

                    // store in the preferences
                    if (((RadioMenuItem) arg2).equals(classicLookAndFeel)) {
                        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.classic.name());
                    } else {
                        OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTLOOKANDFEEL, OwnNoteEditorParameters.LookAndFeel.oneNote.name());
                    }
                }
            });
        
        // add changelistener to pathlabel - not that you should actually change its value during runtime...
        ownCloudPath.textProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                // store in the preferences
                OwnNoteEditorPreferences.put(OwnNoteEditorPreferences.RECENTOWNCLOUDPATH, newValue);

                // scan files in new directory
                initFromDirectory(false);
            }); 
        // TFE, 20181028: open file chooser also when left clicking on pathBox
        ownCloudPath.setOnMouseClicked((event) -> {
            if (MouseButton.PRIMARY.equals(event.getButton())) {
                setOwnCloudPath.fire();
            }
        });
        
        // add action to the button - open a directory search dialogue...
        setOwnCloudPath.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select ownCloud Notes directory");
            if (ownCloudPath != null && !ownCloudPath.getText().isEmpty()) {
                final File ownFile = new File(ownCloudPath.getText());
                // TF, 20160820: directory might not exist anymore!
                // in that case directoryChooser.showDialog throws an error and you can't change to an existing dir...
                if (ownFile.exists() && ownFile.isDirectory() && ownFile.canRead()) {
                    directoryChooser.setInitialDirectory(ownFile);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(setOwnCloudPath.getScene().getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                ownCloudPath.setText(selectedDirectory.getAbsolutePath());
            }
        });
       
        // TFE, 20200712: find unchecked boxes via menu
        searchUnchecked.setOnAction((ActionEvent event) -> {
            noteFilterText.setText(UNCHECKED_BOXES);
            noteFilterCheck.setSelected(true);
        });
        
        clearSearch.setOnAction((ActionEvent event) -> {
            noteFilterText.setText("");
            noteFilterCheck.setSelected(false);
        });

        AboutMenu.getInstance().addAboutMenu(OwnNoteEditor.class, borderPane.getScene().getWindow(), menuBar, "OwnNoteEditor", "v4.6", "https://github.com/ThomasDaheim/ownNoteEditor");
    }

    @SuppressWarnings("unchecked")
    public void initFromDirectory(final boolean updateOnly) {
        checkChangedNote();

        // scan directory
        OwnNoteFileManager.getInstance().initOwnNotePath(ownCloudPath.textProperty().getValue());
        taskList.populateTaskList();
        
        // add new table entries & disable & enable accordingly
        notesList = OwnNoteFileManager.getInstance().getNotesList();
        // http://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        
        // Issue #59: advanced filtering & sorting
        // do the stuff in the OwnNoteTableView - thats the right place!
        notesTable.setNotes(notesList);
        
        /*
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(notesList, p -> true);
        // re-apply filter predicate when already set
        final String curGroupName = (String) notesTable.getTableView().getUserData();
        if (curGroupName != null) {
            setGroupNameFilter(curGroupName);
        }

        // 2. Set the filter Predicate whenever the filter changes.
        // done in TabPane and TableView controls

        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<Map<String, String>> sortedData = new SortedList<Map<String, String>>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(notesTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.        
        notesTable.setNotes(sortedData);
        */
        
        ObservableList<GroupData> groupsList = OwnNoteFileManager.getInstance().getGroupsList();
        myGroupList.setGroups(groupsList, updateOnly);
        
        // and now store group names (real ones!) for later use
        initGroupNames();
    }
    
    @SuppressWarnings("unchecked")
    public boolean checkChangedNote() {
        Boolean result = true;
        
        // fix for #13: check for unsaved changes
        if (noteEditor.hasChanged()) {
            final ButtonType buttonSave = new ButtonType("Save", ButtonData.OTHER);
            final ButtonType buttonDiscard = new ButtonType("Discard", ButtonData.OTHER);

            Optional<ButtonType> saveChanges = showAlert(AlertType.CONFIRMATION, "Unsaved changes!", "Save now or discard your changes?", null, buttonSave, buttonDiscard);

            // TF: 20151229: cancel doesn#t work since there seems to be no way to avoid change of focus
            // and clicking on other note or group => UI would show a different note than is actualy edited
            // final ButtonType buttonCancel = new ButtonType("Cancel", ButtonData.OTHER);
            // alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);

            if (saveChanges.isPresent()) {
                if (saveChanges.get().equals(buttonSave)) {
                    // save note
                    final NoteData prevNote = noteEditor.getUserData();
                    if (saveNoteWrapper(prevNote.getGroupName(), prevNote.getNoteName(), noteEditor.getNoteText())) {
                        noteEditor.hasBeenSaved();
                    }
                }
                
                // if (saveChanges.get().equals(buttonCancel)) {
                //     // stop further processing of new file
                //     result = false;                    
                // }
            }
        }

        return result;
    }
    
    public boolean editNote(final NoteData curNote) {
        boolean result = false;
        
        if (!checkChangedNote()) {
            return result;
        }
        
        // 1. switch views: replace NoteTable with htmlEditor
        showNoteEditor();
        initGroupNameBox();
        noteNameText.setText(curNote.getNoteName());
        groupNameBox.setValue(curNote.getGroupName());
        this.handleQuickSave = true;

        // 2. show content of file in editor
        curNote.setNoteFileContent(OwnNoteFileManager.getInstance().readNote(curNote));
        noteEditor.setNoteText(curNote, curNote.getNoteFileContent());
        
        // 3. store note reference for saving
        noteEditor.setUserData(curNote);
        
        return result;
    }
    
    public void selectNoteAndPosition(final NoteData noteData, final int textPos, final String htmlText) {
        // need to distinguish between views to select group
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            groupsTable.selectGroupForNote(noteData);
        } else {
            groupsPane.selectGroupForNote(noteData);
        }
        
        // and now select the note - leads to callback to editNote to fill the htmleditor
        notesTable.selectNote(noteData);
        
        noteEditor.setTextCursor(textPos, htmlText);
    }

    private void hideAndDisableAllCreateControls() {
        // show new button
        showAndEnableControl(newButton);
        
        hideAndDisableControl(noteNameText);
        
        hideAndDisableControl(groupNameBox);

        hideAndDisableControl(groupNameText);
        
        hideAndDisableControl(createButton);

        hideAndDisableControl(cancelButton);
    }

    private void showAndEnableInitialCreateControls() {
        // hide new button
        hideAndDisableControl(newButton);
        
        showAndEnableControl(noteNameText);
        noteNameText.setPromptText("note title");

        showAndEnableControl(groupNameBox);
        
        hideAndDisableControl(groupNameText);

        showAndEnableControl(createButton);
        
        // make sure quicksave & save button isn't left over shown from previous
        hideAndDisableControl(quickSaveButton);
        this.handleQuickSave = false;
        hideAndDisableControl(saveButton);

        showAndEnableControl(cancelButton);
    }

    private void hideNoteEditor() {
        noteEditor.setDisable(true);
        noteEditor.setVisible(false);
        
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            notesTable.setDisable(false);
            notesTable.setVisible(true);
        }
    }

    private void showNoteEditor() {
        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            notesTable.setDisable(true);
            notesTable.setVisible(false);
        }

        noteEditor.setDisable(false);
        noteEditor.setVisible(true);

        if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
            showAndEnableInitialEditControls();
        }
    }
    
    private void hideAndDisableAllEditControls() {
        // show new button
        showAndEnableControl(newButton);
        
        hideAndDisableControl(noteNameText);
        
        hideAndDisableControl(groupNameBox);

        hideAndDisableControl(groupNameText);
        
        hideAndDisableControl(quickSaveButton);
        this.handleQuickSave = false;

        hideAndDisableControl(saveButton);
        
        hideAndDisableControl(cancelButton);
    }

    private void showAndEnableInitialEditControls() {
        // hide new button
        hideAndDisableControl(newButton);
        
        showAndEnableControl(noteNameText);
        noteNameText.setPromptText("note title");

        showAndEnableControl(groupNameBox);
        
        hideAndDisableControl(groupNameText);

        // make sure create button isn't left over shown from previous
        hideAndDisableControl(createButton);

        showAndEnableControl(quickSaveButton);

        showAndEnableControl(saveButton);
        
        showAndEnableControl(cancelButton);
    }
    
    private void hideAndDisableControl(final Control control) {
        control.setDisable(true);
        control.setVisible(false);
        control.setMinWidth(0);
        control.setMaxWidth(0);
        control.setPrefWidth(0);
    }
    
    private void showAndEnableControl(final Control control) {
        control.setDisable(false);
        control.setVisible(true);
        control.setPrefWidth(Control.USE_COMPUTED_SIZE);
        control.setMinWidth(Control.USE_PREF_SIZE);
        control.setMaxWidth(Control.USE_PREF_SIZE);
    }

    private void initGroupNameBox() {
        groupNameBox.getItems().clear();
        groupNameBox.getItems().add(GroupData.NOT_GROUPED);
        groupNameBox.getItems().add(GroupData.NEW_GROUP);
        groupNameBox.getItems().addAll(realGroupNames);
    }
    
    public OwnNoteHTMLEditor getNoteEditor() {
        return noteEditor;
    }

    public boolean createGroupWrapper(final String newGroupName) {
        Boolean result = OwnNoteFileManager.getInstance().createNewGroup(newGroupName);

        if (!result) {
            // error message - most likely group with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "New group couldn't be created.", "Group with same name already exists.");
        }
        
        return result;
    }

    public boolean renameGroupWrapper(final String oldValue, final String newValue) {
        boolean result = false;

        // no rename for "All" and "Not Grouped"
        if (!newValue.equals(GroupData.ALL_GROUPS) && !newValue.equals(GroupData.NOT_GROUPED)) {
            result = OwnNoteFileManager.getInstance().renameGroup(oldValue, newValue);
            initGroupNames();

            if (!result) {
                // error message - most likely note in new group with same name already exists
                showAlert(AlertType.ERROR, "Error Dialog", "An error occured while renaming the group.", "A file in the new group has the same name as a file in the old.");
            } else {
                //check if we just moved the current note in the editor...
                noteEditor.checkForNameChange(oldValue, newValue);
            }
        }
        
        return result;
    }

    public Boolean deleteGroupWrapper(final GroupData curGroup) {
        boolean result = false;
                
        final String groupName = curGroup.getGroupName();
        // no delete for "All" and "Not Grouped"
        if (!groupName.equals(GroupData.ALL_GROUPS) && !groupName.equals(GroupData.NOT_GROUPED)) {
            result = OwnNoteFileManager.getInstance().deleteGroup(groupName);
            initGroupNames();

            if (!result) {
                // error message - most likely note in "Not grouped" with same name already exists
                showAlert(AlertType.ERROR, "Error Dialog", "An error occured while deleting the group.", "An ungrouped file has the same name as a file in this group.");
            }
        }
        
        return result;
    }

    private void initGroupNames() {
        realGroupNames.clear();

        final ObservableList<GroupData> groupsList = OwnNoteFileManager.getInstance().getGroupsList();
        for (GroupData group: groupsList) {
            final String groupName = group.getGroupName();
            if (!groupName.equals(GroupData.NOT_GROUPED) && !groupName.equals(GroupData.ALL_GROUPS)) {
                realGroupNames.add(groupName);
            }
        }
    }

    public Boolean deleteNoteWrapper(final NoteData curNote) {
        Boolean result = OwnNoteFileManager.getInstance().deleteNote(curNote.getGroupName(), curNote.getNoteName());

        if (!result) {
            // error message - something went wrong
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while deleting the note.", "See log for details.");
        }
        
        return result;
    }

    public boolean createNoteWrapper(final String newGroupName, final String newNoteName) {
        Boolean result = OwnNoteFileManager.getInstance().createNewNote(newGroupName, newNoteName);

        if (!result) {
            // error message - most likely note in "Not grouped" with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "New note couldn't be created.", "Note with same group and name already exists.");
        }
        
        return result;
    }

    public boolean renameNoteWrapper(final NoteData curNote, final String newValue) {
        Boolean result = OwnNoteFileManager.getInstance().renameNote(curNote.getGroupName(), curNote.getNoteName(), newValue);
        
        if (!result) {
            // error message - most likely note with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while renaming the note.", "A note with the same name already exists.");
        } else {
            //check if we just moved the current note in the editor...
            noteEditor.checkForNameChange(curNote.getGroupName(), curNote.getGroupName(), curNote.getNoteName(), newValue);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public boolean moveNoteWrapper(final NoteData curNote, final String newValue) {
        Boolean result = OwnNoteFileManager.getInstance().moveNote(curNote.getGroupName(), curNote.getNoteName(), newValue);
        
        if (!result) {
            // error message - most likely note with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "An error occured while moving the note.", "A note with the same name already exists in the new group.");
        } else {
            //check if we just moved the current note in the editor...
            noteEditor.checkForNameChange(curNote.getGroupName(), newValue, curNote.getNoteName(), curNote.getNoteName());
        }
        
        return result;
    }

    public boolean saveNoteWrapper(String newGroupName, String newNoteName, String noteText) {
        Boolean result = OwnNoteFileManager.getInstance().saveNote(newGroupName, newNoteName, noteEditor.getNoteText());
                
        if (result) {
            if (OwnNoteEditorParameters.LookAndFeel.classic.equals(currentLookAndFeel)) {
                hideAndDisableAllEditControls();
                hideNoteEditor();
                initFromDirectory(false);
            } else {
                // TF, 20170723: refresh notes list since modified has changed
                notesTableFXML.refresh();
            }
        } else {
            // error message - most likely note in "Not grouped" with same name already exists
            showAlert(AlertType.ERROR, "Error Dialog", "Note couldn't be saved.", null);
        }
        
        return result;
    }

    public void setGroupNameFilter(final String groupName) {
        notesTable.setGroupNameFilter(groupName);
        
        // Issue #59: advanced filtering & sorting
        // do the stuff in the OwnNoteTableView - thats the right place!
        /*
        notesTable.getTableView().setUserData(groupName);
        
        filteredData.setPredicate((Map<String, String> note) -> {
            // If filter text is empty, display all persons. Also for "All".
            if (groupName == null || groupName.isEmpty() || groupName.equals(GroupData.ALL_GROUPS) ) {
                return true;
            }
            // Compare note name to filter text.
            return (new NoteData(note)).getGroupName().equals(groupName); 
        });
        */
    }

    public void setNotesTableForNewTab(String style) {
        notesTable.setStyle(style);
        
        selectFirstOrCurrentNote();
    }
    
    @SuppressWarnings("unchecked")
    private void selectFirstOrCurrentNote() {
        // select first or current note - if any
        if (!notesTable.getItems().isEmpty()) {
            // check if current edit is ongoing AND note in the select tab (can happen with drag & drop!)
            NoteData curNote = noteEditor.getUserData();
            
            int selectIndex = 0;
            if (curNote != null && notesTable.getItems().contains(curNote)) {
                selectIndex = notesTable.getItems().indexOf(curNote);
            }
           
            notesTable.selectAndFocusRow(selectIndex);
            editNote(new NoteData((Map<String, String>) notesTable.getItems().get(selectIndex)));
        } else {
            noteEditor.setDisable(true);
            noteEditor.setNoteText(null, "");
            noteEditor.setUserData(null);
        }
    }
    
    public String uniqueNewNoteNameForGroup(final String groupName) {
        String result;
        int newCount = notesList.size() + 1;
        
        do {
            result = OwnNoteEditor.NEWNOTENAME + " " + newCount;
            newCount++;
        } while(OwnNoteFileManager.getInstance().noteExists(groupName, result));
        
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean processFileChange(final WatchEvent.Kind<?> eventKind, final Path filePath) {
        // System.out.printf("Time %s: Gotcha!\n", getCurrentTimeStamp());
        boolean result = true;
        
        if (!filesInProgress.contains(filePath.getFileName().toString())) {
            final String fileName = filePath.getFileName().toString();
        
            // System.out.printf("Time %s: You're new here!\n", getCurrentTimeStamp());
            filesInProgress.add(fileName);
            
            // re-init list of groups and notes - file has beeen added or removed
            Platform.runLater(() -> {
                if (!StandardWatchEventKinds.ENTRY_CREATE.equals(eventKind)) {
                    // delete & modify is only relevant if we're editing this note...
                    if (noteEditor.getUserData() != null) {
                        final NoteData curNote = noteEditor.getUserData();
                        final String curName = OwnNoteFileManager.getInstance().buildNoteName(curNote.getGroupName(), curNote.getNoteName());

                        if (curName.equals(filePath.getFileName().toString())) {
                            String alertTitle;
                            String alertHeaderText;
                            if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                alertTitle = "Note has changed in file system!";
                                alertHeaderText = "Options:\nSave own note to different name\nSave own note and overwrite file system changes\nLoad changed note and discard own changes";
                            } else {
                                alertTitle = "Note has been deleted in file system!";
                                alertHeaderText = "Options:\nSave own note to different name\nSave own note and overwrite file system changes\nDiscard own changes";
                            }
                            
                            // same note! lets ask the user what to do...
                            final ButtonType buttonSave = new ButtonType("Save own", ButtonData.OK_DONE);
                            final ButtonType buttonSaveNew = new ButtonType("Save as new", ButtonData.OTHER);
                            final ButtonType buttonDiscard = new ButtonType("Discard own", ButtonData.CANCEL_CLOSE);
                            Optional<ButtonType> saveChanges = showAlert(AlertType.CONFIRMATION, alertTitle, alertHeaderText, null, buttonSave, buttonSaveNew, buttonDiscard);

                            if (saveChanges.isPresent()) {
                                if (saveChanges.get().equals(buttonSave)) {
                                    // save own note independent of file system changes
                                    final NoteData saveNote = noteEditor.getUserData();
                                    if (saveNoteWrapper(saveNote.getGroupName(), saveNote.getNoteName(), noteEditor.getNoteText())) {
                                        noteEditor.hasBeenSaved();
                                    }
                                }

                                if (saveChanges.get().equals(buttonSaveNew)) {
                                    // save own note under new name
                                    final NoteData saveNote = noteEditor.getUserData();
                                    final String newNoteName = uniqueNewNoteNameForGroup(saveNote.getGroupName());
                                    if (createNoteWrapper(saveNote.getGroupName(), newNoteName)) {
                                        if (saveNoteWrapper(saveNote.getGroupName(), newNoteName, noteEditor.getNoteText())) {
                                            noteEditor.hasBeenSaved();

                                            // we effectively just renamed the note...
                                            saveNote.setNoteName(newNoteName);
                                            noteEditor.setUserData(saveNote);
                                            // System.out.printf("User data updated\n");
                                        }
                                    }
                                }

                                if (saveChanges.get().equals(buttonDiscard)) {
                                    // nothing to do for StandardWatchEventKinds.ENTRY_DELETE - initFromDirectory(true) will take care of this
                                    if (StandardWatchEventKinds.ENTRY_MODIFY.equals(eventKind)) {
                                        // re-load into edit for StandardWatchEventKinds.ENTRY_MODIFY
                                        final NoteData loadNote = noteEditor.getUserData();
                                        loadNote.setNoteFileContent(OwnNoteFileManager.getInstance().readNote(loadNote));
                                        noteEditor.setNoteText(loadNote, loadNote.getNoteFileContent());
                                    }
                                }
                            }
                        }
                    }
                }
            
                // show only notes for selected group
                final String curGroupName = myGroupList.getCurrentGroup().getGroupName();

                initFromDirectory(true);
                selectFirstOrCurrentNote();
                
                // but only if group still exists in the list!
                final List<String> allGroupNames = new LinkedList<>(realGroupNames);
                allGroupNames.add(GroupData.ALL_GROUPS);
                allGroupNames.add(GroupData.NOT_GROUPED);
                
                if (allGroupNames.contains(curGroupName)) {
                    setGroupNameFilter(curGroupName);
                }
                
                filesInProgress.remove(fileName);
            });
        }
        
        return result;
    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    // TF, 20160630: refactored from "getClassicLook" to show its real meeaning
    public OwnNoteEditorParameters.LookAndFeel getCurrentLookAndFeel() {
        return currentLookAndFeel;
    }

    // TF, 20170528: determine color from tabpane for groupname for existing colors
    public String getExistingGroupColor(final String groupName) {
        
        return groupsPane.getMatchingPaneColor(groupName);
    }

    // TF, 20160703: to support coloring of notes table view for individual notes
    // TF, 20170528: determine color from groupname for new colors
    public String getNewGroupColor(String groupName) {
        final FilteredList<GroupData> filteredGroups = OwnNoteFileManager.getInstance().getGroupsList().filtered((GroupData group) -> {
            // Compare group name to filter text.
            return group.getGroupName().equals(groupName); 
        });
        
        String groupColor = "darkgrey";
        if (!filteredGroups.isEmpty()) {
            final GroupData group = (GroupData) filteredGroups.get(0);
            final int groupIndex = OwnNoteFileManager.getInstance().getGroupsList().indexOf(group);
            
            // TF, 20170122: "All" & "Not grouped" have their own colors ("darkgrey", "lightgrey"), rest uses list of colors
            switch (groupIndex) {
                case 0: groupColor = "darkgrey";
                        break;
                case 1: groupColor = "lightgrey";
                        break;
                default: groupColor = groupColors[groupIndex % groupColors.length];
                        break;
            }
            //System.out.println("Found group: " + groupName + " as number: " + groupIndex + " color: " + groupColor);
        }
        return groupColor;
    }
    
    // TF, 20160816: wrapper for alerts that stores the alert as long as its shown - needed for testing alerts with testfx
    public Optional<ButtonType> showAlert(final AlertType alertType, final String title, final String headerText, final String contentText, final ButtonType ... buttons) {
        Alert result;
        
        result = new Alert(alertType);
        if (title != null) {
            result.setTitle(title);
        }
        if (headerText != null) {
            result.setHeaderText(headerText);
        }
        if (contentText != null) {
            result.setContentText(contentText);
        }
        
        // add optional buttons
        if (buttons.length > 0) {
            result.getButtonTypes().setAll(buttons);
        }
        
        // add info for later lookup in testfx - doesn't work yet
        result.getDialogPane().getStyleClass().add("alertDialog");
        result.initOwner(borderPane.getScene().getWindow());

        // get button pressed
        Optional<ButtonType> buttonPressed = result.showAndWait();
        result.close();
        
        return buttonPressed;
    }
    
    public List<NoteData> getNotesWithText(final String searchText) {
        return OwnNoteFileManager.getInstance().getNotesWithText(searchText);
    }
}
