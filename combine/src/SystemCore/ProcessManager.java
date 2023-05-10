package SystemCore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.util.*;

public class ProcessManager implements Runnable {
    private int curPid;             //当前分配的进程的pid
    private boolean priority;       //是否采用优先级调度算法
    private boolean preemptive;     //是否允许抢占
    private int timeSlot;           //时间片
    private List<List<ProcessControlBlock>> readyQueue;     //就绪队列
    private List<ProcessControlBlock> waitingQueue;         //等待队列
    private int currentRunning;
    public static ObservableList<ProcessControlBlock> pcbList;
    private HardwareManager hardwareManager;
    private List<String> devices; //含有的设备
    private Map<String, List<List<Object>>> resourcesHistory;
    private int historyLength;
    private boolean running;        //时候有进程在执行
    private Map<Integer, Integer> pidToAid;//虚拟地址对应实际地址
    Memory memory;
    DiskScheduler disk=new DiskScheduler();
    public int sock; //用于同步kill和run的锁

    public List<ProcessControlBlock> getProcess() {
        //遍历pcbList,打印状态,name,pid
        for (int i = 0; i < pcbList.size(); i++) {
            Diary.println("process_name:" + pcbList.get(i).name + " process_statue:" + pcbList.get(i).status + " pid:" + pcbList.get(i).pid);
        }
        return pcbList;
    }

