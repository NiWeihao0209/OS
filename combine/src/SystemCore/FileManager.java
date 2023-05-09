package SystemCore;

import java.awt.*;
import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import Windows.FileManagerWin;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;

import javax.swing.*;

public class FileManager {
    private static final String file_separator = File.separator;//根据操作系统，a动态的提供分隔符
    private static final String root_path = System.getProperty("user.dir") + file_separator + "File"; // 当前程序所在目录+分隔符+文件名

    public static String current_working_path = file_separator;

    private int block_size;//磁盘块的大小
    private int block_number;//磁盘块的数量
    private int tracks;//磁道数
    private int secs;//扇区数

    private int[] unfillable_block = {3, 6, 9, 17};// 不能使用的编号
    private HashMap<String, int[]> block_dir = new HashMap<>();//文件路径和磁盘使用情况的映射
    private int[] bitmap;//磁盘块的占用情况
    private ArrayList<Block> all_blocks = new ArrayList<>();// 磁盘中所有的块的list

    private Map<String, Object> file_system_tree;// 文件系统树
    //private Disk disk;


    public FileManager(int block_size, int tracks, int secs) {
        this.block_size = block_size;
        this.block_number = tracks * secs;
        this.tracks = tracks;
        this.secs = secs;

        this.all_blocks = initBlocks();

        setUnfillableBlock();
        this.file_system_tree = init_file_system_tree(root_path);
        freeUnfillableBlock();

        //this.disk = new Disk(block_size, tracks, secs);
    }


    public List<String> dss() {
        List<String> result = new ArrayList<>();
        Diary.println("get information of blocks success");

        result.add("当前磁盘块状态为：");
        for (Block block : all_blocks) {
            result.add(block.get_fp() + ": ");
            for (int i : block.get_loc()) {
                result.add(i + " ");
            }
            result.add("\n");
        }
        return result;
    }
    public void change_page(String word) throws IOException {
        //修改System/user第一行为words[1]
        //将路径拼接为完整的文件路径
        String path ="File/user";
        Diary.println("change way of page");
        File file = new File(path);
        //读取文件内容
        String text_content = readFile(file);
        //将文件内容转换为json对象，以便于之后提取content字段
        JSONObject data = JSON.parseObject(text_content);
        //修改mode字段为word
        data.put("mode",word);
        Gson gson = new Gson();
        //将更新后的json对象转换为json字符串
        String jsonData = gson.toJson(data);
        //将更新后的内容写回到文件中
        writeFile(file, jsonData);
    }
    public void change_mem(String word) throws IOException {
        //修改System/user第一行为words[1]
        //将路径拼接为完整的文件路径
        String path ="File/user";
        Diary.println("change way of memory");
        File file = new File(path);
        //读取文件内容
        String text_content = readFile(file);
        //将文件内容转换为json对象，以便于之后提取content字段
        JSONObject data = JSON.parseObject(text_content);
        //修改mem字段为word
        data.put("mem",word);
        Gson gson = new Gson();
        //将更新后的json对象转换为json字符串
        String jsonData = gson.toJson(data);
        //将更新后的内容写回到文件中
        writeFile(file, jsonData);
    }

