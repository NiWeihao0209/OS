import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Kernel {
    //创建文件管理器
    static FileManager fileManager = new FileManager(1024,10,10);
    static Gcc gcc = new Gcc();
    //创建进程管理器
    static ProcessManager processManager;
    //创建内存管理器
    static Memory memoryManager;


    static String[] keyword ={"gcc","vi","re","ls","cd","mkdir","mon","rm","dss","exec","dms","td","mkf","kill","ps","rs","re"};
    //管道0,shell向kernel传输数据
    static String method= "print";
    static PipedInputStream shellOutput = new PipedInputStream();
    static PipedOutputStream shellInput = new PipedOutputStream();
    //管道1,kernel向shell传输数据
    static PipedInputStream shellOutput1 = new PipedInputStream();
    static PipedOutputStream shellInput1 = new PipedOutputStream();
    public static void main(String[] args) throws IOException {
        //初始化内存管理器
        memoryManager = new Memory();
        //初始化进程管理器,把内存管理器传进去
         processManager = new ProcessManager(memoryManager);
        // Create a pipe for communication from shell to kernel
        new Thread(processManager).start();


        shellOutput.connect(shellInput);
        shellOutput1.connect(shellInput1);

        // Create the shell object and pass it the pipe
        Shell shell = new Shell(shellInput, shellOutput,shellInput1,shellOutput1);

        new Thread(shell).start();
        // Read data from shell's output and process it
        byte[] buffer = new byte[1024];
        int bytesRead;
        while (true) {
            bytesRead = shellOutput.read(buffer);
            String data = new String(buffer, 0, bytesRead);
            accept(data);//接受管道传输过来的数据
            // Process the data as needed
        }

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
        else if(words[0].equals("mon")){
            deal_mon(word);
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


        else{
            shellInput1.write("命令不存在".getBytes());
            shellInput1.flush();
        }
    }
    //打印list
    public static void printList(List<String> list) throws IOException {
        //打印list,全打印在同一行,用空格隔开
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                shellInput1.write(list.get(i).getBytes());
                shellInput1.write("\n".getBytes());
            }
            shellInput1.write("\r\n".getBytes());
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
            shellInput1.write("re:参数过多".getBytes());
            shellInput1.flush();
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
                shellInput1.write("ls:第二,三个参数格式错误".getBytes());
                shellInput1.flush();
            }

        }
        else{
            shellInput1.write("ls:参数过多".getBytes());
            shellInput1.flush();
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
                shellInput1.write(list.getBytes());
                shellInput1.flush();
            }



        }
        else{
            shellInput1.write("cd:参数过多".getBytes());
            shellInput1.flush();
        }


    }
    public static void deal_mkdir(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==1){
            shellInput1.write("mkdir:缺少指定路径".getBytes());
            shellInput1.flush();
        }
        else if(words.length==2){
            list=fileManager.mkdir(words[1]);
            if(list!=null ){
                shellInput1.write(list.getBytes());
                shellInput1.flush();
            }

        }
        else{
            shellInput1.write("mkdir:参数过多".getBytes());
            shellInput1.flush();
        }

    }
    public static void deal_mon(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            //调用mon模块
        }
        else if(words.length==2){
            if(words[1].equals("-o")){

            }
            else{
                shellInput1.write("mon:参数错误".getBytes());
                shellInput1.flush();
            }
        }
        else {
                    shellInput1.write("mon:参数过多".getBytes());
                    shellInput1.flush();
            }
        }





    public static void deal_rm(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==1){
            shellInput1.write("rm:缺少路径".getBytes());
            shellInput1.flush();
        }
        //特殊情况:rm+path
        else if(words.length==2){
            list=fileManager.rm(words[1],"-r");
            if(list!=null ){
                shellInput1.write(list.getBytes());
                shellInput1.flush();
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
                shellInput1.write("ls:第二,三个参数格式错误".getBytes());
                shellInput1.flush();
                return;
            }
            if(list!=null ){
                shellInput1.write(list.getBytes());
                shellInput1.flush();
            }

        }
        else{
            shellInput1.write("ls:参数过多".getBytes());
            shellInput1.flush();
        }

    }
    public static void deal_dss(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
        }
        else {
            //参数过多
            shellInput1.write("dss:参数过多".getBytes());
            shellInput1.flush();
        }

    }
    public static void deal_exec(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            shellInput1.write("exec:缺少指定路径".getBytes());
            shellInput1.flush();
        }
        else if(words.length==2){
            //先给filemanager模块传入路径,看是否存在该文件,再接收该文件的内容
            if(fileManager.find(words[1]).equals("error")){
                shellInput1.write("执行文件不存在".getBytes());
                shellInput1.flush();
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
                    shellInput1.write("该文件为空文件".getBytes());
                    shellInput1.flush();
                    return;
                }
                //执行
                processManager.creat_process(map.get("name"),map.get("priority"),map.get("size"),list);
            }
            else {
                shellInput1.write("该文件不属于可执行文件".getBytes());
                shellInput1.flush();
            }
        }
        else{
            shellInput1.write("exec:参数过多".getBytes());
            shellInput1.flush();
        }
    }
    public static void deal_dms(String word) throws IOException {
        String[] words = word.split(" ");
        List<String> list = null;
        if(words.length==1){
            list=memoryManager.check();
            //打印内存信息
            if (list.size()!=0) {
                printList(list);
            }

        }
        else {
            //参数过多
            shellInput1.write("dms:参数过多".getBytes());
            shellInput1.flush();
        }
    }

    public static void deal_td(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
        }
        else {
            //参数过多
            shellInput1.write("td:参数过多".getBytes());
            shellInput1.flush();
        }
    }
    public static void deal_mkf(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==3){
            //调用mkf模块
            list=fileManager.mkf(words[1],"crwx",words[3]);
            if(list!=null ){
                shellInput1.write(list.getBytes());
                shellInput1.flush();
            }
        }
        else {
            shellInput1.write("mkf:参数不为3".getBytes());
            shellInput1.flush();
        }
    }
    public static void deal_kill(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==2){
        }
        else {
            shellInput1.write("kill:参数不为2".getBytes());
            shellInput1.flush();
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
                shellInput1.write("没有进程".getBytes());
                shellInput1.flush();
                return;
            }
            //遍历pcbList,打印状态,name,pid
            for (int i = 0; i < pcbList.size(); i++) {
                shellInput1.write(("进程名字:" + pcbList.get(i).name + " 进程状态:" + pcbList.get(i).status + " 进程pid:" + pcbList.get(i).pid+"\n").getBytes());
                shellInput1.flush();
            }
        }
        else {
            //参数过多
            shellInput1.write("ps:参数过多".getBytes());
            shellInput1.flush();
        }
    }
    public static void deal_rs(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
        }
        else {
            //参数过多
            shellInput1.write("rs:参数过多".getBytes());
            shellInput1.flush();
        }
    }
    public static void deal_man(String word) throws IOException {
        String[] words = word.split(" ");
        if(words.length==1){
            //调用mon模块
        }
        else {
            //检查words每一个元素是否属于字符数组keyword
            for(int i=1;i<words.length;i++){
                boolean flag = false;
                for(int j=0;j<keyword.length;j++){
                    if(words[i].equals(keyword[j])){
                        flag = true;
                        break;
                    }
                }
                if(!flag){
                    shellInput1.write("man:存在不正确的指令".getBytes());
                    shellInput1.flush();
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
                shellInput1.write(list.getBytes());
                shellInput1.flush();
            }
        }
        else {
            //参数过多
            shellInput1.write(":参数错误".getBytes());
            shellInput1.flush();
        }
    }
    public static void deal_gcc(String word) throws IOException {
        String[] words = word.split(" ");
        String list = null;
        if(words.length==2){
            //检测是否存在该文件
            list=fileManager.find(words[1]);
            if(list.equals("error")){
                shellInput1.write("gcc:文件不存在".getBytes());
                shellInput1.flush();
                return;
            }

            //调用gcc模块,成功or不符合格式
            if(gcc.compile(words[1],list)){
                shellInput1.write("gcc:编译成功".getBytes());
                shellInput1.flush();
            }
            else{
                shellInput1.write("gcc:格式错误,编译失败".getBytes());
                shellInput1.flush();
            }
        }
        if(words.length==3){
            //检测是否存在该文件
            list=fileManager.find(words[2]);
            if(list.equals("error")){
                shellInput1.write("gcc:文件不存在".getBytes());
                shellInput1.flush();
                return;
            }
            //检查words[1]是否为数字
            try {
                int i = Integer.parseInt(words[1]);
            }catch (Exception e){
                shellInput1.write("gcc:参数错误".getBytes());
                shellInput1.flush();
                return;
            }
            //调用gcc模块,成功or不符合格式
            if(gcc.compile(words[1],list)){
                shellInput1.write("gcc:编译成功".getBytes());
                shellInput1.flush();
            }
            else{
                shellInput1.write("gcc:编译失败".getBytes());
                shellInput1.flush();
            }
        }
        else {
            //参数过多
            shellInput1.write(":参数错误".getBytes());
            shellInput1.flush();
        }
    }
}