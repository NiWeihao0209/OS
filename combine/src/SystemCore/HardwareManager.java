package SystemCore;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Date;

public class HardwareManager {
    protected int hardware_count;//硬件总个数
    protected int used_count;//被使用的硬件个数
    public static ObservableList<Hardware> hardwares;//所使用的硬件列表
    protected ArrayList<Integer> last_stop_hardware_pid;//当前结束设备使用的但还是waiting状态的进程的pid

    //默认构造函数
    public HardwareManager()
    {
        hardware_count = 2;//初始硬件个数默认为1
        used_count = 0;
        hardwares = FXCollections.observableArrayList();
        last_stop_hardware_pid = new ArrayList<>();
        Hardware hardware = new Hardware();
        Hardware camera=new Hardware("camera");
        hardwares.add(hardware);
        hardwares.add(camera);
    }

    //构造函数，指定第一个设备的种类
    public HardwareManager(String cate)
    {
        hardware_count = 1;//初始硬件个数默认为1
        used_count = 0;
        hardwares = FXCollections.observableArrayList();
        Hardware hardware = new Hardware(cate);
        last_stop_hardware_pid = new ArrayList<>();
        hardwares.add(hardware);
    }

    //更新设备的使用情况
    public void update_hardware_used()
    {
        for(int i = 0; i < hardware_count; i++)
        {
            //judge_finished返回1，该设备停止；否则返回-1，表示还未运行完成
            if(hardwares.get(i).get_hardware_used() == true && hardwares.get(i).judge_finished() == 1)
            {
                //把pid记下来
                int stop_pid = hardwares.get(i).get_hardware_pid();
                last_stop_hardware_pid.add(stop_pid);
                //把这个硬件变为空闲状态
                Date t_now = new Date();
                Hardware tmp=hardwares.get(i);
                tmp.used_to_free(t_now);
                hardwares.set(i,tmp);
                used_count--;
            }
        }
    }

    public void empty_last_stop_pid()
    {
        last_stop_hardware_pid.clear();
    }

    //得到当前目前的硬件个数
    public int get_hardware_count()
    {
        update_hardware_used();
        return hardware_count;
    }

    //得到正在被使用的硬件个数
    public int get_used_count()
    {
        update_hardware_used();
        return used_count;
    }

    //得到正在空闲的硬件个数
    public int get_free_count()
    {
        update_hardware_used();
        return hardware_count - used_count;
    }