    private ArrayList<Block> initBlocks() { // 初始化文件块
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < block_number; i++) {
            Block b = new Block(block_size, cal_loc(i));
            blocks.add(b);
        }
        Diary.println("initialize blocks");
        bitmap = new int[block_number];
        Arrays.fill(bitmap, 1);
        return blocks;
    }

    private int[] cal_loc(int block_num) {
        int track = block_num / this.secs;
        int sec = block_num % this.secs;
        int[] result = {track, sec};
        return result;
    }

    private void setUnfillableBlock() {
        for (int i : this.unfillable_block) {
            this.bitmap[i] = 0;
        }
    }

    private void freeUnfillableBlock() {
        for (int i : this.unfillable_block) {
            this.bitmap[i] = 1;
        }
    }

    private Map<String, Object> init_file_system_tree(String now_path) {
        // now_path是当前递归到的绝对路径
        /* 文件树采用Map形式，文件名为键，
         * 当该文件为文件夹时，其值为一个Map，
         * 否则，其值为长度为4的字符串，表示类型 / 读 / 写 / 执行。*/
        File folder = new File(now_path);
        File[] file_list = folder.listFiles();
        Map<String, Object> part_of_tree = new HashMap<String, Object>();  // 当前文件夹对应的Map
        for (File file : file_list) {
            String file_path = file.getAbsolutePath();
            if (file.isDirectory()) {  // 文件夹为键，其值为Map
                part_of_tree.put(file.getName(), init_file_system_tree(file_path));
            } else {
                try (BufferedReader reader = new BufferedReader(new FileReader(file_path))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    String fileContent = stringBuilder.toString();
                    JSONObject data = JSON.parseObject(fileContent); // 读取json字符串，便于之后提取type
                    part_of_tree.put(file.getName(), data.getString("type"));
                    if (this.fill_file_into_blocks(
                            data, file_path.substring(this.root_path.length()), 0) == -1) {  // 将此文件的信息存于外存块中
                        // 没有足够的存储空间
                        Diary.println("block storage error: No Enough Initial Space");
                    }
                } catch (IOException e) {
                    // 处理读取或解析 JSON 异常
                    Diary.println("error: Json exception");
                }
            }
        }
        return part_of_tree;
    }

    private int fill_file_into_blocks(JSONObject f, String fp, int method) {
        int num = Integer.parseInt(f.getString("size")) / block_size;
        int occupy = Integer.parseInt(f.getString("size")) % block_size;
        if (occupy == 0) {
            num = num - 1;//如果正好是磁块的整数个，则不需考虑多余的余数
        }
        int first_free_block = find_free_blocks(num + 1, method);
        if (first_free_block == -1) {
            return -1;
        }
        int free = block_size - occupy;
        // block分配信息存在dir中
        block_dir.put(fp, new int[]{first_free_block, num + 1, Integer.parseInt(f.getString("size"))});

        int count = first_free_block;
        for (int i = 0; i < num + 1; i++) {
            if (i == num) {
                // 最后一块可能有碎片
                all_blocks.get(count).set_free_space(free);
            } else {
                //否则该block被全部占满
                all_blocks.get(count).set_free_space(0);
            }
            bitmap[count] = 0;
            all_blocks.get(count).set_fp(fp);
            count++;
        }
        return 0;
    }

    // 将数组转为字符串
    private String bitmap2str(int[] bm) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bm.length; i++) {
            sb.append(Integer.toString(bm[i]));
        }
        return sb.toString();
    }

    // num:需要的blocks数，此函数用于寻找连续的num个free blocks
    private int find_free_blocks(int num, int method) {
        int[] bm = new int[num];
        Arrays.fill(bm, 1);
        String goal_str = bitmap2str(bm);
        if (method == 0) {
            return block_first_fit(goal_str);
        } else if (method == 1) {
            return block_best_fit(goal_str);
        } else if (method == 2) {
            return block_worst_fit(goal_str);
        } else {
            Diary.println("error: please set a legal free blocks finding method.");
            return -1;
        }
    }

    // first fit文件填充算法，goal_str指需要的连续块（bitmap形式）的字串
    private int block_first_fit(String goal_str) {
        String bitmap_str = this.bitmap2str(bitmap);
        //System.out.println("当前的bitmap：" );
        //for(int i : bitmap) {
        //	System.out.print(i);
        //}
        //System.out.println();
        //System.out.println("目的字符串：" + goal_str);
        int first_free_block = bitmap_str.indexOf(goal_str);
        //System.out.println("匹配到的索引值："+first_free_block);
        return first_free_block;
    }

    // best fit文件填充算法
    private int block_best_fit(String goal_str) {
        int count = 0;
        List<int[]> free_blocks = new ArrayList<int[]>();
        for (int i = 0; i < bitmap.length; i++) {
            int bit = bitmap[i];
            if (bit == 0) {
                count = 0;
                continue;
            } else {
                if (count == 0) {
                    free_blocks.add(new int[]{i, 0});
                }
                count += 1;
                free_blocks.get(free_blocks.size() - 1)[1] = count; //分别记录每一段空闲block的长度
            }
        }
        free_blocks.sort((a, b) -> a[1] - b[1]);// Lambda表达式，将会按照数组元素的第二个值进行升序排序
        for (int[] i : free_blocks) {
            if (i[1] >= goal_str.length()) {
                return i[0];
            }
        }
        return -1; // 没有合适的block被选到，返回-1
    }

    // worst fit文件填充算法
    private int block_worst_fit(String goal_str) {
        int count = 0;
        List<int[]> free_blocks = new ArrayList<>();

        for (int i = 0; i < this.bitmap.length; i++) {
            int bit = this.bitmap[i];

            if (bit == 0) {
                count = 0;
                continue;
            } else {
                if (count == 0) {
                    free_blocks.add(new int[]{i, 0});
                }
                count++;
                free_blocks.get(free_blocks.size() - 1)[1] = count;
            }
        }

        free_blocks.sort((a, b) -> b[1] - a[1]); //按照元素数组第二个元素降序排列
        return free_blocks.get(0)[0]; //直接返回最大连续空间的磁块号
    }

    public Map<String, Object> path2map(String dir_path) {
        if (dir_path.equals("") || dir_path.charAt(0) != File.separatorChar) {
            dir_path = this.current_working_path + dir_path;
        }
        String[] dir_list = dir_path.split(Pattern.quote(File.separator));
        ArrayList<String> dir_list_cleaned = new ArrayList<String>();
        for (String dir : dir_list) {
            if (!dir.equals("")) {
                dir_list_cleaned.add(dir);
            }
        }// 去除由\分割出的空值

        Map<String, Object> dir_dict = this.file_system_tree;
        try {
            ArrayList<Map<String, Object>> upper_dir_dict_stack = new ArrayList<>();
            upper_dir_dict_stack.add(dir_dict);

            for (int i = 0; i < dir_list_cleaned.size(); i++) {
                String dir = dir_list_cleaned.get(i);
                if (dir.equals(".")) {
                    continue;
                } else if (dir.equals("..")) {
                    if (!upper_dir_dict_stack.isEmpty()) {
                        dir_dict = upper_dir_dict_stack.remove(upper_dir_dict_stack.size() - 1);
                    } else {
                        dir_dict = this.file_system_tree;
                    }
                } else {
                    upper_dir_dict_stack.add(dir_dict);
                    dir_dict = (Map<String, Object>) dir_dict.get(dir);

                }
            }
            return dir_dict;
        } catch (Exception e) {
            Diary.println("error: path2dict.");
            return null;
        }
    }

    // 将 "路径" 分割为 该文件所在的目录 和 该文件名, 以字符串数组返回
    public static String[] path_split(String path) {
        // 无视输入时末尾的\，但“\”（根目录）除外
        if (path.length() != 1 && path.charAt(path.length() - 1) == '\\') {
            path = path.substring(0, path.length() - 1);
        }

        // 从最后一个\分割开，前一部分为该文件所在的目录（末尾有\），后一部分为该文件
        String[] pathList = path.split("\\\\");
        String basename = pathList[pathList.length - 1];
        String upperPath = path.substring(0, path.length() - basename.length());
        // 除去“前一部分”末尾的\，但“\”（根目录）除外
        if (upperPath.length() > 1 && upperPath.charAt(upperPath.length() - 1) == '\\') {
            upperPath = upperPath.substring(0, upperPath.length() - 1);
        }
        String[] result = {upperPath, basename};
        return result;
    }

    // 根据路径返回其存储block的位置
    public List<int[]> fp2loc(String fp) {
        // 当fp为相对路径时, 转成绝对路径
        if (fp.charAt(0) != File.separatorChar) {
            fp = current_working_path + fp;
        }
        int[] block_info = block_dir.get(fp);
        int start = block_info[0];
        int length = block_info[1];
        int size = block_info[2];
        List<int[]> loc_list = new ArrayList<int[]>();
        for (int i = start; i < start + length; i++) {
            loc_list.add(all_blocks.get(i).get_loc());
        }
        return loc_list;
    }

    // 在文件块中删除文件
    public void delete_file_from_blocks(String fp) {
        int start = block_dir.get(fp)[0];
        int length = block_dir.get(fp)[1];
        for (int i = start; i < start + length; i++) {
            all_blocks.get(i).set_free_space(block_size);
            all_blocks.get(i).set_fp(null);
            bitmap[i] = 1;
        }
        block_dir.remove(fp);
    }

    // 整理磁盘碎片
    public void tidy_disk() {
        HashMap<String, int[]> block_dir = new HashMap<>(this.block_dir);
        this.all_blocks = this.initBlocks();
        for (Map.Entry<String, int[]> entry : block_dir.entrySet()) {
            String fileContent = "{size :" + entry.getValue()[2] + "}";
            JSONObject data = JSON.parseObject(fileContent);
            this.fill_file_into_blocks(data, entry.getKey(), 0);
        }
        Diary.println("tidy disk complete");
    }

    public JSONObject get_file(String file_path, String mode, String seek_algo) {
        String[] parts = this.path_split(file_path);
        String upper_path = parts[0];
        String basename = parts[1];
        Map<String, Object> current_working_dict = this.path2map(upper_path);
        //异常1.当路径文件夹不存在时, 报错,报错在 path2dict() 中进行
        if (current_working_dict == null) {
            return null;
        } else {
            // 异常2.文件不存在
            if (!current_working_dict.containsKey(basename)) {
                Diary.println("get_file: cannot get file '" + basename + "': file not exist");
                return null;
            }
            //异常3.是文件夹
            Object file_object = current_working_dict.get(basename);
            if (file_object instanceof HashMap<?, ?>) {
                Diary.println("get_file: cannot get file '" + basename + "': dir not a common file");
                return null;
            }
            String gf_path;
            if (file_path.charAt(0) != File.separatorChar) {
                gf_path = this.root_path + this.current_working_path + file_path;
            } else {
                gf_path = this.root_path + file_path;
            }
            /*
             * seek_queen部分
             * */

            try (BufferedReader reader = new BufferedReader(new FileReader(gf_path))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String fileContent = stringBuilder.toString();
                JSONObject data = JSON.parseObject(fileContent);
                return data;
            } catch (IOException e) {
                // 处理读取或解析 JSON 异常
                Diary.println("error: Json exception");
            }
        }
        return null;

    }
    //command: ls
    //method参数，当其为print，则代表原方法，否则为get，返回file_list
    //method == 'get' 用于实现shell的正则表达式匹配功能
    //dir_path为空时,列出当前目录文件; 非空(填相对路径时), 列出目标目录里的文件
    public List<String> ls(String dir_path, String mode, String method) {
        if (dir_path == null)
            dir_path = current_working_path;
        if (mode == null)
            mode = "";
        Diary.println("try commend ls");
        Map<String, Object> current_working_map = path2map(dir_path);
        List<String> result = new ArrayList<String>();
        // 异常1:ls路径出错. 由于path2dict()中已经报错 | 注: 此处偷懒 如果目标存在, 但不是文件夹, 同样报path error
        if (current_working_map == null) {
            // handle exception
            result.add("error: path2dic");
            Diary.println("error: path2dic");

        }
        // ls的对象是一个文件，则只显示该文件的信息
        else if (hasSingleStringEntry(current_working_map)) {
            String file = current_working_map.values().iterator().next().toString();
            String[] path_parts = path_split(dir_path);
            assert path_parts != null;
            String upper_path = path_parts[0];
            String basename = path_parts[1];
            if (file.equals("x")) {
                if (mode.equals("-l") || mode.equals("-al")) {
                    result.add(current_working_map + "\t" + basename);
                } else {
                    result.add(basename);
                }
            }
        }
        // ls的对象是一个文件夹，则显示文件夹内部的信息
        else {
            Set<String> file_set = current_working_map.keySet();
            List<String> file_list = new ArrayList<>(file_set);

            if (method.equals("get")) {
                return file_list;
            }

            // 目录为空时, 直接结束
            if (file_list.size() == 0) {
                Diary.println("file null");
                return null;
            }
            if (!mode.equals("-a") && !mode.equals("-l") && !mode.equals("-al") && !mode.equals("")) {
                result.add("ls: invalid option'" + mode + "', try '-a' / '-l' / '-al'");
                Diary.println("ls: invalid option'" + mode + "', try '-a' / '-l' / '-al'");
                return result;
            }
            for (String file : file_list) {
                // 隐藏文件不显示
                if (file.charAt(0) == '.' && !mode.startsWith("-a")) {
                    continue;
                }
                // 文件夹高亮蓝色显示
                else if (current_working_map.get(file) instanceof Map) {
                    if (mode.equals("-l") || mode.equals("-al")) {
                        result.add("d---" + "\t" + file);
                    } else {
                        result.add(file);
                    }
                }
                // 可执行文件高亮绿色显示
                else if (((String) current_working_map.get(file)).charAt(0) == 'e') {
                    if (mode.equals("-l") || mode.equals("-al")) {
                        result.add(current_working_map.get(file) + "\t" + file);
                    } else {
                        result.add(file);
                    }
                } else {
                    if (mode.equals("-l") || mode.equals("-al")) {
                        result.add(current_working_map.get(file) + "\t" + file);
                    } else {
                        result.add(file);
                    }
                }
            }
        }
        Diary.println("ls success");
        return result;
    }
    public boolean hasSingleStringEntry(Map<String, Object> map) {
        // 如果map大小不为1，直接返回false
        if (map.size() != 1) {
            return false;
        }
        // 遍历map，查看是否只有一个键对，且值为String类型
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof String)) {
                return false;
            }
        }
        return true;
    }
    public String mkdir(String dir_path) throws IOException {
        String result;
        String[] pathParts = path_split(dir_path);
        assert pathParts != null;
        String upper_path = pathParts[0];
        String basename = pathParts[1];
        Map<String, Object> current_working_dict = path2map(upper_path);

        /*for (Map.Entry<String, Object> entry : current_working_dict.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
            Object value = entry.getValue();
            if(!(value instanceof Map<?,?>))
                System.out.println(value.toString());

            // Do something with the key and value
        }

         */
        // 异常1 路径出错
        if (current_working_dict == null) {
            result =  "path error";
        } else {
            // 异常2 文件已存在
            if (current_working_dict.containsKey(basename)) {
                result = ("mkdir: cannot create directory '" +
                        basename +
                        "': File exists");
                if(FileManagerWin.fileInput!=null)
                    FileManagerWin.fileInput.write("failed".getBytes());
            } else {
                String mkdir_path;
                // 相对路径
                if (dir_path.charAt(0) != File.separatorChar) {
                    mkdir_path = root_path + this.current_working_path + dir_path;
                    // 绝对路径
                } else {
                    mkdir_path = root_path + dir_path;
                }
                File newDir = new File(mkdir_path);
                if (newDir.mkdirs()) {
                    current_working_dict.put(basename, new HashMap<String, Object>());
                    result = ("mkdir success");
                    if(FileManagerWin.fileInput!=null)
                        FileManagerWin.fileInput.write("success".getBytes());
                } else {
                    result = ("mkdir failed");
                }
            }
        }
        Diary.println(result);
        return result;
    }
    // command: cd
    public String cd(String dir_path) {
        String result ;
        if (dir_path.equals("")){
            this.current_working_path = file_separator;
            return (this.current_working_path);
        }

        String[] pathParts = path_split(dir_path);
        String upperPath = pathParts[0];
        String basename = pathParts[1];
        Map<String, Object> currentWorkingDict = path2map(upperPath);
        // 异常1: cd路径出错.
        if (currentWorkingDict == null) {
            result = "path error";
        } else {
            // '.'指向自身, 无变化
            if (dir_path.equals(".")) {
                result = this.current_working_path;
                // '..'指向上一级
            } else if (dir_path.equals("..")) {

                String[] parts = this.current_working_path.split(File.separator + File.separator);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    sb.append(parts[i]).append(File.separator);
                }
                this.current_working_path = sb.toString();
                result = (this.current_working_path);

                // 参数为"\(根目录)", 由于根目录无上级目录, 无法完成下一个分支中的操作, 故在这个分支中单独操作.
            } else if (dir_path.equals(File.separator)) {
                this.current_working_path = File.separator;
                result = this.current_working_path;
            } else {
                try {
                    if (basename.equals(".") || basename.equals("..") || currentWorkingDict.get(basename) instanceof Map) {
                        // 相对路径
                        String pathWithPoint;
                        if (!dir_path.startsWith(File.separator)) {
                            // 警告! 未解决异常: 当路径以数个\结尾时, \不会被无视.
                            pathWithPoint = this.current_working_path + dir_path + File.separator;
                        } else {
                            pathWithPoint = dir_path + File.separator;
                        }
                        // 消除..和.
                        String[] dirList = pathWithPoint.split(File.pathSeparator);
                        ArrayList<String> dirs = new ArrayList<String>();
                        for (String dir : dirList) {
                            if (!dir.isEmpty()) {
                                dirs.add(dir);
                            }
                        }

                        int ptr = 0;  // dir_list指针
                        while (ptr < dirs.size()) {
                            // .即自身
                            if (dirs.get(ptr).equals(".")) {
                                dirs.remove(ptr);
                                // ..表示返回上级
                            } else if (dirs.get(ptr).equals("..")) {
                                if (ptr > 0) {
                                    dirs.remove(ptr);
                                    dirs.remove(ptr - 1);
                                    ptr--;
                                    // 当已经到根目录时
                                } else {
                                    dirs.remove(ptr);
                                }
                            } else {
                                ptr++;
                            }
                        }
                        // 组合current_working_path

                        StringBuilder sb = new StringBuilder();
                        for (String dir : dirs) {

                            sb.append(dir);
                        }
                        this.current_working_path = sb.toString();
                        result = (this.current_working_path);
                        // 异常1 文件存在但不是目录
                    } else if (currentWorkingDict.get(basename) == null) {
                        result = ("cd: error " + basename + ": No such dir");
                    } else {
                        result = ("cd: error " + basename + ": Not a dir");
                    }
                    // 异常2 文件不存在
                } catch (Exception e) {
                    result = ("cd: error " + basename + ": No such dir");
                }
            }
        }
        Diary.println(result);
        return result;
    }
    public String rm(String file_path, String mode) {
        if("\\user".equals(current_working_path +file_path)){
            Diary.println("Error: have no right to delete directory /user.");
            return ("Error: have no right to delete directory /user.");
        }
        String[] pathList = path_split(file_path);
        String upperPath = pathList[0];
        String basename = pathList[1];
        Map<String, Object> current_working_dict = this.path2map(upperPath);
        String result = null;
        if (current_working_dict == null) {
            // 路径出错
            result = "path error";
        } else {
            if (mode.startsWith("-r")) {
                // 删文件夹
                if (current_working_dict.containsKey(basename)) {
                    String rmdir_path;
                    if (file_path.charAt(0) != File.separatorChar) {
                        // 相对路径
                        rmdir_path = root_path + this.current_working_path + file_path;
                    } else {
                        // 绝对路径
                        rmdir_path = root_path + file_path;
                    }
                    if (mode.length() == 3 && mode.charAt(2) == 'f') {
                        // 递归地强制删除文件夹
                        Map<String, Object> subDirDict = this.path2map(file_path);
                        for (String subDirName : subDirDict.keySet()) {
                            String subFilePath = file_path + File.separatorChar + subDirName;
                            String realSubFilePath = rmdir_path + File.separatorChar + subDirName;
                            if (subDirDict.get(subDirName) instanceof Map<?, ?> && !((Map<?, ?>) subDirDict.get(subDirName)).isEmpty()) {
                                // 非空的目录, 需要递归删除
                                rm(subFilePath, "-rf");
                            } else if (subDirDict.get(subDirName) instanceof Map<?, ?> && ((Map<?, ?>) subDirDict.get(subDirName)).isEmpty()) {
                                // 空目录, 直接删除
                                new File(realSubFilePath).delete();
                            } else if (subDirDict.get(subDirName) instanceof String) {
                                // 是文件, 强制删除
                                rm(subFilePath, "-f");
                            }
                        }
                        new File(rmdir_path).delete();
                        current_working_dict.remove(basename);
                    } else {
                        // 仅删除空文件夹
                        new File(rmdir_path).delete();
                        current_working_dict.remove(basename);
                    }
                } else {
                    // 目录不存在
                    result = ("rm -r: cannot remove '" + basename + "': No such directory");
                }
            } else if (mode.equals("") || mode.equals("-f")) {
                // 删文件
                try {
                    if (current_working_dict.containsKey(basename)) {
                        String rm_path;
                        if (file_path.charAt(0) != File.separatorChar) {
                            // 相对路径
                            rm_path = this.current_working_path + file_path;
                        } else {
                            // 绝对路径
                            rm_path = file_path;
                        }
                        char temp = (current_working_dict.get(basename)).toString().charAt(2);
                        if (temp == 'w' || mode.equals("-f")) {
                            // 在block中删除文件
                            delete_file_from_blocks(rm_path);
                            rm_path = root_path + rm_path;
                            // 删真正文件
                            File file = new File(rm_path);
                            if (file.delete()) {
                                // 同时修改文件树
                                current_working_dict.remove(basename);
                            } else {
                                result = ("Failed to delete the file: " + basename);
                            }
                        }
                        // 异常1 文件只读, 不可删除
                        else {
                            result = ("rm: cannot remove '" + basename + "': file read only, try to use -f option");
                        }
                    }
                    // 异常2 文件不存在
                    else {
                        result = ("rm: cannot remove '" + basename + "': No such file");
                    }
                }
                // 异常3 文件是目录
                catch (InvalidPathException e) {
                    result = ("rm: cannot remove '" + basename +
                            "': Is a dir. Try to use -r option");
                }

            } else {
                result = ("rm: invalid option'" +
                        mode +
                        "', try '-r' / '-f' / '-rf'");
            }

        }
        if (result == null)
            result = "rm success";
        Diary.println(result);
        return result;
    }
    public String mkf(String file_path, String file_type, String size) throws IOException {
        if (file_type.charAt(0) != 'c') {
            Diary.println("mkf: cannot create file'" + file_path + "': only common file can be created");
            return ("mkf: cannot create file'" + file_path + "': only common file can be created");
        }
        String[] path_split = path_split(file_path);
        String upper_path = path_split[0];
        String basename = path_split[1];
        Map<String, Object> current_working_dict = path2map(upper_path);
        JSONObject json_text = new JSONObject();
        json_text.put("name", file_path);
        json_text.put("type", file_type);
        json_text.put("size", size);
        JSONArray jsonArray = new JSONArray();
        List<String> contents = List.of();
        for (String content: contents)
            jsonArray.add(new JsonPrimitive(content));
        json_text.put("content", jsonArray);

        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting(); // 设置缩进
        Gson gson = gsonBuilder.create();
        String json_data = gson.toJson(json_text);


        // 异常1 路径出错
        if (current_working_dict == null) {
            return "path error";
        } else {
            // 文件名是否已存在
            if (!current_working_dict.containsKey(basename)) {
                // 相对路径 先不与self.root_path相拼接, 为了紧接着的fill_file_into_blocks传参
                String mkf_path;
                if (file_path.charAt(0) != File.separatorChar) {
                    mkf_path = this.current_working_path + file_path;
                } else {
                    mkf_path = file_path;
                }
                if (fill_file_into_blocks(json_text, mkf_path, 2) == -1) {  // 测试是否能装入block
                    Diary.println("mkf: cannot create file'" + basename + "': No enough Space");
                    return ("mkf: cannot create file'" + basename + "': No enough Space");
                }
                mkf_path = root_path + mkf_path;
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(mkf_path));
                    writer.write(json_data);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return "IO error";
                }
                // 同时修改文件树
                current_working_dict.put(basename, file_type);
                Diary.println("mkf success");
                if(FileManagerWin.fileInput!=null)
                    FileManagerWin.fileInput.write("success".getBytes());
                return "mkf success";
                // 异常2 文件已存在
            } else {
                Diary.println("mkf: cannot create file'" + basename + "': file exists");
                if(FileManagerWin.fileInput!=null)
                    FileManagerWin.fileInput.write("failed".getBytes());
                return ("mkf: cannot create file'" + basename + "': file exists");
            }
        }
    }
    public List<String> readContentFromFile(String filePath) {
        filePath = root_path + current_working_path + filePath;
        List<String> result = new ArrayList<String>();
        try {
            // 使用JSONParser解析json文件
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = (JsonObject) parser.parse(new FileReader(filePath));
            // 从jsonObject中获取content对应的JSONArray
            JsonArray contentArray = (JsonArray) jsonObject.get("content");
            // 遍历JSONArray，将双引号内部的内容添加到result中
            for (int i = 0; i < contentArray.size(); i++) {
                String content = contentArray.get(i).toString().replaceAll("^\"|\"$", "");
                result.add(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    /**

     打开一个文件，并进行编辑

     @param path 文件路径
     */
    public String vi(String path) {
        if("\\user".equals(current_working_path + path)){
            Diary.println("Error: have no right to open directory /user.");
            return ("Error: have no right to open directory /user.");
        }
        //将路径拼接为完整的文件路径
        path = root_path + current_working_path + path;
        File file = new File(path);

        //判断文件是否存在且为目录，如果是则报错并返回
        if (file.exists() && file.isDirectory()) {
            Diary.println("Error: Cannot open directory " + path + ".");
            return ("Error: Cannot open directory " + path + ".");
        }
        //判断文件是否为目录，如果是则报错并返回
        if (file.isDirectory()) {
            Diary.println("Error: Directory " + path + " does not exist.");
            return ("Error: Directory " + path + " does not exist.");
        }
        //判断文件是否存在，如果不存在则报错并返回
        if(!file.exists()) {
            Diary.println("Error:" + path + " does not exist.");
            return ("Error:" + path + " does not exist.");
        }



        try {
            //读取文件内容
            String text_content = readFile(file);
            //将文件内容转换为json对象，以便于之后提取content字段
            JSONObject data = JSON.parseObject(text_content);
            String type = data.getString("type");
            if (!type.contains("x"))
                return "This file isn't editable";
            //提取content字段，该字段为一个数组
            JSONArray content = data.getJSONArray("content");
            String res = "";
            //将content数组中的每个元素拼接为一个字符串，用于之后在文本框中显示
            for(int i = 0;i < content.size();i++) {
                res += content.get(i).toString() + "\n";
            }
            if(FileManagerWin.fileInput!=null){
                if(res.equals(""))
                    FileManagerWin.fileInput.write("null".getBytes());
                else
                    FileManagerWin.fileInput.write(res.getBytes());
            }
            //将拼接好的字符串放入JTextArea中
//            JTextArea textArea = new JTextArea(res);
//            JScrollPane scrollPane = new JScrollPane(textArea);
//            scrollPane.setPreferredSize(new Dimension(800, 600));
//            //弹出编辑窗口，选择保存、不保存或取消
//            int option = JOptionPane.showOptionDialog(null, scrollPane,
//                    "Vi - " + path, JOptionPane.YES_NO_CANCEL_OPTION,
//                    JOptionPane.PLAIN_MESSAGE, null,
//                    new String[]{"Save", "Don't Save", "Cancel"}, "Save");
//            //如果选择保存
//            if (option == JOptionPane.YES_OPTION) {
//                //将文本框中的内容分割为字符串数组
//                String[] text = textArea.getText().split("\n");
//                Gson gson = new Gson();
//                //将字符串数组转换为JsonArray对象
//                JsonArray jsonArray = gson.toJsonTree(text).getAsJsonArray();
//                //将JsonArray对象放回原来的json对象中的content字段
//                data.put("content", jsonArray);
//                //更新文件大小字段
//                data.put("size", textArea.getText().length());
//                //将更新后的json对象转换为json字符串
//                String jsonData = gson.toJson(data);
//                //将更新后的内容写回到文件中
//                writeFile(file, jsonData);
//                //接下来进行磁块等的重新分配
//                delete_file_from_blocks(path.substring(this.root_path.length()));
//                fill_file_into_blocks(data,path.substring(this.root_path.length()),1);
//                return "save success";
//            } else if (option == JOptionPane.NO_OPTION) {
//                //如果选择不保存，则不进行任何操作
//            } else if (option == JOptionPane.CANCEL_OPTION) {
//                return "cancel success";
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }
    private static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }

    private static void writeFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    /**

     从指定文件中读取文件信息并返回。

     @param fileName 文件名

     @return 包含文件名、文件大小和文件类型（如果类型包含“e”，则还会包含优先级）的Map类型变量

     @throws IOException 读取文件时可能发生的IO异常
     */
    public Map<String, String> getFileInfo(String fileName) throws IOException {
        // 构造文件路径
        String path = root_path + this.current_working_path + fileName;
        File file = new File(path);
        String jsonStr = "";
        // 读取文件内容到字符串变量
        jsonStr = readFile(file);
        // 将字符串转换为JSON对象
        JSONObject jsonObject = JSON.parseObject(jsonStr);

        // 从JSON对象中获取文件名、文件大小和文件类型
        String name = jsonObject.getString("name");
        String size = jsonObject.getString("size");
        String type = jsonObject.getString("type");


        // 如果文件类型包含“e”，则还需获取优先级信息
        if (type.contains("e")) {
            // 将这些信息存储在Map变量中
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("name", name);
            resultMap.put("size", size);
            resultMap.put("type", type);
            String priority = jsonObject.getString("priority");
            resultMap.put("priority", priority);
            return resultMap;
        }
        else {
            // 将这些信息存储在Map变量中
            Map<String, String> resultMap = null;
            return resultMap;
        }

    }
    public String find(String path){
        //将路径拼接为完整的文件路径
        path = root_path + current_working_path + path;
        File file = new File(path);
        //判断路径是否为目录，如果是则报错并返回

        String result = "";

        //判断文件是否存在，如果不存在则报错并返回
        if(!file.exists()) {
            Diary.println("Error:" + path + " does not exist.");
            result = "error";

        }
        else  if (file.isDirectory()) {
            Diary.println("Error: Directory " + path + " does not exist.");
            return ("error");
        }
        else {
            result=  path;

        }
        return result;

    }
    public void sv(String path,String fileData) throws IOException {
        path = root_path + current_working_path + path;
        File file = new File(path);
        String text_content = readFile(file);
        //将文件内容转换为json对象，以便于之后提取content字段
        JSONObject data = JSON.parseObject(text_content);
        String[] text = fileData.split("\n");
        Gson gson = new Gson();
        //将字符串数组转换为JsonArray对象
        JsonArray jsonArray = gson.toJsonTree(text).getAsJsonArray();
        //将JsonArray对象放回原来的json对象中的content字段
        data.put("content", jsonArray);
        //更新文件大小字段
        data.put("size", fileData.length());
        //将更新后的json对象转换为json字符串
        String jsonData = gson.toJson(data);
        //将更新后的内容写回到文件中
        writeFile(file, jsonData);
        //接下来进行磁块等的重新分配
        delete_file_from_blocks(path.substring(this.root_path.length()));
        fill_file_into_blocks(data,path.substring(this.root_path.length()),1);
        Diary.println("sv success");
    }
}