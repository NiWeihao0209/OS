import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Memory {
    //启动函数

    class con_table{//连续储存的表
        int available=1;
        int start;//开始地址
        int end;//结束地址
        int pid=-1;//访问号
        int size;//大小
        int num;
    }
    class page_table{//分页储存的表
        int page_number;
        int pid=-1;//访问号
        int available=1;
    }
    class process{
        int pid;
        int size;
        //模式
        int []page=new int[]{-1,-1,-1,-1,-1,-1,-1,-1};//储存过进程的页表
        int in=0;//是否被分配内存
        String name;

    }
    class virtual_page{
        int number=-1;
        int time=0;
    }
    static int con_time=0;//第几个页表
    static int page_size=1024;
    static int page_number=8;//虚拟内存大小
    static int physical_page=4;//物理内存
    static  int size_virtual_page=3;//虚拟页面大小
    static int total_size=page_size*page_number;
    static ArrayList <virtual_page> virtual_page=new ArrayList<virtual_page>();


    String schedule;
    String mode_alloc;//连续分配
    static ArrayList<con_table>  con_table=new ArrayList<con_table>();
    static ArrayList<page_table> page_table=new ArrayList<page_table>();
    static ArrayList<process> process=new ArrayList<process>();



    //连续内存分配算法
    Memory() throws IOException {
        //初始化连续内存表
        con_table new_con_table=new con_table();
        new_con_table.start=0;
        new_con_table.end=total_size-1;
        new_con_table.size=total_size;
        new_con_table.num=con_time;
        con_time++;
        con_table.add(new_con_table);
        mode_alloc="ca";//第一行是分配模式
        schedule="LRU";//第二行分配查页方式
        //初始化虚拟表三个节点,初始都为-1
        for(int i=0;i<size_virtual_page;i++){
            virtual_page new_page=new virtual_page();
            virtual_page.add(new_page);
        }
        //建立大小为page_number的page_table
        for(int i=0;i<page_number;i++){
            page_table new_page=new page_table();
            new_page.page_number=i;
            page_table.add(new_page);
        }

    }
    //导入进程
    public void add_process(int pid,String name,int size){
        process new_process=new process();
        new_process.size=size;
        new_process.pid=pid;
        new_process.name=name;
        //把new_process加入process数组
        process.add(new_process);
        //给内存分配空间
        analyseProcess(new_process);
    }

    public  void analyseProcess(process target){
        //分析进程的内容
        if(mode_alloc.equals("ca")){

            continue_alloc(target);
        }
        else if(mode_alloc.equals("pa")){

            page_alloc(target);
        }
        else{
            System.out.println("分配模式错误");
        }



    }
    public void continue_alloc(process target){

        //连续内存分配
        //遍历table || FIFO算法
        for(int i=0;i<con_table.size();i++){
            if(con_table.get(i).size==target.size && con_table.get(i).available==1 ){//正好等于且可用
                //如果找到了
                con_table.get(i).pid=target.pid;
                con_table.get(i).available=0;
                target.page[0]=i;
                target.in=1;
                return;
            }
            else if(con_table.get(i).size>target.size && con_table.get(i).available==1){//大于且可用
                con_table new_table=new con_table();
                new_table.pid=target.pid;
                new_table.size=target.size;
                new_table.start=con_table.get(i).start;
                new_table.end=new_table.start+target.size-1;
                new_table.available=0;
                new_table.num=con_time;
                target.page[0]=con_time;
                target.in=1;
                con_time++;
                con_table.add(new_table);

                //修改原来的表
                con_table.get(i).start=new_table.end+1;
                con_table.get(i).size=con_table.get(i).size-target.size;

            }

        }


    }
    public static void page_alloc(process target){
        //储存页号
        int p=0;
        //分页内存分配
        //找出空闲的页的总数


        //遍历page_table
        for(int i=0;i<page_table.size();i++){
            //遇到空闲页就把该页号加入到进程的页表中,减去页大小,直到进程的大小为0
            if(page_table.get(i).available==1 ){
                page_table.get(i).pid=target.pid;
                page_table.get(i).available=0;
                //把该页号加入到进程的页表数组中
                target.page[p]=page_table.get(i).page_number;
                p++;
                target.size-=page_size;
            }
            if(target.size<=0){
                target.in=1;
                return;//内容完全分配完毕
            }
        }

    }
    //访问内存access[1999]
    public void access_memory(int pid, int address){
        //根据pid找到进程
        process target=null;
        for(int i=0;i<process.size();i++){
            if(process.get(i).pid==pid){
                target=process.get(i);
                break;
            }
        }
        if(target.in==0){
            System.out.println("进程未进入内存");
            return;
        }


        //访问内存
        if(mode_alloc.equals("ca")){
            access_con(target);
        }
        else if(mode_alloc.equals("pa")){
            access_page(target,address);
        }
        else{
            System.out.println("分配模式错误");
        }
    }
    //访问页表
    public static void access_con(process target){
        //看看target.page[0]是否在虚拟页中
        System.out.println("pid:"+target.pid+" "+target.page[0]);
        for(int i=0;i<virtual_page.size();i++){
            if(virtual_page.get(i).number==target.page[0]){
                //如果在虚拟页中,访问次数加1
                virtual_page.get(i).time++;
                //读取i项
                int time=virtual_page.get(i).time;
                int number=virtual_page.get(i).number;
                //arraylist virtual_page的i项移到最前面
                for(int j=i;j>0;j--){
                    virtual_page.get(j).time=virtual_page.get(j-1).time;
                    virtual_page.get(j).number=virtual_page.get(j-1).number;

                }
                //0项赋值
                virtual_page.get(0).time=time;
                virtual_page.get(0).number=number;
                return;
            }
        }
        //如果不在虚拟页中,删除最后一个,将target.page[0]放在最前面
        for(int j=virtual_page.size()-1;j>0;j--){
            virtual_page.get(j).time=virtual_page.get(j-1).time;
            virtual_page.get(j).number=virtual_page.get(j-1).number;
        }
        virtual_page.get(0).number=target.page[0];
        virtual_page.get(0).time=1;
    }
    //访问页表
    public static void access_page(process target,int address){
        //访问连续表
        int target_page_number=(address)/1024;
        target_page_number=target.page[target_page_number];
        //如果target_page_number为-1,则访问页面超过范围
        if(target_page_number==-1){
            System.out.println("访问地址超过限制");
        }
        //看看target_page_number是否在虚拟页中
        for(int i=0;i<virtual_page.size();i++){
            if(virtual_page.get(i).number==target_page_number){

                //如果在虚拟页中,访问次数加1
                virtual_page.get(i).time++;
                //读取i项
                int time=virtual_page.get(i).time;
                int number=virtual_page.get(i).number;
                //如果在虚拟页中,将数字的i处移动到最前面
                for(int j=i;j>0;j--){
                    virtual_page.get(j).time=virtual_page.get(j-1).time;
                    virtual_page.get(j).number=virtual_page.get(j-1).number;
                }
                //0项赋值
                virtual_page.get(0).time=time;
                virtual_page.get(0).number=number;
                return;
            }
        }
        //如果不在虚拟页中,删除最后一个,将target_page_number放在最前面
        for(int j=virtual_page.size()-1;j>0;j--){
            virtual_page.get(j).time=virtual_page.get(j-1).time;
            virtual_page.get(j).number=virtual_page.get(j-1).number;
        }
        virtual_page.get(0).number=target_page_number;
        virtual_page.get(0).time=1;
    }
    void release_memory(int pid){
        //用pid找到对应process
        process target=null;
        for(int i=0;i<process.size();i++){
            if(process.get(i).pid==pid){
                target=process.get(i);
                break;
            }
        }
        if(target.in==0){
            System.out.println("进程未进入内存");
            return;
        }
        //释放内存
        if(mode_alloc.equals("ca")){
            release_con(target);
        }
        else if(mode_alloc.equals("pa")){
            release_page(target);
        }
        else{
            System.out.println("分配模式错误");
        }
    }
    //释放连续表
    public static void release_con(process target){
        //将进程的页表数组中的页号全部置为-1
        con_table.get(target.page[0]).available=1;//将页表中的available置为1
        con_table.get(target.page[0]).pid=-1;
        //先与后面的连续块合并
        if( con_table.get(target.page[0]+1).available==1){
            con_table.get(target.page[0]).end=con_table.get(target.page[0]+1).end;
            //删除con_table.get(target.page[0]+1)
            con_table.remove(target.page[0]+1);
        }
        //若不为第一个块,则和前面的连续块合并
        if(target.page[0]!=0 && con_table.get(target.page[0]-1).available==1){
            con_table.get(target.page[0]).start=con_table.get(target.page[0]-1).start;
            //删除con_table.get(target.page[0])
            con_table.remove(target.page[0]-1);
        }

    }
    //释放页表
    public static void release_page(process target){
        //将进程的页表数组中的页号对应的页表项的available置为1
        for(int i=0;i<target.page.length;i++){
            if(target.page[i]!=-1) {
                page_table.get(target.page[i]).available = 1;
                page_table.get(target.page[i]).pid = -1;
                //将页表中的available置为1}
            }
        }

    }
    List<String> check(){
        //打印virtual_page
        List<String> list=new ArrayList<String>();
        System.out.println("virtual_page:");
        list.add("virtual_page:");
        for(int i=0;i<virtual_page.size();i++){
            System.out.println(virtual_page.get(i).number+" "+virtual_page.get(i).time);
            list.add(virtual_page.get(i).number+" 被访问次数"+virtual_page.get(i).time);
        }

        //打印page_table
        if(mode_alloc.equals("pa")) {
            System.out.println("page_table:");
            list.add("page_table:");

            for (int i = 0; i < page_table.size(); i++) {
                System.out.println("pid:"+page_table.get(i).pid + " 对应页码为" + page_table.get(i).page_number);
                list.add("pid:"+page_table.get(i).pid + " 对应页码为" + page_table.get(i).page_number);

            }
        }
        //打印con_table
        if(mode_alloc.equals("ca")) {
            System.out.println("con_table:");
            list.add("con_table:");
            for (int i = 0; i < con_table.size(); i++) {

                System.out.println(con_table.get(i).start + " " + con_table.get(i).end);
                list.add("pid:"+con_table.get(i).pid +" 开始处:"+con_table.get(i).start + " 结束处" + con_table.get(i).end);
            }
        }
        return list;
    }
    Boolean check_memory(int size){
        //检查内存是否已满
        if(mode_alloc.equals("ca")){
            //找con_table中available为1的最大的块
            int max=0;
            for(int i=0;i<con_table.size();i++){
                if(con_table.get(i).available==1){
                    if(con_table.get(i).end-con_table.get(i).start>max){
                        max=con_table.get(i).end-con_table.get(i).start;
                    }
                }
            }
            //如果最大的块小于size,返回false
            if(max<size){
                System.out.println("内存不够");
                return false;
            }
            return true;

        }
        else if(mode_alloc.equals("pa")){
            int available=0;
            for(int i=0;i<page_table.size();i++){
                if(page_table.get(i).available==1){
                    available++;
                }
            }
            if(size>available*page_size){
                //如果空闲的页不够
                System.out.println("内存不够");
                return false;
            }
            return true;
        }
        return false;

    }

}