    //memoryManager是内存管理器对象，用于管理进程的内存分配和释放。
    public ProcessManager(Memory memory, HardwareManager hardwareManager) {
        this.curPid = 0;
        this.priority = true;
        this.preemptive = false;
        this.timeSlot = 1000;
        //一个包含三个空列表的列表，表示三个不同优先级的就绪队列。
        this.readyQueue = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            this.readyQueue.add(new ArrayList<>());
        }
        this.waitingQueue = new ArrayList<>();
        //现在正在运行的内存的pid，初始没有置为-1
        this.currentRunning = -1;
        this.pcbList = FXCollections.observableArrayList();
        //表示设备管理资源
        this.hardwareManager = hardwareManager;
        //设备列表，初始先默认加入cpu，打印机两个设备
        this.devices = new ArrayList<>();
        this.devices.add("cpu");
        this.devices.add("printer");
        //表示每个设备的历史记录
        this.resourcesHistory = new HashMap<>();
        this.resourcesHistory.put("cpu", new ArrayList<>());
        this.resourcesHistory.put("printer", new ArrayList<>());
        //时间戳
        this.historyLength = 14;
        //初始没有正在进行运行的进程置为false
        this.running = false;
        this.pidToAid = new HashMap<>();
        this.memory = memory;
        this.sock=0;
    }

    public void fork(List<String> list){
        //首先复制目前当前正在执行的父进程的信息
        ProcessControlBlock runningProcess = pcbList.get(getIndex(pcbList,currentRunning));

        //创建一个新的进程
        int newPid = curPid;
        curPid = curPid+1;
        //复制父进程的信息
        ProcessControlBlock newProcess = new ProcessControlBlock(newPid,runningProcess.pid,runningProcess.name,runningProcess.priority,runningProcess.size,list);
        //设置好子进程的pc
        newProcess.pc = runningProcess.pc+1;
        //设置好子进程的状态为就绪
        newProcess.status = "ready";
        //获取时间
        newProcess.create_time = new Date();

        //修改当前父进程程序计数器，使它读到下一个地址
        pcbList.get(getIndex(pcbList,currentRunning)).pc += 1;

        //复制父进程的size
        int newSize = runningProcess.size;
        //判断内存是否足够创建
        synchronized(memory){
            //申请内存
            boolean createResult =  memory.createProcess(newProcess.pid,newProcess.name,newSize);
            if(createResult){
                Diary.println("Process: "+newProcess.pid+" has successfully allocated memory.");

                //把创建好的进程加入pcbList
                pcbList.add(newProcess);
                Diary.println("Process: "+newProcess.pid+" has been successfully created.");

                //把创建好的进程加入到就绪队列
                synchronized (readyQueue){
                    readyQueue.get(newProcess.priority).add(newProcess);
                    Diary.println("Process: "+newProcess.pid+" successfully added to the readyQueue.");
                }

            }
            else {
                Diary.println("Process: "+newProcess.pid+" failed to create due to insufficient memory allocation!");
                //curPid --;
            }
        }
    }

    //系统自己的程序创建进程，需要调用到文件模块
    public void creat_process(String name,String priority,String size,List<String> list){

        //list-name,priority,size,content
        //priority是字符串，需要转换成int
        int priorityInt = Integer.parseInt(priority);
        //size是字符串，需要转换成int
        int sizeInt = Integer.parseInt(size);

        //创建一个进程
        ProcessControlBlock newProcess = new ProcessControlBlock(curPid,0,name,priorityInt,sizeInt,list);
        newProcess.parent_pid = 0;
        //获取时间
        newProcess.create_time = new Date();

        //修改当前进程的curPid
        curPid ++;

        //把数据传给memory，申请内存
        synchronized(memory) {
            // 这里放置需要互斥访问的代码
            boolean createResult =  memory.createProcess(newProcess.pid,name,sizeInt);
            //如果创建成功，就把进程加入到就绪队列
            if(createResult){
                Diary.println("Process: "+newProcess.pid+" has successfully allocated memory.");

                //把创建好的进程加入pcbList
                pcbList.add(newProcess);
                Diary.println("Process: "+newProcess.pid+" has been successfully created.");

                //把创建好的进程加入到就绪队列
                synchronized (readyQueue){
                    readyQueue.get(newProcess.priority).add(newProcess);
                    Diary.println("Process: "+newProcess.pid+" successfully added to the readyQueue.");
                }

            }
            else {
                Diary.println("Process: "+newProcess.pid+" failed to create due to insufficient memory allocation!");
                //curPid --;
            }
        }
    }


    //进程调度算法
    public void schduler(){
        synchronized (readyQueue){
            int flag = 0;
            //按照优先级对readyQueue进行遍历，如果有进程在排队，就取出第一个
            for (int priority = 2; priority >= 0; priority--) {
                for (int i = 0 ;i < readyQueue.get(priority).size() && flag == 0; i++) {
                    ProcessControlBlock pcb = readyQueue.get(priority).get(i);
                    flag = 1;

                    // 设置其为当前正在运行的进程，将其状态设置为“running”
                    currentRunning = pcb.pid;
                    pcbList.get(getIndex(pcbList,currentRunning)).status = "running";
                    pcbList.set(getIndex(pcbList,currentRunning),pcbList.get(getIndex(pcbList,currentRunning)));
                    Diary.println("---Process: "+currentRunning+" has transitioned to running.");

                    //在readyQueue中清除该进程
                    readyQueue.get(pcb.priority).remove(i);
                }
            }
            //如果找不到，把currentRunning置为-1
            if(flag == 0){
                currentRunning = -1;
            }
        }

    }

    //时间片轮转
    public void time_out(){
        //首先判断当前是否有进程正在运行
        if (currentRunning != -1){
            //修改当前正在执行的进程status为“ready”
            pcbList.get(getIndex(pcbList,currentRunning)).status = "ready";
            pcbList.set(getIndex(pcbList,currentRunning),pcbList.get(getIndex(pcbList,currentRunning)));
            Diary.println("---Process: "+currentRunning+" has transitioned to ready.");

            int priority = pcbList.get(getIndex(pcbList,currentRunning)).priority;
            //判断是否已经执行完所有命令,没执行完就加入对应优先级的就绪队列
            if (pcbList.get(getIndex(pcbList,currentRunning)).commend_queue.size() >= pcbList.get(getIndex(pcbList,currentRunning)).pc){
                synchronized (readyQueue){
                    readyQueue.get(priority).add(pcbList.get(getIndex(pcbList,currentRunning)));
                    Diary.println("Process: "+currentRunning+" successfully added to the readyQueue.");
                }

            }
        }
        schduler();
    }


    //中断管理
    public void io_interrput(String time){
        //修改当前父进程程序计数器，使它读到下一个地址
        //pcbList.get(getIndex(pcbList,currentRunning)).pc += 1;

        //把当前正在执行的进程状态改为“waiting”
        pcbList.get(getIndex(pcbList,currentRunning)).status = "waiting";
        pcbList.set(getIndex(pcbList,currentRunning),pcbList.get(getIndex(pcbList,currentRunning)));
        Diary.println("---Process: "+currentRunning+" has transitioned to waiting.");

        synchronized (waitingQueue){
            // 这里放置需要互斥访问的代码
            waitingQueue.add(pcbList.get(getIndex(pcbList,currentRunning)));
            Diary.println("Process: "+currentRunning+" successfully added to the waitingQueue.");
        }

        //计算预期等待时间
        int expectime = Integer.parseInt(time)*timeSlot;
        //记录使用设备
        int hardwareResult = hardwareManager.use_hardware("printer",pcbList.get(getIndex(pcbList,currentRunning)).pid,expectime);

        if(hardwareResult == -1){
            Diary.println("Process: "+currentRunning+" failed to request the use of device resources!");
        } else if (hardwareResult == 1) {
            Diary.println("Process: "+currentRunning+" has successfully requested the use of device resources.");
            //修改进程状态
            pcbList.get(getIndex(pcbList,currentRunning)).status = "waiting(printer)";
            pcbList.set(getIndex(pcbList,currentRunning),pcbList.get(getIndex(pcbList,currentRunning)));
            Diary.println("---Process: "+currentRunning+" has transitioned to waiting(printer).");
        }
        schduler();
    }

    //释放设备资源
    public void release(int pid){
        synchronized (waitingQueue){
            //程序计数器加一
            pcbList.get(getIndex(pcbList,pid)).pc ++;
            waitingQueue.remove(getIndex(waitingQueue,pid));
            Diary.println("Process: "+pid+" successfully remove from waitingQueue.");

            //对于等待队列中的每个进程，如果该进程的状态不是“等待(设备)”，并且设备有空闲资源，则该进程会被插入打印机的运行队列中。
            for(int waitpid = 0; waitpid < waitingQueue.size(); waitpid++){
                if(Objects.equals(waitingQueue.get(waitpid).status, "waiting")){
                    int wpid = waitingQueue.get(waitpid).pid;

                    //得到需要使用设备的时间
                    String commendnow = pcbList.get(getIndex(pcbList,wpid)).commend_queue.get(pcbList.get(getIndex(pcbList,wpid)).pc);
                    String[] strs= new String[2];
                    strs = commendnow.split(" ", 2);
                    int expectime = Integer.parseInt(strs[1])*timeSlot;

                    //调用设备提供的方法插入到设备中
                    int hardwareResult = hardwareManager.use_hardware(strs[0],pcbList.get(getIndex(pcbList,wpid)).pid,expectime);
                    if(hardwareResult == -1){
                        Diary.println("Process: "+pcbList.get(getIndex(pcbList,wpid)).pid+" failed to request the use of device resources!");
                    } else if (hardwareResult == 1) {
                        Diary.println("Process: "+pcbList.get(getIndex(pcbList,wpid)).pid+" has successfully requested the use of device resources.");
                        pcbList.get(getIndex(pcbList,wpid)).status = "waiting(printer)";
                        pcbList.set(getIndex(pcbList,wpid),pcbList.get(getIndex(pcbList,wpid)));
                        Diary.println("---Process: "+wpid+" has transitioned to waiting.");
                    }
                }
            }
        }

    }

    //杀死进程
    public void killProcess(int pid) {
        sock = 1;
        //遍历是否在pcbList里面
        for (int i = 0; i < pcbList.size(); i++) {
            int indexs=-1;
            for (int j=0;j<pcbList.size();j++){
                if(pcbList.get(j).pid==pid)
                    indexs=j;
            }
            ProcessControlBlock pcb = pcbList.get(indexs);
            if (pcb.pid == pid) {
                //判断进程是否状态为terminated
                if(pcbList.get(getIndex(pcbList,pid)).status.equals("terminated")){
                    Diary.println("Process: "+pid+" has already been terminated.");
                    sock = 0;
                    return;
                }

                // 根据之前的状态释放对应的资源
                if(pcb.status.equals("ready")){
                    //synchronized (readyQueue){
                        int flag = 0;
                        for (int priority = 2; priority >= 0; priority--) {
                            for (int index = 0; index < readyQueue.get(priority).size() && flag == 0; index++) {
                                if(readyQueue.get(priority).get(index).pid == pid){
                                    readyQueue.get(pcb.priority).remove(index);
                                    Diary.println("Process: "+pid+" successfully remove from readyQueue------ready");
                                    flag = 1;
                                }
                            }
                        }
                    //}

                } else if (pcb.status.equals("running")) {
                    currentRunning = -1;
                    //synchronized (readyQueue){
                        int flag = 0;
                        for (int priority = 2; priority >= 0; priority--) {
                            for (int index = 0; index < readyQueue.get(priority).size() && flag == 0; index++) {
                                if(readyQueue.get(priority).get(index).pid == pid){
                                    readyQueue.get(pcb.priority).remove(index);
                                    Diary.println("Process: "+pid+" successfully remove from readyQueue------running");
                                    flag = 1;
                                }
                            }
                        }
                    //}
                } else if (pcb.status.equals("waiting")) {
                    synchronized (waitingQueue){
                        waitingQueue.remove(getIndex(waitingQueue,pid));
                        Diary.println("process: "+pid+" successfully remove from waitingQueue.");
                    }

                } else if (pcb.status.equals("waiting(printer)")) {
                    //release(pid);
                }

                // Set status to terminated
                pcbList.get(getIndex(pcbList,pid)).status = "terminated";
                pcbList.set(getIndex(pcbList,pid),pcbList.get(getIndex(pcbList,pid)));
                Diary.println("---Process: "+pid+" has transitioned to terminated.------kill");

                //从内存提供的方法释放所占用的内存
                synchronized(memory){

                    memory.release(pcbList.get(getIndex(pcbList,pid)).pid);
                    Diary.println("Process:"+pid+" has been killed!");
                }
                sock = 0;
            }
        }
        //如果不在pcbList里面
        Diary.println("Process: "+pid+" is not in the pcbList.");
        sock = 0;
    }

    //显示进程状态
    public void process_status(){
        boolean is_running = false;
        for (int id = 0; id < pcbList.size(); id++){
            ProcessControlBlock pcb = pcbList.get(id);
            if(!Objects.equals(pcb.status, "terminated")){
                System.out.printf("[pid #%5d] name: %-10s status: %-20s create_time: %s\n",pcb.pid,pcb.name,pcb.status,pcb.create_time);
                is_running = true;
            }
            if(is_running == false){
                Diary.println("No process is running currently");
            }

        }
    }


    //记录设备使用历史
    public void append_resources_hestory(String type ,int pid){
        //得到现在系统的时间 unix_time
        long unixTime = Instant.now().getEpochSecond();
        //计入进程和对应时间
        resourcesHistory.get(type).add(List.of(unixTime, pid));
        //删除超过时间戳的信息
        while (!resourcesHistory.get(type).isEmpty() && unixTime - (long) resourcesHistory.get(type).get(0).get(0) > historyLength) {
            resourcesHistory.get(type).remove(0);
        }
    }

    //获得队列的index
    private int getIndex(List<ProcessControlBlock> queue,int pid){
        for (int i=0;i<queue.size();i++){
            if(queue.get(i).pid==pid)
                return i;
        }
        return -1;
    }

    //获得pcblist中的index
    private int getIndex(ObservableList<ProcessControlBlock> pcbList,int pid){
        for (int i=0;i<pcbList.size();i++){
            if(pcbList.get(i).pid==pid)
                return i;
        }
        return -1;
    }

    //启动进程管理器
    @Override
    public void run(){
        running = true;
        int nowpc = -1;
        while(running){
            //时间片轮转
            if(sock == 0){
                time_out();
            }
            //time_out();

            String commend = "";
            String[] strs= new String[100];//blcok<=100

            //把多字符串类型的字符串按空格分割转为字符串数组
            if(currentRunning != -1){
                nowpc = pcbList.get(getIndex(pcbList,currentRunning)).pc;
                //Diary.println("##########################nowpc= "+nowpc+"currentrun= "+currentRunning);
                if(nowpc < pcbList.get(getIndex(pcbList,currentRunning)).commend_queue.size()){
                    //把“access 1”类型的字符串按空格分割转为字符串数组
                    strs = pcbList.get(getIndex(pcbList,currentRunning)).commend_queue.get(nowpc).split(" ");
                    commend = strs[0];
                }
            } else {
                nowpc = -1;
            }

            if(strs[0] != null){
                //只要有程序在运行就检查是否需要中断 printer语句
                if (currentRunning != -1 && commend.equals("printer")){
                    io_interrput(strs[1]);
                }

                //Diary.println("****************************************");
                //Diary.println("pcblist have "+pcbList.size()+" processes");
                //Diary.println("****************************************");
                //Diary.println("##########################nowpc= "+nowpc+"currentrun= "+currentRunning);

                //fork语句
                else if (currentRunning != -1 && pcbList.get(getIndex(pcbList,currentRunning)).commend_queue.get(nowpc).equals("fork")){
                    fork(pcbList.get(getIndex(pcbList,currentRunning)).commend_queue);
                    try {
                        Thread.sleep(timeSlot);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //appendResourcesHistory
                    append_resources_hestory("cpu",pcbList.get(getIndex(pcbList,currentRunning)).pid);
                }


                //access语句
                else if(currentRunning != -1 && commend.equals("access")){
                    //调用 self.memory_manager.access() 方法访问内存，并且如果操作失败，则会将该进程从命令队列中删除。
                    int locate= Integer.parseInt(strs[1]);
                    int accessResult = memory.access(pcbList.get(getIndex(pcbList,currentRunning)).pid,locate);

                    //返回0表示进程不存在，返回1表示访问成功，返回2表示访问越界，返回3表示进程未分配内存，返回4表示其他错误,返回5表示缺页
                    if(accessResult == 1){
                        Diary.println("Process: "+pcbList.get(getIndex(pcbList,currentRunning)).pid+" access memory "+strs[1]+" successfully"+"\n");
                    } else if (accessResult == 2) {
                        Diary.println("Process: "+pcbList.get(getIndex(pcbList,currentRunning)).pid+" access memory "+strs[1]+" failed, crocess-border visit!"+"\n");
                    } else if (accessResult == 5) {
                        Diary.println("Process: "+pcbList.get(getIndex(pcbList,currentRunning)).pid+" access memory "+strs[1]+" failed, page fault"+"\n");
                    } else {
                        Diary.println("Process: "+pcbList.get(getIndex(pcbList,currentRunning)).pid+" access memory "+strs[1]+" failed, else error!"+"\n");
                    }

                    //修改当前父进程程序计数器，使它读到下一个地址
                    pcbList.get(getIndex(pcbList,currentRunning)).pc += 1;
                }

                //对于cpu指令，将当前命令的剩余时间减1，记录使用了CPU资源，如果当前命令执行完毕，则将PC指针加1，转到下一个命令执行。
                else if (currentRunning != -1 && strs[0].equals("cpu")) {
                    //将当前进程命令队列里的剩余时间减1
                    int beforeCpuTime = Integer.parseInt(strs[1]) - 1;
                    String nowCpuTime = Integer.toString(beforeCpuTime);
                    pcbList.get(getIndex(pcbList,currentRunning)).commend_queue.set(nowpc,"cpu "+nowCpuTime);

                    //记录使用了CPU资源
                    append_resources_hestory("cpu", pcbList.get(getIndex(pcbList,currentRunning)).pid);

                    //如果cpu指令后续时间为0，跳转到下一条指令
                    if (beforeCpuTime == 0) {
                        pcbList.get(getIndex(pcbList,currentRunning)).pc +=1;
                    }
                }
                //block语句
                else if(currentRunning != -1 && strs[0].equals("block")){
                    //把str数组第二个数到最后一个值复制到int型disk_access数组中
                    int[] disk_access = new int[strs.length-1];
                    for(int i = 1; i < strs.length; i++){
                        disk_access[i-1] = Integer.parseInt(strs[i]);
                    }

                    disk.Disk_find(disk_access);



                    //修改当前父进程程序计数器，使它读到下一个地址
                    pcbList.get(getIndex(pcbList,currentRunning)).pc += 1;
                }


                //模拟占用时间片
                try {
                    Thread.sleep(timeSlot);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            //判断所有命令是否执行完毕
            if (currentRunning != -1){
                //如果执行完毕，释放资源，修改状态
                if(pcbList.get(getIndex(pcbList,currentRunning)).commend_queue.size() - pcbList.get(getIndex(pcbList,currentRunning)).pc <= 0){

                    // 释放进程占用内存
                    synchronized(memory){
                        memory.release(pcbList.get(getIndex(pcbList,currentRunning)).pid);
                        Diary.println("Process: "+currentRunning+" finish!");
                    }

                    pcbList.get(getIndex(pcbList,currentRunning)).status = "terminated";
                    pcbList.set(getIndex(pcbList,currentRunning),pcbList.get(getIndex(pcbList,currentRunning)));
                    Diary.println("---Process: "+currentRunning+" has transitioned to terminated.");
                    currentRunning = -1;
                }

            }

            //对有进程在运行的设备资源的处理
            //更新设备状态
            hardwareManager.update_hardware_used();
            //遍历得到已经结束使用设备的进程
            for (int i = 0; i < hardwareManager.last_stop_hardware_pid.size(); i++){
                int pid_h = hardwareManager.last_stop_hardware_pid.get(i);
                //记录设备使用了资源
                append_resources_hestory("printer", pcbList.get(getIndex(pcbList,pid_h)).pid);
                //判断进程是否执行完所有命令
                //执行完所有指令
                if(pcbList.get(getIndex(pcbList,pid_h)).commend_queue.size() - pcbList.get(getIndex(pcbList,pid_h)).pc <= 0){

                    // 释放进程占用内存
                    synchronized(memory){
                        memory.release(pcbList.get(getIndex(pcbList,pid_h)).pid);
                        Diary.println("Process: "+pid_h+" finish!");
                    }

                    pcbList.get(getIndex(pcbList,pid_h)).status = "terminated";
                    pcbList.set(getIndex(pcbList,pid_h),pcbList.get(getIndex(pcbList,pid_h)));
                    Diary.println("---Process: "+pid_h+" has transitioned to terminated.");
                }
                //未执行完
                else {
                    //修改对应进程状态为ready
                    pcbList.get(getIndex(pcbList,pid_h)).status = "ready";
                    pcbList.set(getIndex(pcbList,pid_h),pcbList.get(getIndex(pcbList,pid_h)));
                    Diary.println("---Process: "+pid_h+" has transitioned to ready.");

                    //加入到就绪队列
                    synchronized (readyQueue){
                        readyQueue.get(pcbList.get(getIndex(pcbList,pid_h)).priority).add(pcbList.get(getIndex(pcbList,pid_h)));
                        Diary.println("Process: "+pid_h+" successfully added to the readyQueue.");
                    }

                }
                //释放并重新分配设备资源
                release(pid_h);
            }

            //清空表示此时没有需要从waiting态转为其他状态的进程
            hardwareManager.empty_last_stop_pid();
        }
    }
}


