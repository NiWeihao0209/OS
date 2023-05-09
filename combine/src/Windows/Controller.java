package Windows;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.stage.Stage;
import SystemCore.Kernel;

import java.io.*;
import java.util.*;


public class Controller implements Runnable{

	private Desktop desktop=new Desktop(this);

	private TerminalWin terminal;

	private TaskManagerWin taskManager;

	private FileManagerWin fileManager;

	private DeviceWin device;

	private HashMap<String,Win> winHashMap = new HashMap<>();

	public Stage primaryStage;

	private HashSet<String> keyWord=new HashSet<>(Arrays.asList(new String[] { "gcc","vi","vi-ui","re","ls","cd","mkdir","mon","rm","dss","exec","dms","td","mkf","kill","ps","rs","man","sv"}));

	static int Level=0;//一级权限等级,只能识别基础命令

	static PipedInputStream[] output;
	static PipedOutputStream[] input;

	public Controller(Stage primaryStage) {
		output=Kernel.output;
		input=Kernel.input;
		this.primaryStage=primaryStage;
		primaryStage.setScene(new Scene(desktop.getBase()));

	}

	public void newWin(String name) throws IOException {
		switch(name) {
			case "终端":
				terminal=new TerminalWin(this);
				winHashMap.put(name, terminal);
				desktop.addWin(terminal);
				break;
			case "任务管理器":
				taskManager=new TaskManagerWin(this);
				winHashMap.put(name, taskManager);
				desktop.addWin(taskManager);
				break;
			case "文件管理器":
				fileManager=new FileManagerWin(this);
				winHashMap.put(name, fileManager);
				desktop.addWin(fileManager);
				break;
			case "设备管理器":
				device=new DeviceWin(this);
				winHashMap.put(name, device);
				desktop.addWin(device);
				break;
		}
	}

	public void newText(Win textWin,String name){
		winHashMap.put(name,textWin);
		desktop.addWin(textWin);
	}
	public void closeWin(String name) {
		desktop.deleteWin(winHashMap.get(name),name);
		winHashMap.remove(name);
	}

	public Boolean isOpen(String name) {
		return winHashMap.containsKey(name);
	}

	public void setVisible(String name) {
		winHashMap.get(name).scene.setVisible(true);
	}
	//判断绝对路径是否存在
	public Boolean isExist(String path) {
		File file=new File(path);
		return file.exists();
	}

	public void communicate(String command) throws IOException {

		if(!keyWord.contains(command.split(" ")[0])&& Level==0){
			TerminalWin.setText("非法输入!\n\n");
		} else {
			if(command.split(" ")[0].equals("vi")){
				String path=command.split(" ")[1];
				int lastIndex=path.lastIndexOf(File.separator);
				String name=path;
				if(lastIndex!=-1&&lastIndex<path.length()-1){
					name=path.substring(lastIndex+1);
				}
				//判断file是否存在
				if(!isExist(System.getProperty("user.dir") + File.separator + "File"+File.separator+command.split(" ")[1])){
					input[0].write("jr file not exist".getBytes());
					input[0].flush();
					return;
				}
				System.out.println("s:" + command);
				//开启写管道
				input[0].write(command.getBytes());
				input[0].flush();
				byte[] buffer = new byte[1024];
				int bytesRead;
				FileManagerWin.fileOutput=new PipedInputStream();
				FileManagerWin.fileInput=new PipedOutputStream();
				FileManagerWin.fileOutput.connect(FileManagerWin.fileInput);
				bytesRead = FileManagerWin.fileOutput.read(buffer);
				String data = new String(buffer, 0, bytesRead);
				FileInfo file=new FileInfo(name,"",System.getProperty("user.dir") + File.separator + "File"+File.separator+command.split(" ")[1]);

				FileEditWin fileEditWin=new FileEditWin(this,file);
				newText(fileEditWin,"编辑: "+file.name);
				if(!data.equals("null"))
					fileEditWin.setText(data);
				FileManagerWin.fileInput.close();
				FileManagerWin.fileOutput.close();
				FileManagerWin.fileInput=null;
				FileManagerWin.fileOutput=null;
			}else{
				if(command.split(" ")[0].equals("vi-ui"))
					command=command.replace("vi-ui","vi");
				System.out.println("s:" + command);
				//开启写管道
				input[0].write(command.getBytes());
				input[0].flush();
			}
		}
	}

	public ObservableMap<FileInfo, Object> iniFileTree(String now_path) {
		// now_path是当前递归到的绝对路径
		/* 文件树采用Map形式，文件名为键，
		 * 当该文件为文件夹时，其值为一个Map，
		 * 否则，其值为长度为4的字符串，表示类型 / 读 / 写 / 执行。*/
		File folder = new File(now_path);
		File[] file_list = folder.listFiles();
		ObservableMap<FileInfo, Object> part_of_tree = FXCollections.observableHashMap();;  // 当前文件夹对应的Map
		for (File file : file_list) {
			String file_path = file.getAbsolutePath();
			if (file.isDirectory()) {  // 文件夹为键，其值为Map
				part_of_tree.put(new FileInfo(file.getName(),"d---",file_path), iniFileTree(file_path));
			} else {
				try (BufferedReader reader = new BufferedReader(new FileReader(file_path))) {
					StringBuilder stringBuilder = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						stringBuilder.append(line);
					}
					String fileContent = stringBuilder.toString();
					JSONObject data = JSON.parseObject(fileContent); // 读取json字符串，便于之后提取type
					part_of_tree.put(new FileInfo(file.getName(),data.getString("type"),file_path),file);
				} catch (IOException e) {
					// 处理读取或解析 JSON 异常
					System.out.println("error: Json exception");
				}
			}
		}
		return part_of_tree;
	}

	public void run() {
		try {

			// Read data from shell's output and process it
			int bytesRead;
			byte[] buffer = new byte[1024];
			while ((bytesRead = output[1].read(buffer)) != -1) {
				String data = new String(buffer, 0, bytesRead);
				//打印data
				//System.out.println(data);
				if(terminal!=null)
					TerminalWin.setText(data);
				// Process the data as needed
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
