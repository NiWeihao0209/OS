package Windows;

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

    private TableView<DeviceInfo> deviceTable = new TableView<>();
    private ObservableList<DeviceInfo> deviceList = FXCollections.observableArrayList();

	public DeviceWin(Controller controller) {
		
		super(controller, "设备管理器", 500, 500);
		
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5, 5, 5, 5));

        // 工具栏
        HBox toolbar = new HBox(5);
        Button refreshBtn = new Button("Refresh");
        toolbar.getChildren().addAll(refreshBtn);
        root.setTop(toolbar);
        
        // 设备列表
        deviceTable.setEditable(false);
        //创建一列表格，表格接受的输入为deviceInfo类型，显示的类型为ImageView
        TableColumn<DeviceInfo,ImageView> iconColumn = new TableColumn<>("Icon");
        //将表格显示的内容与deviceInfo类内部的属性绑定
        iconColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getIcon()));
        TableColumn<DeviceInfo, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getName()));
        TableColumn<DeviceInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getStatus()));
        TableColumn<DeviceInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getType()));
        TableColumn<DeviceInfo, String> manufacturerColumn = new TableColumn<>("Manufacturer");
        manufacturerColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getManufacturer()));
        TableColumn<DeviceInfo, String> driverColumn = new TableColumn<>("Driver");
        driverColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDriver()));
        deviceTable.getColumns().addAll(iconColumn, nameColumn, statusColumn,typeColumn,manufacturerColumn,driverColumn);
        deviceTable.setItems(deviceList);
        root.setCenter(deviceTable);

        refreshDevices();

        // 窗口设置
        super.scene.setCenter(root);
    }

    private void refreshDevices() {
    	deviceList.clear();
    	DeviceInfo test=new DeviceInfo("打印机","占用",new ImageView("file:res/icons/printer.png"),"local","bupt","1.0");
    	DeviceInfo test2=new DeviceInfo("摄像头","空闲",new ImageView("file:res/icons/camera.png"),"local","bupt","1.0");
    	deviceList.addAll(test,test2);
    }
    
    private class DeviceInfo {
        private String name;
        private String status;
        private String type;
        private String manufacturer;
        private String driver;
        private ImageView icon;

        public DeviceInfo(String name,String status,ImageView icon,String type,String manufacturer,String driver) {
            this.name=name;
        	this.status =status;
        	this.icon=icon;
        	this.type=type;
        	this.manufacturer=manufacturer;
        	this.driver=driver;
        }
        
        public ImageView getIcon() {
        	return icon;
        }
        
        public String getName() {
        	return name;
        }
        
        public String getStatus() {
        	return status;
        }
        
        public String getType() {
        	return type;
        }
        
        public String getManufacturer() {
        	return manufacturer;
        }
        
        public String getDriver() {
        	return driver;
        }
    }
}
