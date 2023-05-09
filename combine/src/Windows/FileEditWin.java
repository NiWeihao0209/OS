package Windows;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.io.PipedOutputStream;

public class FileEditWin extends Win{

    private TextArea textArea;
    private FileInfo file;
    public FileEditWin(Controller controller,FileInfo file) {
        super(controller, "编辑: "+file.name, 500, 500);
        this.file=file;
        super.minButton.setDisable(true);
        VBox pane = new VBox();

        Label message=new Label("");

        Button saveButton=new Button("保存");
        Button quitButton=new Button("退出");

        HBox buttomBar = new HBox();
        buttomBar.setPrefHeight(50);
        buttomBar.setAlignment(Pos.CENTER);
        buttomBar.setSpacing(20);

        buttomBar.getChildren().addAll(saveButton,message,quitButton);

        textArea = new TextArea();
        textArea.setMaxSize(495,435);
        textArea.setMinSize(495,435);

        pane.getChildren().addAll(textArea,buttomBar);

        super.scene.setCenter(pane);

        saveButton.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent mouseEvent) {
                String text=textArea.getText();
                try {
                    controller.communicate("cd ");
                    String command="sv "+file.path.replace(System.getProperty("user.dir") + File.separator + "File"+File.separator,"")+" "+text;
                    controller.communicate(command);
                    message.setText("保存成功");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        quitButton.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                controller.closeWin("编辑: "+file.name);
            }
        });

    }
    public void setText(String text){
        textArea.setText(text);
    }
}
