package SystemCore;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UsingFrameBar{
    List<Integer> usingFrameList;
    public UsingFrameBar(LinkedList queue){
        usingFrameList= new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            usingFrameList.add((Integer) queue.get(i));
        }
        for (int i = queue.size(); i < 16; i++) {
            usingFrameList.add(-1);
        }
    }
    public String getPage(int i){
        if(usingFrameList.
                get(i) == -1)
            return "N";
        else
            return usingFrameList.get(i).toString();
    }
}