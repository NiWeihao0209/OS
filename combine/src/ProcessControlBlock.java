import java.util.ArrayList;
// import java.util.Arrays;
import java.util.List;
import java.util.Date;
// import java.util.Scanner;
// import java.io.FileReader;

public class ProcessControlBlock {
    public int pid; //进程标识号
    public int parent_pid;//父进程id，若没有父进程的话则设为-1
    public ArrayList<Integer> child_pid = new ArrayList<>();//子进程id
    public Date create_time = new Date();//创建时间
    public String name;//进程名字
    public String status;//状态，共5种，为new, ready, running, waiting, terminated
    public int priority;//优先级，数值越大，优先级越高
    public int size;//进程大小，需要为进程分配内存
    public int pc;//程序计数器
    public ArrayList<String> commend_queue = new ArrayList<>();//执行队列(commend_queue)：记录该进程仍需要执行的批处理指令
    public int commend_now;//此时运行到的指令号，初始为0

    public ProcessControlBlock(int pid, int parent_id, String name, int priority, int size, List<String> commands){
        this.pid = pid;
        this.parent_pid = parent_id;
        this.name = name;
        this.priority = priority;
        this.size = size;
        this.status = "ready";
        this.pc = 0;//当前正在执行的指令号先设为0

        //分配内存为size——与内存管理相连接

        //存储进程的指令
        for (int i = 0; i < commands.size(); i++)
        {
            // String[] strs = commands.get(i).split(" ");
            // for(int j = 0; j < strs.length; j++)
            // {
            //     commend_quene.add(i, Arrays.asList(strs[j]));
            // }
            commend_queue.add(commands.get(i));
        }

        // //存储进程的指令
        // try (Scanner sc = new Scanner(new FileReader(path)))
        // {
        //     sc.useDelimiter("\n");  //分隔符
        //     while (sc.hasNext()) {   //按分隔符读取字符串
        //         String commend = sc.next();
        //         commend_queue.get(commend_now).add(commend);
        //     }
        // }
        // catch (Exception e)
        // {
        //     e.printStackTrace();
        // }
    }

    //该进程已完成，变为terminated状态
    public void terminated()
    {
        this.status = "terminated";
        //进程的子进程也需要结束
        //内存回收
    }

    //更改pid
    public void set_pid(int pid)
    {
        this.pid = pid;
    }

    //更改name
    public void set_name(String name)
    {
        this.name = name;
    }

    //更改优先级
    public void set_priority(int priority)
    {
        this.priority = priority;
    }

    //更改size
    public void set_size(int size)
    {
        //内存管理——更改size
        this.size = size;
    }


    /*
    //进程变waiting态
    public void process_to_waiting()
    {
        this.status = "waiting";
    }

    //进程变ready态
    public void process_to_ready()
    {
        this.status = "ready";
    }

    //进程变running态
    public void process_running()
    {
        this.status = "running";

        for(int i = commend_now; i < commend_queue.size() && this.status == "running"; i++)//确保在running态开始以下操作
        {
            String now_thread = commend_queue.get(i);//获取现在正在执行的线程
            String[] strs = now_thread.split(" ", 2);
            int thread_time = Integer.parseInt(strs[0]);//进程所需时间,单位为毫秒
            String thread_name = strs[1];//进程名字
            System.out.println("pid: "+this.pid+"'s thread"+thread_name+"is running! It needs "+thread_time+"ms.");
            commend_now = i;
        }

    }

    //模拟线程运行时间
    static public void process_sleep(int thread_time)
    {
        try {
            Thread.sleep(thread_time); // 延迟ms
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    */

    //主函数，测试用
    // public static void main (String [] args)
    // {
    //     ProcessControlBlock tmp = new ProcessControlBlock(0, 0, "name", 0, 0, "C:\\Users\\GeBeiyu\\Desktop\\test.txt");
    //     System.out.println(tmp.name);
    //     System.out.println(tmp.pid);
    //     for(int i = 0 ; i<tmp.commend_queue.size();i++)
    //     {
    //         System.out.println(tmp.commend_queue.get(i));
    //     }
    // }
}
