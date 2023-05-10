package Windows;

import SystemCore.Hardware;
import SystemCore.HardwareManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.Random;

public class DeviceWin extends Win {

    private TableView<Hardware> deviceTable = new TableView<>();

	public DeviceWin(Controller controller) {
		
		super(controller, "设备管理器", 650,500);
		
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5, 5, 5, 5));

        // 工具栏
        HBox toolbar = new HBox(5);
        //toolbar.getChildren().addAll(refreshBtn);
        root.setTop(toolbar);
        
        // 设备列表
        deviceTable.setEditable(false);
        //创建一列表格，表格接受的输入为deviceInfo类型，显示的类型为ImageView
        TableColumn<Hardware,ImageView> iconColumn = new TableColumn<>("设备图标");
        //将表格显示的内容与deviceInfo类内部的属性绑定
        iconColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getIcon()));
        TableColumn<Hardware, String> nameColumn = new TableColumn<>("名称");
        nameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get_hardware_cate()));
        TableColumn<Hardware, String> statusColumn = new TableColumn<>("设备状态");
        statusColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getStatus()));
        TableColumn<Hardware, String> pidColumn = new TableColumn<>("当前进程PID");
        pidColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPid()));
        TableColumn<Hardware, String> startColumn = new TableColumn<>("开始时间");
        startColumn.setPrefWidth(200);
        startColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getStartTime()));
        TableColumn<Hardware, String> planColumn = new TableColumn<>("计划使用时间");
        planColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPlanTime()));
        deviceTable.getColumns().addAll(iconColumn, nameColumn, statusColumn,pidColumn,startColumn,planColumn);
        deviceTable.setItems(HardwareManager.hardwares);
        root.setCenter(deviceTable);

        // 窗口设置
        super.scene.setCenter(root);
    }
}