    //得到所有的现在未被使用的硬件设备的种类
    public ArrayList<String> get_free_cate()
    {
        update_hardware_used();//更新目前设备的使用情况
        ArrayList<String> free_cate = new ArrayList<>();
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == false && !free_cate.contains(hardwares.get(i).hardware_cate))
            {
                free_cate.add(hardwares.get(i).hardware_cate);
            }
        }
        return free_cate;
    }

    //得到所有的现在未被使用的硬件设备的设备编号
    public ArrayList<Integer> get_free_id()
    {
        update_hardware_used();//更新目前设备的使用情况
        ArrayList<Integer> free_id = new ArrayList<>();
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == false)
            {
                free_id.add(i);
            }
        }
        return free_id;
    }

    //得到所有的现在正在使用的硬件设备的种类
    public ArrayList<String> get_used_cate()
    {
        update_hardware_used();//更新目前设备的使用情况
        ArrayList<String> free_cate = new ArrayList<>();
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == true && !free_cate.contains(hardwares.get(i).hardware_cate))
            {
                free_cate.add(hardwares.get(i).hardware_cate);
            }
        }
        return free_cate;
    }

    //得到所有的现在正在被使用的硬件设备的设备编号
    public ArrayList<Integer> get_used_id()
    {
        update_hardware_used();//更新目前设备的使用情况
        ArrayList<Integer> free_id = new ArrayList<>();
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == true)
            {
                free_id.add(i);
            }
        }
        return free_id;
    }

    //得到所有的现在正在被使用的硬件设备的进程pid
    public ArrayList<Integer> get_used_pid()
    {
        update_hardware_used();//更新目前设备的使用情况
        ArrayList<Integer> free_id = new ArrayList<>();
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == true)
            {
                free_id.add(hardwares.get(i).get_hardware_pid());
            }
        }
        return free_id;
    }

    //添加某种类的硬件，并返回添加后的硬件个数
    public int add_hardware(String cate)
    {
        hardware_count++;
        Hardware hardware = new Hardware(cate);
        hardwares.add(hardware);
        return hardware_count;
    }

    //删除某硬件，若成功，返回删除后的硬件个数（从0开始）；
    //若失败（若该硬件还有任务，即对应handware_used的值为TRUE），返回-1
    //若失败（没有该硬件编号），返回-2
    //注意handware_id是从0开始的
    public int delete_hardware(int handware_id)
    {
        if(handware_id + 1 > hardware_count)
            return -2;
        if(hardwares.get(handware_id).get_hardware_used() == true)
            return -1;

        hardware_count--;
        hardwares.remove(handware_id);
        return hardware_count;
    }

    //给某个进程pid使用某个种类的硬件，并传入预计使用的时间(单位ms)，如果该硬件被其它进程占用，则使用失败，返回-1，否则使用成功返回1
    public int use_hardware(String cate, int pid, int time_plan)
    {
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == false && hardwares.get(i).hardware_cate .equals(cate))
            {
                Date t_now = new Date();
                Hardware tmp=hardwares.get(i);
                tmp.free_to_use(t_now, pid, time_plan);
                hardwares.set(i,tmp);
                used_count++;
                return 1;
            }
        }

        return -1;
    }

    //给某个进程pid使用某个硬件编号的硬件，并传入预计使用的时间(单位ms)，若失败（没有该硬件编号），返回-2;如果该硬件被其它进程占用，则使用失败，返回-1，否则使用成功返回1
    public int use_hardware(int handware_id, int pid, int time_plan)
    {
        if(handware_id + 1 > hardware_count)
            return -2;
        if(hardwares.get(handware_id).hardware_used == true)
            return -1;

        Date t_now = new Date();
        Hardware tmp=hardwares.get(handware_id);
        tmp.free_to_use(t_now, pid, time_plan);
        hardwares.set(handware_id,tmp);
        used_count++;
        return 1;
    }

    //给某个进程pid对应的硬件停止执行，若该进程有对应的硬件返回1，停止执行成功，返回1，否则返回-1
    public int stop_hardware_pid(int pid)
    {
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_pid == pid)
            {
                Date t_now = new Date();
                Hardware tmp=hardwares.get(i);
                tmp.used_to_free(t_now);
                hardwares.set(i,tmp);
                used_count--;
                return 1;
            }
        }

        return -1;
    }

    //给某个硬件停止执行，若有对应的硬件返回1，停止执行成功，返回1，否则返回-1
    public int stop_hardware_id(int handware_id)
    {
        if(handware_id + 1 > hardware_count)
            return -1;

        Date t_now = new Date();
        Hardware tmp=hardwares.get(handware_id);
        tmp.used_to_free(t_now);
        hardwares.set(handware_id,tmp);
        used_count--;
        return 1;
    }


    //得到正在运行的硬件的相关信息
    public ArrayList<Hardware> get_hardware_used_info()
    {
        ArrayList<Hardware> hardwares_used = new ArrayList<>();//所使用的硬件列表
        for(int i = 0; i < hardware_count; i++)
        {

            hardwares_used.add(hardwares.get(i));

        }
        return hardwares_used;
    }



    //得到空闲的硬件的相关信息
    public ArrayList<Hardware> get_hardware_free_info()
    {
        ArrayList<Hardware> hardwares_free = new ArrayList<>();//空闲的硬件列表
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_used == false)
            {
                hardwares_free.add(hardwares.get(i));
            }
        }
        return hardwares_free;
    }

    //得到某个种类的硬件的相关信息
    public ArrayList<Hardware> get_hardware_cate_info(String cate)
    {
        ArrayList<Hardware> hardwares_free = new ArrayList<>();//空闲的硬件列表
        for(int i = 0; i < hardware_count; i++)
        {
            if(hardwares.get(i).hardware_cate.equals(cate) )
            {
                hardwares_free.add(hardwares.get(i));
            }
        }
        return hardwares_free;
    }

    //得到某个硬件的相关信息
    public Hardware get_hardware_info(int handware_id)
    {
        return hardwares.get(handware_id);
    }

    //测试用，打印所有设备的相关信息



    //主函数。测试用


}

