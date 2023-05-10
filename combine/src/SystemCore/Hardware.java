package SystemCore;//设备资源类
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Date;

public class Hardware {
    protected int hardware_pid;//记录硬件对应的进程号，如果目前硬件未被使用，设为-1
    protected Boolean hardware_used;//记录目前硬件是否被使用，被使用为TRUE，未被使用为FALSE
    protected String hardware_cate;//设备种类
    protected Date time_begin;//所有硬件安装的时间
    protected Date time_correct;//上次硬件的空闲/使用的时间被修改的时间
    protected long time_free;//硬件所有空闲的时间，单位ms，当被调用或者修改的时候进行更新
    protected long time_used;//硬件所有使用的时间，单位ms，当被调用或者修改的时候进行更新
    protected Date this_time_begin;//本次硬件开始使用的时间
    protected int this_time_used_plan;//本次硬件计划被使用的时间，单位ms
    protected long this_time_used_now;//本次硬件已经被使用的时间，单位ms
    private ImageView icon;//设备图标

    //默认构造函数
    public Hardware()
    {
        hardware_pid = -1;
        hardware_used = false;
        hardware_cate = "printer";//默认是打印机
        icon=new ImageView(new Image("file:res/icons/printer.png"));
        Date t_now = new Date();
        time_begin = t_now;
        time_correct = t_now;
        time_free = 0;
        time_used = 0;
    }

    //构造函数
    public Hardware(String cate)
    {
        hardware_used = false;
        hardware_cate = cate;
        icon=new ImageView(new Image("file:res/icons/"+cate+".png"));
        Date t_now = new Date();
        time_begin = t_now;
        time_correct = t_now;
        time_free = 0;
        time_used = 0;
    }

    public int get_hardware_pid()
    {
        return hardware_pid;
    }

    public Boolean get_hardware_used()
    {
        return hardware_used;
    }

    public String get_hardware_cate()
    {
        return hardware_cate;
    }

    public Date get_time_begin()
    {
        return time_begin;
    }

    public Date get_time_correct()
    {
        return time_correct;
    }

    public Date get_this_time_begin()
    {
        return this_time_begin;
    }

    public int get_this_time_used_plan()
    {
        return this_time_used_plan;
    }

    public String getStartTime(){
        if(hardware_used)
            return this_time_begin.toString();
        return "--";
    }

    public String getPlanTime(){
        if(hardware_used)
            return String.valueOf(this_time_used_plan);
        return "--";
    }

    public String getPid(){
        if(hardware_used)
            return String.valueOf(hardware_pid);
        return "--";
    }

    public ImageView getIcon(){
        return icon;
    }

    public String getStatus(){
        if(hardware_used)
            return "占用";
        return "空闲";
    }

    public int get_this_time_used_now()
    {
        Date t_now = new Date();
        long time_diff = t_now.getTime() - time_correct.getTime();
        if(hardware_used == true)
        {
            time_used += time_diff;
            this_time_used_now +=time_diff;
        }
        else
            time_free += time_diff;
        time_correct = t_now;
        return (int)this_time_used_now;
    }

    public long get_time_free()
    {
        Date t_now = new Date();
        long time_diff = t_now.getTime() - time_correct.getTime();
        if(hardware_used == true)
        {
            time_used += time_diff;
            this_time_used_now +=time_diff;
        }
        else
            time_free += time_diff;
        time_correct = t_now;
        return time_free;
    }

    public long get_time_used()
    {
        Date t_now = new Date();
        long time_diff = t_now.getTime() - time_correct.getTime();
        if(hardware_used == true)
        {
            time_used += time_diff;
            this_time_used_now +=time_diff;
        }
        else
            time_free += time_diff;
        time_correct = t_now;
        return time_used;
    }

    public void set_hardware_pid(int pid)
    {
        hardware_pid = pid;
    }

    public void set_hardware_used(Boolean used)
    {
        hardware_used = used;
    }

    public void set_time_correct(Date correct)
    {
        time_correct = correct;
    }

    public void set_time_free(Date t_now)
    {
        long time_diff = t_now.getTime() - time_correct.getTime();
        time_used += time_diff;
        this_time_used_now += time_diff;
        time_correct = t_now;
    }

    public void set_time_used(Date t_now)
    {
        long time_diff = t_now.getTime() - time_correct.getTime();
        time_free += time_diff;
        time_correct = t_now;
    }

    //设备从空闲到正在使用
    public void free_to_use(Date t_now, int pid, int time_plan)
    {
        this_time_used_plan = time_plan;
        this_time_begin = t_now;
        this_time_used_now = 0;

        hardware_pid = pid;
        hardware_used = true;
        long time_diff = t_now.getTime() - time_correct.getTime();
        time_free += time_diff;
        time_correct = t_now;

        // System.out.println("free_to_use,pid:"+handware_pid);
        // System.out.println("free_to_use,this_time_used_plan:"+this_time_used_plan);
        // System.out.println("free_to_use,used:"+handware_used);
    }

    //设备从正在使用到空闲
    public void used_to_free(Date t_now)
    {
        this_time_used_now = 0;
        this_time_used_plan = 0;

        hardware_pid = -1;
        hardware_used = false;
        long time_diff = t_now.getTime() - time_correct.getTime();
        time_used += time_diff;
        time_correct = t_now;
    }

    //判断该设备是否已经运行完成，若运行完成，返回1，该设备停止；否则返回-1，表示还未运行完成
    public int judge_finished()
    {
        int plan_time = get_this_time_used_plan();
        int used_time = get_this_time_used_now();
        // System.out.println("judge_finished,plan_time:"+plan_time);
        // System.out.println("judge_finished,used_time:"+used_time);
        if(plan_time > used_time)//预计时间大于已经运行的时间，则还没有完成
            return -1;

        return 1;
    }
}
