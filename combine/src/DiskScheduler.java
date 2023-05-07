import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.*;


public class DiskScheduler {
    private int initialPosition;
    private int totalDistance;
    private int[] requests;
    private int endPosition = 199;
    private int startPosition = 0;
    DiskScheduler(){

    }

    public void Disk_find( int[] requests) {
        //对resquests的所有元素取模
        for(int i=0;i<requests.length;i++){
            requests[i]=requests[i]%(endPosition-startPosition+1);
        }
        //随机生成初始位置0-199
        this.initialPosition = initialPosition%(endPosition-startPosition+1);
        this.requests = requests;
        List<Integer> result = allResult();
        //把result写入附加在已有的System/block_access文件中

        try {
            String content = "";
            FileWriter fw = new FileWriter("System/block_access", true);
            content=initialPosition+" ";
            //把requests组成字符串,用空格分隔
            for(int i=0;i<requests.length;i++){
                content += requests[i]+" ";
            }
            content += "\n";
            content += "FIFO: " + result.get(0) + " ";
            content += "SSTF: " + result.get(1) + " ";
            content += "SCAN: " + result.get(2) + " ";
            content += "CSCAN: " + result.get(3) + " ";
            content += "LOOK: " + result.get(4) + " ";
            content += "CLOOK: " + result.get(5) + "\n";
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public List<Integer> allResult(){
        return Arrays.asList(FIFO(),SSTF(),SCAN(),CSCAN(),LOOK(),CLOOK());
    }
    public int FIFO() {
        // 复制请求列表
        int[] queue = Arrays.copyOf(this.requests, this.requests.length);
        // 初始化磁头位置
        int current = this.initialPosition;
        // 初始化磁头移动距离为0
        int distance = 0;
        // 遍历请求队列，按照先进先出的顺序处理请求
        for (int i = 0; i < queue.length; i++) {
            // 计算当前请求和磁头位置之间的距离
            int diff = Math.abs(current - queue[i]);
            // 将磁头移动到当前请求的位置
            current = queue[i];
            // 累计磁头移动距离
            distance += diff;
        }
        // 返回磁头移动距离
        return distance;
    }
    public int SSTF() {
        // 复制请求列表
        int[] queue = Arrays.copyOf(requests, requests.length);
        // 计算请求总数
        int n = queue.length;
        // 记录已访问的请求
        boolean[] visited = new boolean[n];
        // 记录磁头当前位置
        int current = this.initialPosition;
        // 记录磁头移动距离
        int totalDist = 0;
        // 循环处理所有请求
        for (int i = 0; i < n; i++) {
            // 计算未访问请求中与当前位置最近的请求
            int closestIndex = -1;
            int minDist = Integer.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (!visited[j]) {
                    int dist = Math.abs(queue[j] - current);
                    if (dist < minDist) {
                        closestIndex = j;
                        minDist = dist;
                    }
                }
            }
            // 记录本次移动距离并标记该请求为已访问
            totalDist += minDist;
            visited[closestIndex] = true;
            current = queue[closestIndex];
        }
        return totalDist;
    }
    public int LOOK() {
        int[] queue = Arrays.copyOf(requests, requests.length);
        Arrays.sort(queue);
        Integer[] moveDistance = new Integer[queue.length]; // 记录移动距离
        Integer[] orderhead = new Integer[queue.length];
        int distance = 0;

        int readWriteHead = this.initialPosition; // 读写头位置初始化
        int index = 0;          // 用于记录下标

        for(int i=0;i<queue.length; i++){
            if(queue[i] >= readWriteHead){
                index=i;
                break;
            }
        }

        int num = 0; // 用于记录order中下标的位置
        // 先向增大方向
        for (int k = index; k < queue.length; k++,num++) {
            moveDistance[num] = Math.abs(queue[k] - readWriteHead);
            readWriteHead = queue[k];
            orderhead[num] = queue[k];
        }
        // 再向减小方向
        for (int j = index - 1; j >= 0; j--,num++) {
            moveDistance[num] = readWriteHead - queue[j];
            readWriteHead = queue[j];
            orderhead[num]=queue[j];
        }
        for(int i: moveDistance) {
            distance += i;
        }
        return distance;
    }
    public int CLOOK() {
        int[] queue = Arrays.copyOf(requests, requests.length);
        Arrays.sort(queue);
        Integer[] moveDistance = new Integer[queue.length]; // 记录移动距离
        Integer[] orderhead=new Integer[queue.length];
        int distance = 0;
        int readWriteHead = initialPosition; // 读写头在 100
        int index = 0;          // 用于记录下标

        for(int i=0;i<queue.length; i++){
            if(queue[i] >= readWriteHead){
                index=i;
                break;
            }
        }
        int num = 0; // 用于记录order中下标的位置

        // 向磁道号递增方向扫描，扫描方向不变

        while(num!=queue.length) {
            if(index==queue.length){
                index=0;
            }
            moveDistance[num] = Math.abs(queue[index] - readWriteHead);
            readWriteHead = queue[index];
            orderhead[num]=queue[index];
            index++;
            num++;
        }

        for(int i: moveDistance) {
            distance += i;
        }

        return distance;
    }
    public int SCAN() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            arrayList.add(requests[i]);
        }
        arrayList.add(endPosition);
        int[] queue = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            queue[i] = arrayList.get(i);
        }
        Arrays.sort(queue);
        Integer[] moveDistance = new Integer[queue.length]; // 记录移动距离
        Integer[] orderhead = new Integer[queue.length];
        int distance = 0;

        int readWriteHead = this.initialPosition; // 读写头位置初始化
        int index = 0;          // 用于记录下标

        for (int i = 0; i < queue.length; i++) {
            if (queue[i] >= readWriteHead) {
                index = i;
                break;
            }
        }

        int num = 0; // 用于记录order中下标的位置
        // 先向增大方向
        for (int k = index; k < queue.length; k++, num++) {
            moveDistance[num] = Math.abs(queue[k] - readWriteHead);
            readWriteHead = queue[k];
            orderhead[num] = queue[k];
        }
        // 再向减小方向
        for (int j = index - 1; j >= 0; j--, num++) {
            moveDistance[num] = readWriteHead - queue[j];
            readWriteHead = queue[j];
            orderhead[num] = queue[j];
        }
        for (int i : moveDistance) {

            distance += i;
        }

        return distance;
    }
    public int CSCAN() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            arrayList.add(requests[i]);
        }
        arrayList.add(endPosition);
        arrayList.add(startPosition);
        int[] queue = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            queue[i] = arrayList.get(i);
        }
        Arrays.sort(queue);
        Integer[] moveDistance = new Integer[queue.length]; // 记录移动距离
        Integer[] orderhead=new Integer[queue.length];
        int distance = 0;
        int readWriteHead = initialPosition;
        int index = 0;          // 用于记录下标

        for(int i=0;i<queue.length; i++){
            if(queue[i] >= readWriteHead){
                index=i;
                break;
            }
        }
        int num = 0; // 用于记录order中下标的位置

        // 向磁道号递增方向扫描，扫描方向不变

        while(num!=queue.length) {
            if(index==queue.length){
                index=0;
            }
            moveDistance[num] = Math.abs(queue[index] - readWriteHead);
            readWriteHead = queue[index];
            orderhead[num]=queue[index];
            index++;
            num++;
        }

        for(int i: moveDistance) {
            distance += i;
        }
        return distance;
    }
}

