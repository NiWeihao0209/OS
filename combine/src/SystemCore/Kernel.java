package SystemCore;

import Windows.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;



public class Kernel extends Application {
    //创建文件管理器
    static FileManager fileManager = new FileManager(1024,10,10);
    static Gcc gcc = new Gcc();
    //创建进程管理器
    static ProcessManager processManager;
    //创建内存管理器

    static Controller controller;
    private static HashSet<String> keyWord=new HashSet<>(Arrays.asList(new String[] { "pc","ar","mc","jr","gcc","vi","re","ls","cd","mkdir","rm","dss","exec","dms","td","mkf","kill","ps","rs","man","sv"}));
    //管道0,shell向kernel传输数据
    static String method= "print";
    public static PipedInputStream[] output=new PipedInputStream[2];
    public static PipedOutputStream[] input=new PipedOutputStream[2];

    private DoubleProperty baseWidth = new SimpleDoubleProperty((int) Toolkit.getDefaultToolkit().getScreenSize().width);//界面宽度
    private DoubleProperty baseHeight = new SimpleDoubleProperty((int)Toolkit.getDefaultToolkit().getScreenSize().height);//界面高度
    static Memory memoryManager;
    static HardwareManager hardwareManager;
    public static void main(String[] args) throws IOException {
        Diary.println("System start");
        //初始化内存管理器
        memoryManager = new Memory();
        hardwareManager=new HardwareManager();
        //初始化进程管理器,把内存管理器传进去
        processManager = new ProcessManager(memoryManager, hardwareManager);
        // Create a pipe for communication from shell to kernel
        new Thread(processManager).start();

        for(int i=0;i<2;i++){
            output[i]=new PipedInputStream();
            input[i]=new PipedOutputStream();
        }

        output[0].connect(input[0]);
        output[1].connect(input[1]);

        new Thread(() -> launch(args)).start();

        byte[] buffer = new byte[1024];
        int bytesRead;
        while (true) {
            bytesRead = output[0].read(buffer);
            String data = new String(buffer, 0, bytesRead);
            Diary.println("receive:"+data);
            accept(data);//接受管道传输过来的数据
            // Process the data as needed
        }
    }

