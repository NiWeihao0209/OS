package SystemCore;

public class Block {
    private int total_space;
    private int free_space;
    private String fp; //块中的文件
    private int[] loc; // 块的位置,用track和sec来锁定

    public Block(int total_space, int[] loc) {
        this.total_space = total_space;
        this.free_space = total_space;
        this.fp = null;
        this.loc = loc;
    }

    public void set_free_space(int fs) {
        this.free_space = fs;
    }

    public int get_free_space() {
        return this.free_space;
    }

    public void set_fp(String fp) {
        this.fp = fp;
    }

    public String get_fp() {
        return this.fp;
    }

    public int[] get_loc() {
        return this.loc;
    }
}
