package Windows;

import SystemCore.Hardware;
import SystemCore.HardwareManager;
import SystemCore.Memory;
import SystemCore.UsingFrameBar;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.Queue;

public class PageInspectWin extends Win{

    private TableView<UsingFrameBar> pageTable = new TableView<>();

    public PageInspectWin(Controller controller) {
        super(controller, "页框监视器", 1320, 500);
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5, 5, 5, 5));
        //禁用最小化
        super.minButton.setDisable(true);
        // 工具栏
        HBox toolbar = new HBox(5);
        //toolbar.getChildren().addAll(refreshBtn);
        root.setTop(toolbar);

        // 设备列表
        pageTable.setEditable(false);
        //创建一列表格，表格接受的输入为deviceInfo类型，显示的类型为ImageView
        for (int i=0;i<16;i++){
            TableColumn<UsingFrameBar,String> column = new TableColumn<>("页框"+i);
            //将表格显示的内容与deviceInfo类内部的属性绑定
            int finalI = i;
            column.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPage(finalI)));
            pageTable.getColumns().add(column);
        }
        pageTable.setItems(Memory.pageFrameList);
        root.setCenter(pageTable);

        // 窗口设置
        super.scene.setCenter(root);

    }
}