    public void start(Stage primaryStage) {

        StackPane readyPane = new StackPane();//欢迎界面
        readyPane.setStyle("-fx-background-color:#000000");

        ImageView wellcome=new ImageView(new Image("file:res/icons/logo.png"));
        readyPane.getChildren().add(wellcome);

        Scene readyScene = new Scene(readyPane);
        readyPane.minWidthProperty().bind(baseWidth);
        readyPane.maxWidthProperty().bind(baseWidth);
        readyPane.minHeightProperty().bind(baseHeight);
        readyPane.maxHeightProperty().bind(baseHeight);

        // 创建一个进度条，并设置进度为0
        ProgressBar progressBar = new ProgressBar(0);

        // 创建一个标签，用于显示进度的百分比
        Label progressLabel = new Label("0%");
        progressLabel.setTextFill(Color.WHITE);

        // 创建一个垂直布局容器，将进度条和标签添加到其中
        VBox root = new VBox();
        root.setPrefWidth(50);
        root.setPrefHeight(50);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        root.setPadding(new Insets(20));
        root.setTranslateY(baseHeight.multiply(0.2).doubleValue());
        root.getChildren().addAll(progressBar, progressLabel);

        readyPane.getChildren().add(root);

        // 模拟进度的增加
        new Thread(() -> {
            for (double progress = 0; progress <= 1.0; progress += 0.01) {
                double finalProgress = progress;
                Platform.runLater(() -> {
                    progressBar.setProgress(finalProgress);
                    progressLabel.setText((int) (finalProgress * 100) + "%");
                });
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Platform.runLater(() -> {
                controller=new Controller(primaryStage);
                new Thread(controller).start();
            });
        }).start();

        primaryStage.setScene(readyScene);
        //primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    public Kernel() throws IOException {
    }

    public static void accept(String word) throws IOException {//主要判断格式此处
        String[] words = word.split(" ");
        //打印words
        if(words[0].equals("ls")){
            deal_ls(word);


        }
        else if(words[0].equals("cd")){
            deal_cd(word);
        }
        else if(words[0].equals("mkdir")){
            deal_mkdir(word);
        }

        else if(words[0].equals("rm")){
            deal_rm(word);
        }
        else if(words[0].equals("dss")){
            deal_dss(word);
        }
        else if(words[0].equals("exec")){
            deal_exec(word);
        }
        else if(words[0].equals("dms")){
            deal_dms(word);
        }

        else if(words[0].equals("td")){
            deal_td(word);
        }
        else if(words[0].equals("mkf")){
            deal_mkf(word);
        }
        else if(words[0].equals("kill")){
            deal_kill(word);
        }
        else if(words[0].equals("ps")){
            deal_ps(word);
        }
        else if(words[0].equals("rs")){
            deal_rs(word);
        }
        else if(words[0].equals("man")){
            deal_man(word);
        }
        else if(words[0].equals("re")){
            deal_re(word);
        }
        else if (words[0].equals("vi")){
            deal_vi(word);
        }
        else if (words[0].equals("gcc")){
            deal_gcc(word);
        }
        else if(words[0].equals("sv")){
            deal_sv(word);
        }
        else if(words[0].equals("jr")){
            deal_jr(word);
        }
        else if (words[0].equals("mc")){
            deal_mc(word);
        }
        else if (words[0].equals("ar")){
            deal_ar(word);

        }
        else if (words[0].equals("pc")){
            deal_pc(word);

        }

        else{
            write("命令不存在");

        }
    }
    private static void deal_pc(String word) throws IOException{
        String[] words = word.split(" ");
        if(words.length==2 &&(words[1].equals("LRU")||words[1].equals("FIFO"))){
            fileManager.change_page(words[1]);

        }
        else{
            try {
                write("pc:错误");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void deal_ar(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==2 &&(words[1].equals("camera")||words[1].equals("printer"))){
            hardwareManager.add_hardware(words[1]);

        }
        else{
            try {
                write("ar:参数错误");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private static void deal_mc(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==2 &&(words[1].equals("ca")||words[1].equals("pa"))){
            fileManager.change_mem(words[1]);

        }
        else{
            try {
                write("mc:错误");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //向管道中写入数据
    public static void write(String word) throws IOException {
        input[1].write((word+"\n").getBytes());
        input[1].flush();

    }
    public static void deal_jr(String word) throws IOException {
        String[] words = word.split(" ");
        //把words第二个值开始拼接为字符串,用空格隔开
        String str = "";
        for (int i = 1; i < words.length; i++) {
            str = str + words[i] + " ";
        }
        write(str);
    }

    //打印list
    public static void printList(List<String> list) throws IOException {
        //打印list,全打印在同一行,用空格隔开
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                write(list.get(i));
            }
        }
    }

    //对命令进行处理,并且调用相应的模块
    public static void deal_re(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            //修改匹配模式
            method = "get";
        }
        else{
            write("re:参数过多");

        }
    }
    public static void deal_ls(String word) throws IOException {

        String[] words = word.split(" ");
        List<String> list = null;
        if(words.length==1){
            //调用ls模块
            list=fileManager.ls("","-a",method);
        }
        //特殊情况:ls+path
        else if(words.length==2){
            if(words[1].equals("-l")){
                //调用ls -l模块
                list=fileManager.ls("","-l",method);
            }
            else if(words[1].equals("-a")){
                //调用ls -a模块
                list=fileManager.ls("","-a",method);
            }
            else if(words[1].equals("-al")){
                //调用ls -al模块
                list=fileManager.ls("","-al",method);
            }
            else{
                //调用ls path模块
                list=fileManager.ls(words[1],"-a",method);

            }
        }
        else if(words.length==3){
            if(words[1].equals("-l")){
                //调用ls -l -a模块
                list=fileManager.ls(words[2],"-l",method);
            }
            else if(words[1].equals("-a")){
                //调用ls -a -l模块
                list=fileManager.ls(words[2],"-a",method);
            }
            else if(words[1].equals("-al") ){
                //调用ls -al模块
                list= fileManager.ls(words[2],"-al",method);
            }
            else{
                write("ls:第二,三个参数格式错误");

            }

        }
        else{
            write("ls:参数过多");

        }
        //打印list
        printList(list);


    }
    public static void deal_cd(String word) throws IOException {
        String list = null;
        String[] words = word.split(" ");
        if(words.length==1){
            //调用cd模块
            fileManager.cd("");

        }
        else if(words.length==2){

            //调用cd path模块
            fileManager.cd(words[1]);
            if(list!=null ){
                write(list);

            }



        }
        else{
            write("cd:参数过多");

        }


    }
    public static void deal_mkdir(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==1){
            write("mkdir:缺少指定路径");

        }
        else if(words.length==2){
            list=fileManager.mkdir(words[1]);
            if(list!=null ){
                write(list);

            }

        }
        else{
            write("mkdir:参数过多");

        }

    }






    public static void deal_rm(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==1){
            write("rm:缺少路径");

        }
        //特殊情况:rm+path
        else if(words.length==2){
            list=fileManager.rm(words[1],"-r");
            if(list!=null ){
                write(list);

            }
        }
        else if(words.length==3){
            if(words[1].equals("-r")){
                //调用r模块
                list=fileManager.rm(words[2],"-r");
            }
            else if(words[1].equals("-f")){
                //调用f模块
                list=fileManager.rm(words[2],"-f");

            }
            else if(words[1].equals("-rf") ){
                //调用rf模块
                list=fileManager.rm(words[2],"-rf");
            }
            else{
                write("ls:第二,三个参数格式错误");

                return;
            }
            if(list!=null ){
                write(list);

            }

        }
        else{
            write("ls:参数过多");

        }

    }
    public static void deal_dss(String word) throws IOException {
        String[] words = word.split(" ");
        List<String> list = null;
        if(words.length==1){
            list=fileManager.dss();
            if(list!=null ){
                write(list.toString());

            }
        }
        else {
            //参数过多
            write("dss:参数过多");

        }

    }
    public static void deal_exec(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            write("exec:缺少指定路径");

        }
        else if(words.length==2){
            //先给filemanager模块传入路径,看是否存在该文件,再接收该文件的内容
            if(fileManager.find(words[1]).equals("error")){
                write("执行文件不存在");

                return;
            }
            //执行该文件的内容
            Map<String, String> map = null;
            map= fileManager.getFileInfo(words[1]);

            //判断map为空
            if (map!=null) {
                List<String> list = null;
                list=fileManager.readContentFromFile(words[1]);
                if(list==null){
                    write("该文件为空文件");

                    return;
                }
                //执行
                processManager.creat_process(map.get("name"),map.get("priority"),map.get("size"),list);
                //controller.refreshTask();
            }
            else {
                write("该文件不属于可执行文件");

            }
        }
        else{
            write("exec:参数过多");

        }
    }
    public static void deal_dms(String word) throws IOException {
        String[] words = word.split(" ");
        List<String> list = null;
        if(words.length==1){
            list=memoryManager.showAllMemory();
            //打印内存信息
            if (list.size()!=0) {
                printList(list);
            }

        }
        else {
            //参数过多
            write("dms:参数过多");

        }
    }

    public static void deal_td(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            fileManager.tidy_disk();
        }
        else {
            //参数过多
            write("td:参数过多");

        }
    }
    public static void deal_mkf(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==3){
            //调用mkf模块
            list=fileManager.mkf(words[1],"crwx",words[2]);
            if(list!=null ){
                write(list);

            }
        }
        else {
            write("mkf:参数不为3");

        }
    }
    public static void deal_kill(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==2){
            try {
                processManager.killProcess(Integer.parseInt(words[1]));
            }catch (Exception e){
                write("kill:存在非法参数");

                return;
            }

        }
        else {
            write("kill:参数不为2");

        }

    }
    public static void deal_ps(String word) throws IOException {
        String[] words = word.split(" ");
        List<ProcessControlBlock> pcbList;
        if(words.length==1){
            //检查进程表
            pcbList=processManager.getProcess();
            //如果pcbList为空,则没有进程
            if(pcbList==null){
                write("没有进程");

                return;
            }
            //遍历pcbList,打印状态,name,pid
            for (int i = 0; i < pcbList.size(); i++) {
                write(("进程名字:" + pcbList.get(i).name + " 进程状态:" + pcbList.get(i).status + " 进程pid:" + pcbList.get(i).pid+"\n"));

            }
        }
        else {
            //参数过多
            write("ps:参数过多");

        }
    }
    public static void deal_rs(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            //获得硬件信息
            ArrayList<Hardware> list=hardwareManager.get_hardware_used_info();
            //把硬件信息传回shell
            for (int i = 0; i < list.size(); i++) {
                write(("种类为:"+list.get(i).hardware_cate+" pid为:"+list.get(i).hardware_pid+" 是否被使用:"+list.get(i).hardware_used));

            }
        }
        else {
            //参数过多
            write("rs:参数过多");

        }
    }
    public static void deal_man(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){

            //读取文件System/Guide文件的每一行,并且转化为字符串list
            String filePath = "System/Guide"; // 文件路径
            List<String> lines = new ArrayList<>(); // 字符串列表

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line); // 将每行字符串添加到列表中
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //打印list
            printList(lines);
        }
        else {
            //检查words每一个元素是否属于字符数组keyword
            for(int i=1;i<words.length;i++){
                if(!keyWord.contains(words[i])){
                    write("man:存在不正确的指令");

                    return;
                }
            }

        }

    }
    public static void deal_vi(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==2){
            //调用vi模块
            list=fileManager.vi(words[1]);
            if(list!=null ){
                write(list);

            }
        }
        else {
            //参数过多
            write(":参数错误");

        }
    }
    public static void deal_gcc(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==2){
            //检测是否存在该文件
            list=fileManager.find(words[1]);
            if(list.equals("error")){
                write("gcc:文件不存在");

                return;
            }

            //调用gcc模块,成功or不符合格式
            if(gcc.compile(words[1],list)){
                write("gcc:编译成功");

            }
            else{
                write("gcc:格式错误,编译失败");

            }
        }
        if(words.length==3){
            //检测是否存在该文件
            list=fileManager.find(words[2]);
            if(list.equals("error")){
                write("gcc:文件不存在");

                return;
            }
            //检查words[1]是否为数字
            try {
                int i = Integer.parseInt(words[1]);
            }catch (Exception e){
                write("gcc:参数错误");

                return;
            }
            //调用gcc模块,成功or不符合格式
            if(gcc.compile(words[1],list)){
                write("gcc:编译成功");

            }
            else{
                write("gcc:编译失败");

            }
        }
        else {
            //参数过多
            write(":参数错误");

        }
    }
    private static void deal_sv(String word) throws IOException {
        String[] words = word.split(" ",3);
        fileManager.sv(words[1],words[2]);
    }
}