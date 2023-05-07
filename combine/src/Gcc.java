import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Gcc {
    //格式检验
    public static Boolean check(List<String> res){
        //遍历res中的每个元素
        for(int i=0;i<res.size();i++){
            //将每个元素按照空格分割，存入数组中
            String[] str = res.get(i).split(" ");
            //判断数组中的元素个数是否为3
            if(str.length==1){
                if(str[0].equals("fork")){

                }
                else{
                    return false;
                }
            }
           else if(str.length==2){
               //判断str[1]是否为int类型
                if(str[0].equals("cpu") || str[0].equals("access") || str[0].equals("printer") ){
                     if(str[1].matches("[0-9]+")){

                     }
                     else{
                          return false;
                     }
                }
                else if( str[0].equals("block") && str.length<=100){
                    //把str[1]按空格分割，存入数组中
                    String[] str1 = str[1].split(" ");
                    //判断str1中的元素是否都为int类型
                    for(int j=0;j<str1.length;j++){
                        if(str1[j].matches("[0-9]+")){

                        }
                        else{
                            return false;
                        }
                    }

                }

                else{
                     return false;
                }

            }
           else{
               return false;
            }

        }
        return true;


    }

    //建立一个新的字符串数组
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
    //编译
    public static Boolean compile(String prioity,String list) throws IOException {
        //将路径拼接为完整的文件路径
        String path = list;
        File file = new File(path);
        //读取文件内容
        String text_content = readFile(file);
        //将文件内容转换为json对象，以便于之后提取content字段
        JSONObject data = JSON.parseObject(text_content);
        //提取content字段，该字段为一个数组
        JSONArray content = data.getJSONArray("content");
        List<String> res = new ArrayList<>();
        //将content数组中的每个元素拼接为一个字符串，用于之后在文本框中显示
        for (int i = 0; i < content.size(); i++) {
            res.add(content.get(i).toString().trim());
        }
        if (!check(res)) {
            return false;
        }
        data.put("type", "erwx");
        data.put("priority", prioity);
        Gson gson = new Gson();
        //将更新后的json对象转换为json字符串
        String jsonData = gson.toJson(data);
        //将更新后的内容写回到文件中
        writeFile(file, jsonData);
        return true;

    }

}





