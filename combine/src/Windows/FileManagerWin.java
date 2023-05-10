package Windows;

import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

public class FileManagerWin extends Win {
        private ObservableMap<FileInfo, Object> fileSystemTree;
        private Image folderImage = new Image("file:res/icons/folder.png");
        private Image fileImage = new Image("file:res/icons/file.png");
        protected FileInfo curFile=null;
        protected String curPath=System.getProperty("user.dir") + File.separator + "File";;
        private TreeView<String> fileList;
        private String root_path=System.getProperty("user.dir") + File.separator + "File";
        private FileTreeItem rootItem;
        protected String text=null;
        public static PipedInputStream fileOutput=null;
        public static PipedOutputStream fileInput=null;
        public FileManagerWin(Controller controller) throws IOException {

                super(controller, "文件管理器", 500, 500);

                fileSystemTree=controller.iniFileTree(root_path);

                BorderPane root = new BorderPane();

                // 左侧目录树
                fileList = new TreeView<>();
                rootItem = new FileTreeItem("我的电脑",new FileInfo("File","d---",root_path),null);
                rootItem.setGraphic(new ImageView(new Image("file:res/icons/myComputer.png")));
                rootItem.setExpanded(true);
                buildTree(fileSystemTree,rootItem);
                fileList.setRoot(rootItem);

                root.setCenter(fileList);

                // 底部状态栏
                HBox statusBox = new HBox();
                statusBox.setPadding(new Insets(5));
                statusBox.setStyle("-fx-background-color: #f5f5f5");
                root.setBottom(statusBox);

                // 菜单栏
                MenuBar menuBar = new MenuBar();
                Menu fileMenu = new Menu("选项");
                MenuItem newFileItem = new MenuItem("新建文件");
                MenuItem newFolderItem = new MenuItem("新建文件夹");
                MenuItem runMenuItem = new MenuItem("运行");
                MenuItem editMenuItem = new MenuItem("编辑");
                MenuItem deleteMenuItem = new MenuItem("删除");
                newFileItem.setDisable(true);
                newFolderItem.setDisable(true);
                runMenuItem.setDisable(true);
                deleteMenuItem.setDisable(true);
                editMenuItem.setDisable(true);
                fileMenu.getItems().addAll(newFileItem, newFolderItem,runMenuItem,editMenuItem,deleteMenuItem);
                menuBar.getMenus().addAll(fileMenu);
                root.setTop(menuBar);

                super.scene.setCenter(root);

                fileList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null && newValue instanceof FileTreeItem) {
                                // 强制类型转换为FileTreeItem
                                FileTreeItem item = (FileTreeItem) newValue;
                                //获取父节点信息
                                FileTreeItem parentItem=(FileTreeItem)item.getParent();
                                if(parentItem!=null)
                                        curPath=parentItem.getFileInfo().path;
                                else
                                        curPath=root_path;
                                // 获取自定义属性值
                                curFile = item.getFileInfo();

                        }
                });

                fileList.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, new EventHandler<javafx.scene.input.MouseEvent>() {
                        @Override
                        public void handle(MouseEvent event) {
                                if(curFile!=null){

                                        if(!curFile.path.equals(root_path))
                                                deleteMenuItem.setDisable(false);
                                        if(curFile.type.equals("d---")){
                                                newFileItem.setDisable(false);
                                                newFolderItem.setDisable(false);
                                                runMenuItem.setDisable(true);
                                                editMenuItem.setDisable(true);
                                        }else{
                                                newFileItem.setDisable(true);
                                                if(curFile.type.contains("e"))
                                                        runMenuItem.setDisable(false);

                                                else
                                                        runMenuItem.setDisable(true);
                                                if(curFile.type.contains("x"))
                                                        editMenuItem.setDisable(false);
                                                else
                                                        editMenuItem.setDisable(true);
                                                newFileItem.setDisable(true);
                                                newFolderItem.setDisable(true);
                                        }
                                }
                        }
                });

                deleteMenuItem.setOnAction(event -> {
                        try {
                                controller.communicate("cd ");
                                Thread.sleep(100);
                                String command="rm "+"-r "+curFile.path.replace(root_path,"");
                                controller.communicate(command);
                                TreeItem<String> parentItem=fileList.getSelectionModel().getSelectedItem().getParent();
                                parentItem.getChildren().remove(fileList.getSelectionModel().getSelectedItem());
                                fileSystemTree=controller.iniFileTree(root_path);
                                fileList.refresh();
                        } catch (IOException e) {
                                throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                        }
                });
                runMenuItem.setOnAction(event -> {
                        try {
                                controller.communicate("cd ");
                                Thread.sleep(100);
                                String command="exec "+curFile.path.replace(root_path,"");
                                controller.communicate(command);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });
                editMenuItem.setOnAction(event->{
                        try {
                                if(!controller.isOpen("编辑: "+curFile.name)){
                                        fileInput=new PipedOutputStream();
                                        fileOutput=new PipedInputStream();
                                        fileInput.connect(fileOutput);
                                        controller.communicate("cd ");
                                        Thread.sleep(100);
                                        String command="vi-ui "+curFile.path.replace(root_path+File.separator,"");
                                        controller.communicate(command);
                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        bytesRead = fileOutput.read(buffer);
                                        String data = new String(buffer, 0, bytesRead);
                                        FileEditWin fileEditWin=new FileEditWin(controller,curFile);
                                        controller.newText(fileEditWin,"编辑: "+curFile.name);
                                        if(!data.equals("null"))
                                                fileEditWin.setText(data);
                                        fileInput.close();
                                        fileOutput.close();
                                        fileInput=null;
                                        fileOutput=null;
                                }
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });
                newFileItem.setOnAction(event -> {
                        try {
                                controller.newText(new TextWin(controller,"File",super.scene),"命名");
                                super.scene.setDisable(true);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });
                newFolderItem.setOnAction(event -> {
                        try {
                                controller.newText(new TextWin(controller,"Folder",super.scene),"命名");
                                super.scene.setDisable(true);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });
        }

        private void buildTree(ObservableMap<FileInfo, Object> map, FileTreeItem parentItem) {
                for (Map.Entry<FileInfo, Object> entry : map.entrySet()) {
                        FileTreeItem item = new FileTreeItem(entry.getKey().name,entry.getKey(),null);

                        if (entry.getValue() instanceof Map) {
                                item.setGraphic(new ImageView(folderImage));
                                parentItem.getChildren().add(item);
                                buildTree((ObservableMap<FileInfo, Object>) entry.getValue(), item);
                        }else {
                                if(!item.getFileInfo().type.equals("unc")){
                                        item.setGraphic(new ImageView(fileImage));
                                        parentItem.getChildren().add(item);
                                }
                        }
                }
        }

        public class FileTreeItem extends TreeItem<String> {
                private FileInfo fileInfo;

                public FileTreeItem(String value,FileInfo fileInfo,File file) {
                        super(value);
                        this.fileInfo=fileInfo;
                }

                public FileInfo getFileInfo(){
                        return fileInfo;
                }

        }
        private class TextWin extends Win{
                private TextField textField;
                public TextWin(Controller controller,String type,BorderPane scene) throws IOException {
                        super(controller, "命名", 300, 100);
                        super.minButton.setDisable(true);
                        VBox pane = new VBox();
                        Label message=new Label("");
                        fileInput=new PipedOutputStream();
                        fileOutput=new PipedInputStream();
                        fileOutput.connect(fileInput);

                        Button confirmButton=new Button("确定");
                        Button cancelButton=new Button("取消");

                        HBox buttomBar = new HBox();
                        buttomBar.setPrefHeight(50);
                        buttomBar.setAlignment(Pos.CENTER);
                        buttomBar.setSpacing(20);

                        buttomBar.getChildren().addAll(confirmButton,message,cancelButton);

                        textField = new TextField();
                        textField.setMaxSize(295,35);
                        textField.setMinSize(295,35);

                        pane.getChildren().addAll(textField,buttomBar);

                        super.scene.setCenter(pane);
                        confirmButton.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
                                @Override
                                public void handle(MouseEvent event) {
                                        text=textField.getText();
                                        if(!text.equals("")&&type.equals("File")){
                                                byte[] buffer = new byte[1024];
                                                int bytesRead;
                                                try {
                                                        controller.communicate("cd ");
                                                        Thread.sleep(100);
                                                        String command="mkf "+curFile.path.replace(root_path,"")+File.separator+text+" 100";
                                                        controller.communicate(command);
                                                        bytesRead = fileOutput.read(buffer);
                                                        String data = new String(buffer, 0, bytesRead);
                                                        if(!data.equals("success"))
                                                                message.setText("相同名字的文件已存在");
                                                        else{
                                                                controller.closeWin("命名");
                                                                fileInput.close();
                                                                fileOutput.close();
                                                                fileInput=null;
                                                                String newPath=curFile.path+File.separator+text;
                                                                FileInfo newFile=new FileInfo(text,"crwx",newPath);
                                                                TreeItem<String> parentItem=fileList.getSelectionModel().getSelectedItem();
                                                                FileTreeItem newItem=new FileTreeItem(text,newFile,new File(newFile.path));
                                                                newItem.setGraphic(new ImageView(fileImage));
                                                                parentItem.getChildren().add(newItem);
                                                                fileSystemTree=controller.iniFileTree(root_path);
                                                                fileList.refresh();
                                                                scene.setDisable(false);
                                                        }
                                                } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                }
                                        }else if(!text.equals("")&&type.equals("Folder")){
                                                byte[] buffer = new byte[1024];
                                                int bytesRead;
                                                try {
                                                        controller.communicate("cd ");
                                                        Thread.sleep(100);
                                                        String command="mkdir "+curFile.path.replace(root_path,"")+File.separator+text;
                                                        controller.communicate(command);
                                                        bytesRead = fileOutput.read(buffer);
                                                        String data = new String(buffer, 0, bytesRead);
                                                        if(!data.equals("success"))
                                                                message.setText("相同名字的文件夹已存在");
                                                        else{
                                                                controller.closeWin("命名");
                                                                fileInput.close();
                                                                fileOutput.close();
                                                                fileInput=null;
                                                                String newPath=curFile.path+File.separator+text;
                                                                FileInfo newFile=new FileInfo(text,"d---",newPath);
                                                                TreeItem<String> parentItem=fileList.getSelectionModel().getSelectedItem();
                                                                FileTreeItem newItem=new FileTreeItem(text,newFile,new File(newFile.path));
                                                                newItem.setGraphic(new ImageView(folderImage));
                                                                parentItem.getChildren().add(newItem);
                                                                fileSystemTree=controller.iniFileTree(root_path);
                                                                fileList.refresh();
                                                                scene.setDisable(false);
                                                        }
                                                } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                }
                                        }
                                }
                        });
                        cancelButton.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
                                @Override
                                public void handle(MouseEvent mouseEvent) {
                                        scene.setDisable(false);
                                        controller.closeWin("命名");
                                }
                        });
                }
        }
}
class FileInfo {
        String name;
        String type;
        String path;

        public FileInfo(String name, String type,String path) {
                this.name = name;
                this.type = type;
                this.path = path;
        }
}
