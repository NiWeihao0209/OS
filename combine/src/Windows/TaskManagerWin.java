package Windows;

import SystemCore.Kernel;
import SystemCore.ProcessControlBlock;
import SystemCore.ProcessManager;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class TaskManagerWin extends Win {

    private static TableView<ProcessControlBlock> taskTable=new TableView<>();
    private Label cpuUsageLabel;

    private ProcessControlBlock selectedProcess;
	public TaskManagerWin(Controller controller) {
		
		super(controller, "任务管理器", 500, 500);
		
        BorderPane root = new BorderPane();
        root.setPrefSize(800, 600);

        // Top bar
        HBox topBar = new HBox();
        topBar.setPrefHeight(50);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Memory Usage:0%		");
        topBar.getChildren().add(titleLabel);

        cpuUsageLabel = new Label("CPU Usage: 0%        ");
        topBar.getChildren().add(cpuUsageLabel);

        Button killBtn = new Button("终止进程");
        killBtn.setDisable(true);
        topBar.getChildren().add(killBtn);

        root.setTop(topBar);

        // Center table
        taskTable = new TableView<>();
        taskTable.setEditable(false);

        TableColumn<ProcessControlBlock, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getName()));

        TableColumn<ProcessControlBlock, String> pidColumn = new TableColumn<>("PID");
        pidColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPid()));

        TableColumn<ProcessControlBlock, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));

        TableColumn<ProcessControlBlock, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getStatus()));

        TableColumn<ProcessControlBlock, String> priorityColumn = new TableColumn<>("Priority");
        priorityColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPriority()));
        
        taskTable.getColumns().addAll(nameColumn,pidColumn,sizeColumn,statusColumn,priorityColumn);

        taskTable.setItems(ProcessManager.pcbList);
        root.setCenter(taskTable);

        super.scene.setCenter(root);

        taskTable.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                selectedProcess=taskTable.getSelectionModel().getSelectedItem();
                if(selectedProcess!=null)
                    killBtn.setDisable(false);
            }
        });
        killBtn.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    controller.communicate("kill "+selectedProcess.pid);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
	}

}
