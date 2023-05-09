package Windows;

import SystemCore.FileManager;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TerminalWin extends Win {
	
	//历史输入
    private List<String> history = new ArrayList<>();

    private static TextArea textArea;

    private TextField textField;
    //缓存大小
    private int size=0;
    
    private int index=-1;
    
    private boolean isRecorded=false;
    
    private String command;

    String current_path="";
	
	public TerminalWin(Controller controller) {
		
		super(controller, "终端", 600, 450);
		
        VBox pane = new VBox();

        textArea = new TextArea();
        textArea.setMaxSize(600,395);
        textArea.setMinSize(600,395);
        textArea.setStyle("-fx-text-fill:white");
        textArea.setStyle("-fx-control-inner-background: black;");
        textArea.setEditable(false);

        textField = new TextField();
        textField.setMaxSize(600,30);
        textField.setMinSize(600,30);

        textField.setStyle("-fx-text-fill:white");
        textField.setStyle("-fx-control-inner-background: black;");
        pane.getChildren().addAll(textArea ,textField);
        super.scene.setCenter(pane);

        textField.setOnKeyReleased(new EventHandler<javafx.scene.input.KeyEvent>() {
        	
            @Override
            public void handle(javafx.scene.input.KeyEvent event) {
            	if (event.getCode() == KeyCode.ENTER){
                    command = textField.getText();
                    if(!command.equals("")) {
                        recordHistory(command);
                        history.removeIf(e->e.equals(""));
                        size=history.size();
                        index=size-1;
                        isRecorded=false;
                        textField.positionCaret(19);
                        textField.clear();

                        if(command.equals("clear"))
                            textArea.setText("");
                        else{
                            String[] words = command.split("\\|");
                            //打印words
                            for(int i=0;i<words.length;i++){
                                System.out.println("a:"+words[i]);
                            }
                            try {
                                for (int i = 0; i < words.length; i++) {
                                    String command=words[i].trim();
                                    controller.communicate(command);

                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        textArea.appendText(FileManager.current_working_path +"$ "+command+'\n');
                    }
                }
                if(event.getCode()==KeyCode.F1) {
                	if(size>0) {
                        if(!isRecorded) {
                            command = textField.getText();
                            recordHistory(command);
                            isRecorded=true;
                            textField.setText(history.get(index));
                        }
                        else if(index>0) {
                        	index--;
                        	textField.setText(history.get(index));
                        }
                    	
                	}
                }
                if(event.getCode()==KeyCode.F2) {
                	if(index!=size-1&&size>0) {
                		index++;
                		textField.setText(history.get(index));
                	}
                }
                
            }
        });
	}
	
	private void recordHistory(String command) {
		history.add(command);
		if(size<10)
			size++;
		else
			history.remove(0);
	}

    protected static void setText(String text){
        textArea.appendText(text);
    }
}
