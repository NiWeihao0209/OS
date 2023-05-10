package Windows;

import SystemCore.Kernel;
import SystemCore.Memory;
import SystemCore.ProcessControlBlock;
import SystemCore.ProcessManager;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
    public static Label memoryUsageLabel=new Label("内存使用:0%");;
    private ProcessControlBlock selectedProcess;
    public TaskManagerWin(Controller controller) {

        super(controller, "任务管理器", 500, 500);

        BorderPane root = new BorderPane();
        root.setPrefSize(500, 600);

        // Top bar
        HBox topBar = new HBox();
        topBar.setPrefHeight(50);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setSpacing(125);
        topBar.getChildren().add(memoryUsageLabel);

        Button pageInspectButton=new Button("页框监视");
        Button killBtn = new Button("终止进程");
        killBtn.setDisable(true);
        if(Memory.testName.equals("ca")){
            pageInspectButton.setDisable(true);
        }
        topBar.getChildren().addAll(pageInspectButton,killBtn);

        root.setTop(topBar);

        // Center table
        taskTable = new TableView<>();
        taskTable.setEditable(false);

        TableColumn<ProcessControlBlock, String> nameColumn = new TableColumn<>("进程名称");
        nameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getName()));

        TableColumn<ProcessControlBlock, String> pidColumn = new TableColumn<>("PID");
        pidColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPid()));

        TableColumn<ProcessControlBlock, String> sizeColumn = new TableColumn<>("进程大小");

        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));

        TableColumn<ProcessControlBlock, String> statusColumn = new TableColumn<>("进程状态");
        statusColumn.setPrefWidth(100);
        statusColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getStatus()));

        TableColumn<ProcessControlBlock, String> priorityColumn = new TableColumn<>("进程优先级");
        priorityColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPriority()));

        taskTable.getColumns().addAll(nameColumn,pidColumn,sizeColumn,statusColumn,priorityColumn);

        taskTable.setItems(ProcessManager.pcbList);
        root.setCenter(taskTable);

        super.scene.setCenter(root);

        taskTable.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                selectedProcess=taskTable.getSelectionModel().getSelectedItem();
                if(selectedProcess!=null&&!selectedProcess.getStatus().equals("terminated"))
                    killBtn.setDisable(false);
                else{
                    killBtn.setDisable(true);
                }
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
        pageInspectButton.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    if(!controller.isOpen("页框监视器"))
                        controller.newWin("页框监视器");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    public static void updateMemory(Float usedRate){
        memoryUsageLabel.setText("内存使用:  "+usedRate*100+"%    ");
    }
}
