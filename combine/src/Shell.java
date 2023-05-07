import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Shell extends JFrame implements ActionListener, KeyListener, MouseListener,Runnable  {
    private static JTextArea terminalOutput;
    private JTextField terminalInput;
    static String[] keyword ={"gcc","vi","re","ls","cd","mkdir","mon","rm","dss","exec","dms","td","mkf","kill","ps","rs","man","mc"};
    static int Level=0;//一级权限等级,只能识别基础命令
    private static PipedOutputStream kernelInput;
    private PipedInputStream kernelOutput;
    private static PipedOutputStream kernelInput1;
    private PipedInputStream kernelOutput1;


    public Shell(PipedOutputStream kernelInput, PipedInputStream kernelOutput,PipedOutputStream kernelInput1, PipedInputStream kernelOutput1) throws IOException {
        super("Shell");
        this.kernelInput = kernelInput;
        this.kernelOutput=kernelOutput;
        this.kernelInput1=kernelInput1;
        this.kernelOutput1=kernelOutput1;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create terminal output area
        terminalOutput = new JTextArea(20, 50);
        terminalOutput.setEditable(false);
        terminalOutput.addMouseListener(this);
        JScrollPane scrollPane = new JScrollPane(terminalOutput);

        // Create terminal input area
        terminalInput = new JTextField(50);
        terminalInput.addActionListener(this);
        terminalInput.addKeyListener(this);
        terminalInput.addMouseListener(this);

        // Add components to GUI
        add(scrollPane, BorderLayout.CENTER);
        add(terminalInput, BorderLayout.SOUTH);

        pack();
        setVisible(true);

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        String input = terminalInput.getText();
        terminalInput.setText("");
        terminalOutput.append("$ " + input + "\n");
        if (input.equals("clear")) {
            terminalOutput.setText("");
            return;
        }

        String[] words = input.split("\\|");
        //打印words
        for(int i=0;i<words.length;i++){
            System.out.println("a:"+words[i]);
        }
        for (int i = 0; i < words.length; i++) {
            try {
                communication(words[i]);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        if (input.equals("exit")) {
            System.exit(0);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
            try {
                communication("stop");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            terminalOutput.append("命令终止\n");
            // Add logic to terminate running program here
        }
        if (e.getKeyCode() == KeyEvent.VK_U && e.isControlDown()) {

            terminalInput.setText("");
            // Add logic to terminate running program here
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    int pos = terminalInput.getCaretPosition();
                    terminalInput.getDocument().insertString(pos, text, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            String selectedText = terminalOutput.getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                StringSelection selection = new StringSelection(selectedText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public static void communication(String word) throws IOException {
        //删除字符串前后空格
        word=word.trim();
        if(word.equals("stop")){
            return;
        }
        if(Check(word)==0 && Level==0){
            terminalOutput.append("非法输入\n");
        }
        else{
            System.out.println("s:"+word);
            //开启写管道
            kernelInput.write(word.getBytes());
            kernelInput.flush();
        }
    }
    public static int Check(String word) {
        //分割字符串
        String[] words = word.split(" ");
        //遍历words数组
        for (int i = 0; i < keyword.length; i++) {
            if (words[0].equals(keyword[i]) ) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void run() {
        try {

            // Read data from shell's output and process it
            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = kernelOutput1.read(buffer)) != -1) {
                String data = new String(buffer, 0, bytesRead);
               //打印data
                System.out.println(data);
                terminalOutput.append(data + "\n");
                // Process the data as needed
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
}
}