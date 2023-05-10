package Windows;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Desktop {

	private BorderPane desktop;
	private Pane base;
	private Pane topPane;
	private Pane midPane;
	private Pane leftPane;
	private DoubleProperty baseWidth = new SimpleDoubleProperty((int) Toolkit.getDefaultToolkit().getScreenSize().width);//界面宽度
	private DoubleProperty baseHeight = new SimpleDoubleProperty((int)Toolkit.getDefaultToolkit().getScreenSize().height);//界面高度
	private ImageView background;
	private HashMap<ImageView,String> appHashMap = new HashMap<>();

	public Desktop(Controller controller) {

		desktop=new BorderPane();
		base=new Pane();
		topPane=new Pane();
		midPane=new Pane();
		leftPane=new Pane();

		/*
		 * 顶部面板设置
		 */

		Rectangle rectangle = new Rectangle(0,0,baseWidth.doubleValue(),baseHeight.divide(30).doubleValue());
		rectangle.setFill(Color.valueOf("#222222"));

		//时钟设置
		Text clockText = new Text();
		clockText.setFont(Font.font("Arial", 15));
		clockText.setFill(Color.WHITE);
		clockText.setTranslateX(baseWidth.doubleValue()/2);
		clockText.setTranslateY(25);
		StackPane.setAlignment(clockText, javafx.geometry.Pos.CENTER);
		clockText.setLayoutX(topPane.getWidth()/2 - clockText.getBoundsInLocal().getWidth()/2);

		Timeline timeline = new Timeline(
				new KeyFrame(Duration.seconds(1), event -> {
					DateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
					Calendar calendar = Calendar.getInstance();
					Date now = calendar.getTime();
					String time = dateFormat.format(now);
					clockText.setText(time);
				})
		);
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();

		//关机按钮
		ImageView powerButton=new ImageView("file:res/icons/power.png");
		powerButton.setLayoutX(baseWidth.subtract(34).doubleValue());
		topPane.getChildren().addAll(rectangle,clockText,powerButton);

		/*
		 * 左面板设置
		 */

		//添加app图标
		appHashMap.put(new ImageView("file:res/icons/deviceManager.png"), "设备管理器");
		appHashMap.put(new ImageView("file:res/icons/terminal.png"), "终端");
		appHashMap.put(new ImageView("file:res/icons/taskManager.png"), "任务管理器");
		appHashMap.put(new ImageView("file:res/icons/fileManager.png"), "文件管理器");

		//将app图标放入VBox容器
		VBox appBox=new VBox();
		appBox.setPrefWidth(baseWidth.divide(30).doubleValue());
		appBox.setPrefHeight(baseHeight.subtract(baseHeight.divide(30)).doubleValue()/2);
		appBox.setSpacing(10);
		for (ImageView key : appHashMap.keySet()) {
			key.setFitWidth(baseWidth.divide(30).doubleValue());
			key.setPreserveRatio(true);
			appBox.getChildren().add(key);
		}
		appBox.setAlignment(javafx.geometry.Pos.CENTER);
		appBox.setLayoutY(100);

		//样式设置
		Rectangle rectangle2 = new Rectangle(0,0,baseWidth.divide(30).doubleValue(),baseHeight.subtract(baseHeight.divide(30)).doubleValue());
		rectangle2.setFill(Color.valueOf("#000000"));
		rectangle2.setOpacity(0.5);
		rectangle2.setBlendMode(BlendMode.SRC_OVER);
		leftPane.getChildren().addAll(rectangle2,appBox);

		desktop.setTop(topPane);
		desktop.setLeft(leftPane);
		desktop.setCenter(midPane);

		background = new ImageView("file:res/backgrounds/Desert1.jpg");
		base.getChildren().addAll(background,desktop);


		/*
		 *  属性绑定
		 */
		topPane.minWidthProperty().bind(baseWidth);
		topPane.maxWidthProperty().bind(baseWidth);
		topPane.minHeightProperty().bind(baseHeight.divide(30));
		topPane.maxHeightProperty().bind(baseHeight.divide(30));

		leftPane.minWidthProperty().bind(baseWidth.divide(30));
		leftPane.maxWidthProperty().bind(baseWidth.divide(30));
		leftPane.minHeightProperty().bind(baseHeight.subtract(baseHeight.divide(30)));
		leftPane.maxHeightProperty().bind(baseHeight.subtract(baseHeight.divide(30)));

		midPane.minWidthProperty().bind(baseWidth);
		midPane.maxWidthProperty().bind(baseWidth);
		midPane.minHeightProperty().bind(baseHeight.subtract(baseHeight.divide(30)));
		midPane.maxHeightProperty().bind(baseHeight.multiply(baseHeight.divide(30)));


		base.minWidthProperty().bind(baseWidth);
		base.maxWidthProperty().bind(baseWidth);
		base.minHeightProperty().bind(baseHeight);
		base.maxHeightProperty().bind(baseHeight);

		background.fitHeightProperty().bind(baseHeight);
		background.fitWidthProperty().bind(baseWidth);


		//添加事件
		for (ImageView key : appHashMap.keySet()) {
			key.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if(!controller.isOpen(appHashMap.get(key)))
						key.setStyle("-fx-effect: dropshadow(three-pass-box, #000000, 1.8, 1, 0, 0);");
				}
			});
			key.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if(!controller.isOpen(appHashMap.get(key)))
						key.setStyle("-fx-effect: dropshadow(three-pass-box, #0096c9, 0, 0, 0, 0);");
				}
			});
			key.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if(!controller.isOpen(appHashMap.get(key))) {
						key.setStyle("-fx-effect: dropshadow(three-pass-box, #0096c9, 4.6, 1, 0, 0);");
						try {
							controller.newWin(appHashMap.get(key));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					else {
						controller.setVisible(appHashMap.get(key));
					}
				}
			});

		}

		powerButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				powerButton.setImage(new Image("file:res/icons/power2.png"));
			}
		});
		powerButton.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				powerButton.setImage(new Image("file:res/icons/power.png"));
			}
		});

		powerButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				System.exit(0);
			}
		});

	}

	public Pane getBase() {
		return base;
	}

	public void addWin(Win win) {
		Pane pane=win.getPane();
		pane.setLayoutX(baseWidth.divide(3).doubleValue());
		pane.setLayoutY(baseHeight.divide(4).doubleValue());
		midPane.getChildren().add(pane);
	}

	public void deleteWin(Win win,String name) {
		midPane.getChildren().remove(win.getPane());
		for (Map.Entry<ImageView, String> entry:appHashMap.entrySet()) {
			if(name.equals(entry.getValue()))
				entry.getKey().setStyle("-fx-effect: dropshadow(three-pass-box, #0096c9, 0, 0, 0, 0);");
		}
	}
}
