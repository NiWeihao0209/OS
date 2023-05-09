package SystemCore;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

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
    private HardwareResource printer;
    private List<String> devices; //含有的设备
    private Map<String, List<ProcessControlBlock>> resourcesHistory;
    private double historyLength;
    private boolean running;        //时候有进程在执行
    private Map<Integer, Integer> pidToAid;//虚拟地址对应实际地址
    Memory memory;
    DiskScheduler disk=new DiskScheduler();

    public List<ProcessControlBlock> getProcess() {
        //遍历pcbList,打印状态,name,pid
        for (int i = 0; i < pcbList.size(); i++) {
            System.out.println("进程名字:" + pcbList.get(i).name + " 进程状态:" + pcbList.get(i).status + " 进程pid:" + pcbList.get(i).pid);
        }
        return pcbList;
    }



    //memoryManager是内存管理器对象，用于管理进程的内存分配和释放。
    public ProcessManager(Memory memory) {
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

        //表示打印机资源
        //********this.printer = new SystemCore.HardwareResource(printerNum);******//
        //设备列表，初始先默认加入cpu，打印机两个设备
        this.devices = new ArrayList<>();
        this.devices.add("cpu");
        this.devices.add("printer");
        //表示每个设备的历史记录
        this.resourcesHistory = new HashMap<>();
        this.resourcesHistory.put("cpu", new ArrayList<>());
        this.resourcesHistory.put("printer", new ArrayList<>());
        this.historyLength = 14.0;
        //初始没有正在进行运行的进程置为false
        this.running = false;
        this.pidToAid = new HashMap<>();
        this.memory = memory;
    }

    public void fork(List<String> list){
        //首先复制目前当前正在执行的父进程的信息
        ProcessControlBlock runningProcess = pcbList.get(currentRunning);

        //创建一个新的进程
        int newPid = curPid;
        curPid = curPid+1;
        ProcessControlBlock newProcess = new ProcessControlBlock(newPid,runningProcess.pid,runningProcess.name,runningProcess.priority,runningProcess.size,list);

        //修改当前父进程程序计数器，使它读到下一个地址
        pcbList.get(currentRunning).pc += 1;
        //复制父进程的size
        int newSize = runningProcess.size;
        synchronized(memory){
            //申请内存
            //******这里需要调用内存方面的函数
            memory.createProcess(newProcess.pid,newProcess.name,newSize);
            System.out.println("pid为"+newProcess.pid+"的进程申请内存成功");
        }



        //如果申请成功
        //复制父进程的信息
        //newProcess.parent_pid = runningProcess.pid;
        //newProcess.commend_queue = runningProcess.commend_queue;
        //newProcess.priority = runningProcess.priority;
        //设置好子进程的pc
        newProcess.pc = runningProcess.pc;

        //设置好子进程的状态为就绪
        newProcess.status = "ready";


        //获取时间newProcess.create_time =

        //加入pcblist
        pcbList.add(newProcess);

        //加入到就绪队列
        readyQueue.get(newProcess.priority).add(newProcess);

    }

    //系统自己的程序创建进程，需要调用到文件模块
    public void creat_process(String name,String priority,String size,List<String> list){

        //list-name,priority,size,content
        //priority是字符串，需要转换成int
        int priorityInt = Integer.parseInt(priority);
        //size是字符串，需要转换成int
        int sizeInt = Integer.parseInt(size);
        ProcessControlBlock newProcess = new ProcessControlBlock(curPid,0,name,priorityInt,sizeInt,list);
        newProcess.parent_pid = 0;

        //把数据存给memory
        synchronized(memory) {
            //获取时间
            //*******newProcess.create_time =

            //********从list里读取到name，priority，centent，size

            //把内存分的实际地址与当前进程建立联系，即填好当前processManager的pidToAid

            //修改当前进程的curPid
            curPid ++;

            //把创建好的进程加入pcbList
            pcbList.add(newProcess);

            //把创建好的进程加入到就绪队列
            readyQueue.get(newProcess.priority).add(newProcess);
            // 这里放置需要互斥访问的代码
            boolean createResult =  memory.createProcess(newProcess.pid,name,sizeInt);
            //TODO:分情况讨论，如果创建成功，就把进程加入到就绪队列，如果创建失败，就把进程加入到等待队列
            System.out.println("---pid为"+newProcess.pid+"的进程申请内存成功");
        }
    }


    //进程调度算法
    public void schduler(){
        int flag = 0;
        //按照优先级对readyQueue进行遍历，如果有进程在排队，就取出第一个
        for (int priority = 2; priority >= 0; priority--) {
            for (int i = 0 ;i < readyQueue.get(priority).size() && flag == 0; i++) {
                ProcessControlBlock pcb = readyQueue.get(priority).get(i);
                flag = 1;
                // 设置其为当前正在运行的进程，将其状态设置为“running”
                currentRunning = getIndex(pcbList,pcb.pid);
                pcbList.get(currentRunning).status="running";
                pcbList.set(currentRunning,pcbList.get(currentRunning));
                //在readyQueue中清除该进程
                readyQueue.get(pcb.priority).remove(i);
            }
        }
        //如果找不到，把currentRunning置为-1
        if(flag == 0){
            currentRunning = -1;
        }
    }

    //时间片管理
    public void time_out(){
        //首先判断当前是否有进程正在运行
        if (currentRunning != -1){
            //修改当前正在执行的进程status为“ready”
            pcbList.get(currentRunning).status="ready";
            pcbList.set(currentRunning,pcbList.get(currentRunning));
            int priority = pcbList.get(currentRunning).priority;
            //判断是否已经执行完所有命令,没执行完就加入对应优先级的就绪队列
            if (pcbList.get(currentRunning).commend_queue.size() >= pcbList.get(currentRunning).pc){
                readyQueue.get(priority).add(pcbList.get(currentRunning));
            }
        }
        schduler();
    }


    //中断管理
    public void io_interrput(){
        //修改当前父进程程序计数器，使它读到下一个地址
        pcbList.get(currentRunning).pc += 1;
        //把当前正在执行的进程状态改为“waiting”
        pcbList.get(currentRunning).status="waiting";
        pcbList.set(currentRunning,pcbList.get(currentRunning));
        waitingQueue.add(pcbList.get(currentRunning));
        //计算预期等待时间
        //检查打印设备是否空闲
        //设置好设备工作
        schduler();
    }

    //释放资源
    public void release(int pid){
        //程序计数器加一
        pcbList.get(getIndex(pcbList,pid)).pc ++;
        waitingQueue.remove(getIndex(waitingQueue,pid));
        //在打印机的运行队列中遍历找到后删除相关信息
        //************************

        //对于等待队列中的每个进程，如果该进程的状态不是“等待打印机”，并且打印机有空闲资源，则该进程会被插入打印机的运行队列中。
        for(int waitpid = 0; waitpid < waitingQueue.size(); waitpid++){
            if(waitingQueue.get(waitpid).status != "wating(Printer)"){
                //根据下一条指令计算出预计需要使用设备的时间
                //调用设备提供的方法插入到设备中
                //*************
                //修改改pcb的status
                waitingQueue.get(waitpid).status = "waiting(Printer)";
            }
        }
    }

    //杀死进程
    public void killProcess(int pid) {
        //遍历是否在pcbList里面
        int index=getIndex(pcbList,pid);
        if(index!=-1){
            String status=pcbList.get(index).status;
            // Set status to terminated
            pcbList.get(index).status="terminated";
            pcbList.set(index,pcbList.get(index));
            // 根据之前的状态释放对应的资源
            if(status == "ready"){
                readyQueue.get(pcbList.get(index).priority).remove(getIndex(readyQueue.get(pcbList.get(index).priority),pid));
            } else if (status == "running") {
                currentRunning = -1;
            } else if (status == "waiting") {
                waitingQueue.remove(getIndex(waitingQueue,pid));
            } else if (status =="waiting(Printer)") {
                release(pid);
            }
        }
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
                System.out.println("No process is running currently");
            }
        }
    }


    //记录设备使用历史
    public void append_resources_hestory(String type ,int pid){
        //得到现在系统的时间 unix_time
        resourcesHistory.get(type).add(pcbList.get(pid));
        //只要空间够就一直记录到对应的设备使用历史

    }


    //启动进程管理器
    @Override
    public void run(){
        running = true;
        int nowpc = -1;
        while(running){
            //时间片轮转
            time_out();

            String commend = "";
            String[] strs= new String[2];

            if(currentRunning != -1){
                nowpc = pcbList.get(currentRunning).pc;
                if(nowpc < pcbList.get(currentRunning).commend_queue.size()){
                    //把“access 1”类型的字符串按空格分割转为字符串数组
                    strs = pcbList.get(currentRunning).commend_queue.get(nowpc).split(" ", 2);
                    commend = strs[0];
                }
            } else {
                nowpc = -1;
            }



            //只要有程序在运行就检查是否需要中断 printer语句
            if (currentRunning != -1 && pcbList.get(currentRunning).commend_queue.get(nowpc).equals("printer")){
                io_interrput();
            }

            //fork语句
            if (currentRunning != -1 && pcbList.get(currentRunning).commend_queue.get(nowpc).equals("fork")){
                fork(pcbList.get(currentRunning).commend_queue);
                try {
                    Thread.sleep(timeSlot);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                //appendResourcesHistory


            }


            //access语句
            if(currentRunning != -1 && commend.equals("access")){
                //调用 self.memory_manager.access() 方法访问内存，并且如果操作失败，则会将该进程从命令队列中删除。
                int locate= Integer.parseInt(strs[1]);
                int accessResult = memory.access(pcbList.get(currentRunning).pid,locate);
                //TODO：分情况讨论
                System.out.print("pid"+pcbList.get(currentRunning).pid+"访问内存"+strs[1]+"成功"+"\n");

                //修改当前父进程程序计数器，使它读到下一个地址
                pcbList.get(currentRunning).pc += 1;
            }
            //block语句
            if(currentRunning != -1 && commend.equals("block")){
                //把str数组第二个数到最后一个值复制到int型disk_access数组中
                int[] disk_access = new int[strs.length-1];
                for(int i = 1; i < strs.length; i++){
                    disk_access[i-1] = Integer.parseInt(strs[i]);
                }

                disk.Disk_find(disk_access);



                //修改当前父进程程序计数器，使它读到下一个地址
                pcbList.get(currentRunning).pc += 1;
            }

            //????时间片调度算法
            try {
                Thread.sleep(timeSlot);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //判断所有命令是否执行完毕
            if (currentRunning != -1){
                //如果执行完毕，释放资源，修改状态
                if(pcbList.get(currentRunning).commend_queue.size() - pcbList.get(currentRunning).pc <= 0){
                    System.out.print(String.format("\033[2K\033[%dG", 0));
                    // 需要内存的方法
                    synchronized(memory){
                        memory.release(pcbList.get(currentRunning).pid);
                        System.out.println(String.format("[pid #%d] finish!", currentRunning));
                    }

                    pcbList.get(currentRunning).status="terminated";
                    pcbList.set(currentRunning,pcbList.get(currentRunning));
                    currentRunning = -1;
                }
                //没有执行完，将当前命令的剩余时间减1，记录使用了CPU资源，如果当前命令执行完毕，则将PC指针加1，转到下一个命令执行。
                else if (pcbList.get(currentRunning).commend_queue.size() - pcbList.get(currentRunning).pc > 1) {
                    //将当前命令的剩余时间减1
                    //需要pcb存入时修改变量类型为int
                    //记录使用了CPU资源
                    // appendResourcesHistory("cpu", pcbList.get(currentRunning).getPid());

                    /*if (pcbList.get(currentRunning).commend_queue.get(pcbList.get(currentRunning).pc).get(1) == 0) {
                        pcbList.get(currentRunning).pc +=1;
                    }*/
                }


            }



            ///对打印机队列的处理，需要调用到printer函数


        }


    }

    private int getIndex(ObservableList<ProcessControlBlock> pcbList,int pid){
        for (int i=0;i<pcbList.size();i++){
            if(pcbList.get(i).pid==pid)
                return i;
        }
        return -1;
    }

    private int getIndex(List<ProcessControlBlock> queue,int pid){
        for (int i=0;i<queue.size();i++){
            if(queue.get(i).pid==pid)
                return i;
        }
        return -1;
    }

    //主函数，测试用



}


