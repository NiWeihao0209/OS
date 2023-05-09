package Windows;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Win {
	
    private int thickness = 25;//窗口边框宽度
    private double x,y;//实现拖动效果的参数
    protected BorderPane scene=new BorderPane();;//模拟scene
    private String name;
    private Text winName;
    private Pane topPane=new Pane();//窗口顶部
    private Circle closeButton;
    protected Circle minButton;

    public Win(Controller controller, String name,int width,int height) {
    	
        this.name=name;

    	//设置窗口顶部
    	topPane.setMaxSize(width,thickness);
        topPane.setMinSize(width,thickness);
        
        Rectangle rectangle=new Rectangle(0,0,width-4,thickness);
        rectangle.setFill(Color.valueOf("#393939"));
        
        winName = new Text("  "+name);
        winName.setTranslateX(0);
        winName.setTranslateY(20);
        winName.setFill(Color.WHITE);
        
        closeButton=new Circle(width-0.5*thickness,0.5*thickness,0.3*thickness);
        minButton=new Circle(width-1.5*thickness,0.5*thickness,0.3*thickness);
        closeButton.setFill(Color.valueOf("#EE6B65"));
        minButton.setFill(Color.valueOf("#FCBE53"));
        
        topPane.getChildren().addAll(rectangle,closeButton,minButton,winName);
        //scene = new BorderPane();
        scene.setTop(topPane);
        scene.setMaxSize(width, height);
        scene.setMinSize(width, height);
        scene.setStyle("-fx-border-color: black;"+"-fx-border-width: 2px;"+"-fx-background-color: white;"+"-fx-border-radius: 10px;"+"-fx-background-radius: 10px;");
        
        //拖动准备
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	scene.toFront();
                x = event.getSceneX()-scene.getTranslateX();
                y = event.getSceneY()-scene.getTranslateY();
            }
        });

        //拖动过程
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	scene.setTranslateX(event.getSceneX() - x);
            	scene.setTranslateY(event.getSceneY() - y);
            }
        });
        
        //关闭按钮设置
        closeButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	controller.closeWin(name);
            }
        });
        
        //最小化按钮设置
        minButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	scene.setVisible(false);
            }
        });
        
        //窗口置最前方
        scene.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	scene.toFront();
            }
        });
    }
    
    public Pane getPane() {
    	return scene;
    }
    
    public String getName() {
    	return name;
    }
}
