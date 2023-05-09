package SystemCore;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Diary {
    public static void println(String str)  {
        try {
            System.out.println(str + "\n");
            FileWriter fw = new FileWriter("System/diary", true);
            //获取当前时间
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            str = time + ":" + str + "\n";
            //向System/diary文件中附加str
            fw.write(str);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
