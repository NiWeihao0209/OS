import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//连续分配式存储管理
class ConProcess {
    private String name; // 进程名称
    private int size; // 进程大小
    private MemoryBlock block; // 进程所在内存块
    int pid; // 进程id

    public ConProcess(String name, int size, int pid){
        this.name = name;
        this.size = size;
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public MemoryBlock getBlock() {
        return block;
    }

    public void setBlock(MemoryBlock block) {
        this.block = block;
    }

    public void releaseBlock() {
        block.setProcess(null);
    }
}

class MemoryBlock {
    private int start; // 内存块起始地址
    private int end; // 内存块结束地址
    private int size; // 内存块大小
    private boolean isFree; // 内存块是否空闲
    private ConProcess process; // 内存块所分配的进程

    public MemoryBlock(int start, int size) {
        this.start = start;
        this.end = start + size;
        this.size = size;
        this.isFree = true;
        this.process = null;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getSize() {
        return size;
    }

    public boolean isFree() {
        return isFree;
    }

    public ConProcess getProcess() {
        return process;
    }

    public void setProcess(ConProcess process) {
        this.process = process;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public int getPid() {
        if (process != null) {
            return process.pid;
        } else {
            return -1;
        }
    }
}

class ConMemoryManager {
    private List<MemoryBlock> freeList;
    private List<MemoryBlock> allocatedList;
    private int memorySize;
    private HashMap<Integer, ConProcess> ProcessMap = new HashMap<Integer, ConProcess>();

    public ConMemoryManager(int size) {    //size为内存总大小
        freeList = new ArrayList<>();
        allocatedList = new ArrayList<>();
        MemoryBlock freeBlock = new MemoryBlock(0, size);
        freeList.add(freeBlock);
        memorySize = size;
    }

    public boolean allocate(ConProcess p) {
        int processSize = p.getSize();
        boolean allocated = false;
        for (int i = 0; i < freeList.size(); i++) {
            MemoryBlock block = freeList.get(i);
            if (block.getSize() >= processSize) {
                MemoryBlock allocatedBlock = new MemoryBlock(block.getStart(), processSize);
                allocatedBlock.setProcess(p);
                allocatedBlock.setFree(false);
                allocatedList.add(allocatedBlock);
                p.setBlock(allocatedBlock);
                if (block.getSize() > processSize) {
                    MemoryBlock freeBlock = new MemoryBlock(block.getStart() + processSize, block.getSize() - processSize);
                    freeList.add(i + 1, freeBlock);
                }
                freeList.remove(i);
                allocated = true;
                break;
            }
        }
        return allocated;
    }

    public void deallocate(ConProcess p) {
        MemoryBlock allocatedBlock = p.getBlock();
        allocatedList.remove(allocatedBlock);
        MemoryBlock mergedBlock = mergeBlocks(allocatedBlock, getAdjacentBlocks(allocatedBlock));
        freeList.add(mergedBlock);
        ProcessMap.remove(p.pid);
    }

    private List<MemoryBlock> getAdjacentBlocks(MemoryBlock block) {
        List<MemoryBlock> adjacentBlocks = new ArrayList<>();
        int startIndex = -1;
        int endIndex = -1;
        for (int i = 0; i < freeList.size(); i++) {
            MemoryBlock freeBlock = freeList.get(i);
            if (freeBlock.getStart() + freeBlock.getSize() == block.getStart()) {
                endIndex = i;
            } else if (block.getStart() + block.getSize() == freeBlock.getStart()) {
                startIndex = i;
            }
        }
        if (startIndex != -1) {
            adjacentBlocks.add(freeList.remove(startIndex));
        }
        if (endIndex != -1) {
            adjacentBlocks.add(freeList.remove(endIndex - (startIndex != -1 ? 1 : 0)));
        }
        return adjacentBlocks;
    }

    private MemoryBlock mergeBlocks(MemoryBlock block, List<MemoryBlock> adjacentBlocks) {
        int start = block.getStart();
        int size = block.getSize();
        for (MemoryBlock adjacentBlock : adjacentBlocks) {
            if (adjacentBlock.getStart() < start) {
                start = adjacentBlock.getStart();
            }
            size += adjacentBlock.getSize();
        }
        return new MemoryBlock(start, size);
    }

    public int getMemorySize() {
        return memorySize;
    }

    public List<String> getFreeList() {
        List<String> freeListString = new ArrayList<>();
        for (int i = 0; i < freeList.size(); i++) {
            MemoryBlock block = freeList.get(i);
            freeListString.add("Block " + i + ": " + block.getStart() + " - " + block.getEnd() + " " + "free" );
        }
        return freeListString;
    }

    public List<String> getAllocatedList() {
        List<String> allocatedListString = new ArrayList<>();
        for (int i = 0; i < allocatedList.size(); i++) {
            MemoryBlock block = allocatedList.get(i);
            allocatedListString.add("Block " + i + ": " + block.getStart() + " - " + block.getEnd() + " " + "allocated");
        }
        return allocatedListString;
    }

    public void printFreeMemory() {
        for (int i = 0; i < freeList.size(); i++) {
            MemoryBlock block = freeList.get(i);
            System.out.println("Block " + i + ": " + block.getStart() + " - " + block.getEnd() + " " + "free" );
        }
    }

    public void printAllocated() {
        for (int i = 0; i < allocatedList.size(); i++) {
            MemoryBlock block = allocatedList.get(i);
            System.out.println("Block " + i + ": " + block.getStart() + " - " + block.getEnd() + " " + "allocated");
        }
    }

    public boolean createProcess(int pid,String name,int size){
        ConProcess p = new ConProcess(name, size, pid);
        ProcessMap.put(pid, p);
        if(allocate(p)){
            ProcessMap.put(pid, p);
        }
        else{
            return false;
        }
        return true;
    }

    public int accessProcess(int pid, int address){
        //返回0表示进程不存在，返回1表示访问成功，返回2表示访问越界，返回3表示进程未分配内存，返回4表示其他错误
        ConProcess p = ProcessMap.get(pid);
        if(p == null){
            return 0;
        }
        //遍历allocatedList，找到pid所在的内存块
        MemoryBlock block;
        for(int i = 0; i < allocatedList.size(); i++){
            block = allocatedList.get(i);
            if(block.getPid() == pid){
                if(block.getStart() <= address && address <= block.getEnd()){
                    return 1;
                } else {
                    return 2;
                }
            }
            return 3;
        }
        return 4;
    }

    public ConProcess getProcess(int pid){
        return ProcessMap.get(pid);
    }

}

//页式存储管理

class PageTableEntry {
    //  pageFrameNumber：页框号，表示该页表项对应的物理页框的编号。
    //  present：是否存在位，表示该页表项对应的虚拟页面是否存在于物理内存中。
    //  referencedTime：最近被访问的时间戳，用于页面置换算法中的参考位。
    //  modifiedTime：最近被修改的时间戳，用于页面置换算法中的修改位。
    //  pid：进程id，表示该页表项对应的页面属于哪个进程。
    //  visits：访问次数，表示该页表项对应的页面被访问的次数。
    private int pageFrameNumber;
    private boolean present;
    private long referencedTime;
    private long modifiedTime;
    private int pid;
    private int visits;

    public PageTableEntry() {
        this.modifiedTime = System.currentTimeMillis();
        this.pageFrameNumber = -1;
        this.present = false;
        this.referencedTime = -1;
        this.pid = -1;
        this.visits = 0;
    }

    public int getPageFrameNumber() {
        return pageFrameNumber;
    }

    public void setPageFrameNumber(int pageFrameNumber) {
        this.pageFrameNumber = pageFrameNumber;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public long getReferencedTime() {
        return referencedTime;
    }

    public void setReferencedTime(long referencedTime) {
        this.referencedTime = referencedTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getVisits() {
        return visits;
    }

    public void setVisits(int visits) {
        this.visits = visits;
    }
}

class PageMemoryManager{
    //  Memory：里面包括物理内存与虚拟内存
    //  pageFrameSize：页框大小，表示一个页框的大小。
    //  pageFrameNum：页框数量，表示物理内存中的页框数量。
    //  virtualMemoryNum：虚拟内存数量，表示虚拟内存的页数。
    //  pageTableSize：页表大小，表示一个页目录中的页表大小。
    //  pageTableNum：页表数量，表示总页表数量。
    //  pageFault：缺页次数，表示缺页的次数。
    //  pageReplace：页面置换次数，表示页面置换的次数。
    //  pageReplaceAlgorithm：页面置换算法，表示页面置换的算法。
    //  pageTable：页表，表示页表。
    private MemoryPage memory;
    private int pageFrameSize;
    private int pageFrameNum;
    private int pageTableSize;
    private int pageTableNum;
    private int pageFault = 0;
    private int pageReplace = 0;
    private int freePageFrameNum;
    private int freePageNum;
    private String pageReplaceAlgorithm;
    private HashMap<Integer, PageProcess> processMap = new HashMap<Integer, PageProcess>();
    private PageTable pageTable;

    public PageMemoryManager(int pageFrameSize, int pageFrameNum, int pageTableSize, int pageTableNum, String pageReplaceAlgorithm) {
        this.pageFrameSize = pageFrameSize;
        this.pageFrameNum = pageFrameNum;
        this.pageTableSize = pageTableSize;
        this.pageTableNum = pageTableNum;
        this.pageReplaceAlgorithm = pageReplaceAlgorithm;
        this.memory = new MemoryPage(pageFrameNum, pageFrameSize, pageTableNum, pageTableSize);
        this.pageTable = new PageTable(pageTableNum);
        this.freePageNum = pageTableNum;
        this.freePageFrameNum = pageFrameNum;
    }
    public boolean createProcess(int pid,String name,int size){
        PageProcess p = new PageProcess(name, size, pid);
        if(allocateMemory(p)){
            processMap.put(p.getPid(), p);
        }
        else {
            return false;
        }
        return true;
    }


    // 分配内存(策略)
    public boolean allocateMemory(PageProcess process) {
        int size = process.getSize();
        int pageNum = size / 4096;
        if (size % pageTableSize != 0) {
            pageNum++;
        }
        if (freePageFrameNum < pageNum) {
            //内存不足，无法分配
            return false;
        }
        int[] pageDir = new int[pageNum];
        int j = 0;
        int i=0;
        while(j!=pageNum){
            PageTableEntry pageTableEntry = pageTable.getEntry(i);
            if (pageTableEntry.getPid() == -1){
                pageTableEntry.setPid(process.getPid());
                pageTableEntry.setModifiedTime(System.currentTimeMillis());
                freePageNum--;
                pageDir[j] = i;
                j++;
            }
            i++;
        }
        process.setPageDirectory(pageDir);
        return true;
    }

    public int accessProcess(PageProcess process, int address){
        //返回1表示访问成功，返回0表示访问越界，返回2表示缺页
        int pid = process.getPid();
        int pageNum = address / pageTableSize;
        int offset = address % pageTableSize;
        int pageDir[] = process.getPageDirectory();
        if(pageNum >= pageDir.length){
            System.out.println("访问越界");
            return 0;
        }
        int pageTableIndex = pageDir[pageNum];
        PageTableEntry pageTableEntry = pageTable.getEntry(pageTableIndex);
        if (pageTableEntry.isPresent()){
            pageTableEntry.setReferencedTime(System.currentTimeMillis());
            return 1;
        }
        else {
            pageFault++;
            int pageFrameIndex;
            if (freePageFrameNum > 0){
                // 有空闲页框
                pageFrameIndex = memory.findFreePageFrame();
                memory.useFrame(pageFrameIndex);
                pageTableEntry.setPageFrameNumber(pageTableIndex);
                pageTableEntry.setPresent(true);
                pageTableEntry.setReferencedTime(System.currentTimeMillis());
                pageTableEntry.setPid(pid);
                freePageFrameNum--;
            }
            else {
                // 无空闲页框
                pageFrameIndex =  1;//TODO: pageReplaceAlgorithm();
                pageTableEntry.setPageFrameNumber(pageFrameIndex);
                pageTableEntry.setPresent(true);
                pageTableEntry.setReferencedTime(System.currentTimeMillis());
                pageTableEntry.setPid(pid);
                pageReplace++;
            }
            return 2;
        }
    }

    public void showMemory(){
        System.out.println("页框数量：" + pageFrameNum);
        System.out.println("页表数量：" + pageTableNum);
        System.out.println("缺页次数：" + pageFault);
        System.out.println("页面置换次数：" + pageReplace);
        System.out.println("页面置换算法：" + pageReplaceAlgorithm);
        System.out.println("空闲页表数量：" + freePageNum);
        System.out.println("空闲页框数量：" + freePageFrameNum);
    }

    public List<String> getBriefUsage(){
        List<String> memoryUsageString = new ArrayList<>();
        memoryUsageString.add("页框数量：" + pageFrameNum);
        memoryUsageString.add("页表数量：" + pageTableNum);
        memoryUsageString.add("缺页次数：" + pageFault);
        memoryUsageString.add("页面置换次数：" + pageReplace);
        memoryUsageString.add("页面置换算法：" + pageReplaceAlgorithm);
        memoryUsageString.add("空闲页表数量：" + freePageNum);
        memoryUsageString.add("空闲页框数量：" + freePageFrameNum);
        return memoryUsageString;
    }

    public List<String> getDetailedUsage(){
        //TODO(后期) 输出详细内存的使用信息（按照每个进程）
        return null;
    }

    public PageProcess getProcess(int pid){
        return processMap.get(pid);
    }

    public boolean deleteProcess(int pid){
        //TODO:完善删除进程的方法
        boolean deleteProcessResult = false;
        PageProcess process = processMap.get(pid);
        int[] pageDir = process.getPageDirectory();
        for (int i = 0; i < pageDir.length; i++) {
            PageTableEntry pageTableEntry = pageTable.getEntry(pageDir[i]);
            pageTableEntry.setPid(-1);
            pageTableEntry.setPresent(false);
            pageTableEntry.setReferencedTime(0);
            pageTableEntry.setModifiedTime(0);
            pageTableEntry.setPageFrameNumber(-1);
            freePageNum++;
            freePageFrameNum++;
        }
        processMap.remove(pid);
        return deleteProcessResult;
    }
}

class PageTable {
    private PageTableEntry[] entries; // 存储页表项的数组

    public PageTable(int numEntries) {
        this.entries = new PageTableEntry[numEntries]; // 初始化页表项数组
        for(int i = 0; i < numEntries; i++) {
            entries[i] = new PageTableEntry(); // 初始化每个页表项
        }
    }

    // 获取页表项
    public PageTableEntry getEntry(int index) {
        return entries[index];
    }

    // 设置页表项
    public void setEntry(int index, PageTableEntry entry) {
        entries[index] = entry;
    }
}


// PhysicalMemory类，表示物理内存，包括多个页框
class MemoryPage {
    private int pageFrameSize;
    private int pageTableNum;
    private int pageTableSize;
    private PageFrame[] pageFrames;
    private Page[] pages;

    public MemoryPage(int numFrames, int pageFrameSize, int pageTableNum, int pageTableSize) {
        // 初始化页框(物理内存)
        this.pageFrameSize = pageFrameSize;
        this.pageFrames = new PageFrame[numFrames];
        this.pages = new Page[pageTableNum];

        for (int i = 0; i < numFrames; i++) {
            pageFrames[i] = new PageFrame(i,pageFrameSize);
        }

        // 初始化页表(虚拟内存)
        this.pageTableNum = pageTableNum;
        this.pageTableSize = pageTableSize;
        for (int i = numFrames; i < pageTableNum; i++) {
            pages[i] = new Page(i, null);
        }
    }

    public PageFrame getPageFrame(int index) {
        return pageFrames[index];
    }

    public int findFreePageFrame(){
        for (int i = 0; i < pageFrameSize; i++) {
            if (pageFrames[i].isFree()){
                return i;
            }
        }
        return -1;
    }

    public void freeFrame(int index){
        pageFrames[index].setFree(true);
    }

    public void useFrame(int index){
        pageFrames[index].setFree(false);
    }
}

// 页框类，表示物理页框，包括页框号和页
class PageFrame {
    private int frameNumber; //   页框号
    private int pageFrameSize; // 页面大小
    private boolean isFree = true; // 是否空闲
    private byte[] data; // 存储的数据

    public PageFrame(int frameNumber,int pageFrameSize) {
        this.frameNumber = frameNumber;
        this.pageFrameSize = pageFrameSize;
    }

    public int getId() {
        return frameNumber;
    }


    public boolean setData(byte[] data) {
        if (data.length > pageFrameSize) {
            return false;
        }
        this.data = data;
        return true;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean state) {
        isFree = state;
    }
}

class Page {
    private int pageNumber; // 页面号
    private byte[] data; // 存储的数据

    public Page(int pageNumber, byte[] data) {
        this.pageNumber = pageNumber;
        this.data = data;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

class PageProcess {
    private String name;
    private int pid;
    private int size;   // 进程大小
    private int[] pageDirectory;    // 记得改为HashMap来存储————————————————————————————————————————————

    public PageProcess(String name, int pid, int size) {
        this.name = name;
        this.pid = pid;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public int[] getPageDirectory() {
        return pageDirectory;
    }

    public void setPageDirectory(int[] pageDirectory) {
        this.pageDirectory = pageDirectory;
    }

    public int getPid() {
        return pid;
    }

    public int getSize() {
        return size;
    }
}

public class Memory {
    static String testName = "ca";
    ConMemoryManager conMemoryManager;
    PageMemoryManager pageMemoryManager;
    Memory(){
        if (testName.equals("ca")) {
            conMemoryManager = new ConMemoryManager(10000);
        }
        else if (testName.equals("pa")) {
            pageMemoryManager = new PageMemoryManager(4096, 8, 4096, 24, "LRU");
        }

    }
    List<String> showAllMemory(){
        List<String> allMemoryList = new ArrayList<>();
        if (testName.equals("ca")) {
            allMemoryList.addAll(conMemoryManager.getFreeList());
            allMemoryList.addAll(conMemoryManager.getAllocatedList());
        }
        else if (testName.equals("pa")) {
            allMemoryList.addAll(pageMemoryManager.getBriefUsage());
        }
        return allMemoryList;
    }
    boolean createProcess(int pid,String name,int size){
        boolean createResult = false;
        if (testName.equals("ca")) {
            createResult = conMemoryManager.createProcess(pid, name, size);
        }
        else if (testName.equals("pa")) {
            createResult = pageMemoryManager.createProcess(pid, name, size);
        }
        return createResult;
    }
    int access(int pid, int address){
        int accessResult = 0;
        if (testName.equals("ca")) {
            accessResult = conMemoryManager.accessProcess(pid, address);
        }
        else if (testName.equals("pa")) {
            accessResult = pageMemoryManager.accessProcess(pageMemoryManager.getProcess(pid), address);
        }
        return accessResult;
    }
    boolean release(int pid){
        boolean releaseResult = false;
        if (testName.equals("ca")) {
            ConProcess process = conMemoryManager.getProcess(pid);
            if(process != null){
                conMemoryManager.deallocate(process);
                releaseResult = true;
            }
        }
        else if (testName.equals("pa")) {
            //TODO:pageMemoryManager.release();
            releaseResult = pageMemoryManager.deleteProcess(pid);
        }
        return releaseResult;
    }

}
