package tracker;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Client1 extends JFrame implements Runnable


{
    public void run() 
    {
        try {
            read(str);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private static String str=null;
    private static Socket socket=null;
    private static OutputStream outMessage=null;
    public  static Map<String, String>ipList=new HashMap<String,String>();
    public  static String[] fileContent=new String[48];
    public static String center_ip;
    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException 
    {

        Client1 one = new Client1(1);

        one.addWindowListener(new WindowAdapter() {  

            public void windowClosing(WindowEvent e) {  
                super.windowClosing(e); 

                System.exit(0);
             }  

            });

    }
    JPanel p1 = new JPanel();
    JPanel p2 = new JPanel();

    JLabel l = new JLabel("请输入中心节点IP:");

    JTextField f = new JTextField();
    JButton b = new JButton("跟踪");
    public Client1()
    {

    }
    public Client1(int x) //构造方法
    {
        this.setLayout(new BorderLayout());
        p1.setLayout(new GridLayout(3, 2));
        p1.add(l);
        p1.add(f);

        this.add(p1, BorderLayout.NORTH);
        p2.add(b, BorderLayout.CENTER);
        this.add(p2, BorderLayout.CENTER);

        this.setSize(200, 150);
        this.setVisible(true);
        this.setLocation(600, 300);
        this.setResizable(false);


        b.addActionListener(
            new ActionListener() 
            {
                public void actionPerformed(ActionEvent e) 
            {
                str =f.getText();
                new Thread(new Client1()).start();

         }
     });    
    }

    public void read(String str) throws UnknownHostException, IOException, InterruptedException{

        this.socket=new Socket(str,30005);
        //OutputStream outMessage=null;

        this.outMessage=socket.getOutputStream();
        new Thread(new Client1Thread(socket)).start();
        while(true)
        {
            //-------------------------获取配置文件信息---------------
            /*
             * 0：本机名
             * 1：本机id
             * 2：中心节点id
             * 3：中心节点ip
             * 4：中心启动模式 ？2：1
             */
            String[] iniMessage=readUserFile();
            //String[] tempID=iniMessage[4]
            iniMessage[4]=iniMessage[4].substring(iniMessage[4].length()-1, iniMessage[4].length());
            //获取 中心ID
            String[] tempSplit=iniMessage[3].split("=");
            iniMessage[3]=tempSplit[1];
            tempSplit=iniMessage[2].split("=");
            iniMessage[2]=tempSplit[1];

            //-------------把主机ID放入输出流---------------
            byte[] message =new byte[100];
            //-------------处理节点ID名，
            //System.out.println(iniMessage[4]);
            if (!iniMessage[4].equals("2"))//如果是节点启动
            {
                iniMessage[1]+="\n";
                message=iniMessage[1].getBytes();
                //System.out.println("mode="+iniMessage[4]);
                //System.out.println(iniMessage[1]);
            }
            else//如果是中心启动
            {
                iniMessage[1]="center "+iniMessage[1];
                iniMessage[1]+="\n";
                message=iniMessage[1].getBytes();
            }
            //while(true)
            //{
                outMessage.write(message);
                Thread.sleep(1000); 
                modifyUser();
            //}
        }
    }

    //------------------------------修改配置文件的 信息--------------------------
        public static void modifyUser() throws IOException
        {
//          System.out.println("modify前所有的节点信心");
//          for (String temps : Client1.ipList.keySet()) {
//              System.out.println(temps+":"+Client1.ipList.get(temps));
//          }
//          System.out.println("--------------------------");
            //1、获取ipList中 中心节点的 ID 和 IP
            String id=null;//格式："4"
            String ip=null;//格式："199.1.1.1"
            for (String pointName : Client1.ipList.keySet()) //获取 ipList表中的中心节点的 ID和IP
            {
                String[] isCenter=pointName.split(" ");
                if (isCenter.length==2)//如果是中心节点  1  isCenter[1]=nodeid=3
                {
                    String[] IdSplit=isCenter[1].split("=");
                    id=IdSplit[1];//3
                    ip=Client1.ipList.get(pointName);
                    break;
                }
            }
            //System.out.println("modify");
            if (id!=null && ip!=null) //如果有中心节点
            {
                String[] idFromUser=fileContent[19].split("=");//对来自User的 中心节点ID进行切割 切成（linkid，4）
                String[] ipFromUser=fileContent[21].split("=");//对来自User的 中心节点IP进行切割 切成（localip，192.1.1.1）

                if (!idFromUser[1].equals(id) || !ipFromUser[1].equals(ip))//且配置文件的  信息与ipList表的中心节点信息 不同
                {
                    fileContent[19]="linkid="+id;//更新中心节点的 id和IP信息
                    fileContent[21]=ipFromUser[0]+"="+ip;

                    File user1=new File("/Users/XXY/Desktop/user.ini");//写入配置文件中
                    BufferedWriter userBW=new BufferedWriter(new FileWriter(user1, false));

                    for (int i = 0; i < 47; i++)
                    {
                        //System.out.println(fileContent[i]);

                        userBW.write(fileContent[i]);
                        userBW.newLine();
                    }
                    userBW.close();
                }
            }

        }
    //--------------------------------读取配置文件关键信息--------------------------------------
    public static String[]  readUserFile() throws IOException
    {
        //-----------------------读取配置文件信息-----------------
        File user=new File("/Users/XXY/Desktop/user.ini");
        BufferedReader fileBR=new BufferedReader(new InputStreamReader(new FileInputStream(user)));
        String fileLine=null;
        int i=1,j=0;
        String[] iniMessage=new String[5];

        while( (fileLine=fileBR.readLine())!=null )
        {
            fileContent[i-1]=fileLine;
            if(i==2)
                iniMessage[j++]=fileLine;
            if(i==3)
                iniMessage[j++]=fileLine;
            if(i==20)
                iniMessage[j++]=fileLine;
            if(i==22)
                iniMessage[j++]=fileLine;
            if(i==27)
                iniMessage[j++]=fileLine;
            i++;
        }
        fileBR.close();
        //System.out.println(iniMessage[4]);
        return iniMessage;
    }
}
//--------------------------------客户端线程类--------------------------------------

class Client1Thread implements Runnable //客户端线程 接收 服务器发来的最新IP地址
{
    private Socket socket=null;//与客户端连接的Socket
    BufferedReader br=null;    //此socket的输入流

    //----------------------------客户端线程构造函数-----------------------------------
    public Client1Thread(Socket socket) throws IOException 
    {
        this.setSocket(socket);
        br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    //----------------------------线程体-------------------------------------------
    public void run() 
    {   
        try 
        {
            String user=null;
            
            while( (user=br.readLine())!=null )
            {
                //System.out.println(user);
                String[] singleUser=user.split(":");//分割成（ID、IP）  
                //ID全称存在 singleUser[0]中，然后对  singleUser[0]进行空格切割
                String[] isCenter= singleUser[0].split(" ");

                if (isCenter.length==1) // 新来的边缘节点 如果是原来的中心节点 便要删除原来这个中心节点
                {
                		
                    for (String stringD : Client1.ipList.keySet())
                    {
                        String[] tempName=stringD.split(" ");
                        if (tempName.length==2) 
                        {
                            if (tempName[1].equals(isCenter[0]))
                            {
                                Client1.ipList.remove(stringD);
                                break;
                            }
                        }
                    }
                }
                if (isCenter.length==2) // 新来的中心节点 如果是原来的边缘  便要删除原来这个边缘节点
                {
                    for (String stringD : Client1.ipList.keySet())
                    {
                        if (stringD.equals(isCenter[1])) {
                            Client1.ipList.remove(stringD);
                            break;
                        }
                    }
                }               
                Client1.ipList.put(singleUser[0], singleUser[1]);//（用户名，IP地址）存入Map中

                //-----------------------------控制台输出最新的IP表--------------------
                System.out.println("所有节点信息如下");
                System.out.println(Client1.ipList.isEmpty());
                for (String temp : Client1.ipList.keySet())
                {
                    System.out.println(temp+":"+Client1.ipList.get(temp));
                }
                //modifyUser();
            }       
        } 
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
    public Socket getSocket() {
        return socket;
    }
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}