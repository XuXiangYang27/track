package tracker;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Server 

{
	//（节点名，IP）键值对
	public static Map<String, String > ipList=new Hashtable<>();
	//socket集
	public static List<Socket> socketList=Collections.synchronizedList(new ArrayList<>());	
	/*
	 * 界面相关的成员
	 */
	public static Frame frame=null;
	public static Panel p1=null;
	public static ScrollPane sp=null;	
	public Server() throws UnknownHostException 
	{
		InetAddress addr = InetAddress.getLocalHost();
		String ip1=addr.getHostAddress();//获得本机IP
		System.out.println(ip1);
		frame=new Frame("节点跟踪器"+ip1);
		
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				System.exit(0);
			}
		});
		
		frame.add(new Button("节点信息"), BorderLayout.NORTH);	
		//--------frame.add(p1);
		p1=new Panel();
		p1.setLayout(new GridLayout(20, 3,0,0));
		
		p1.add(new Button("节点类型"));
		p1.add(new Button("ID"));
		p1.add(new Button("IP"));
		sp=new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
			
		for(int i=3;i<60;i++)
		{
			Button bbb=new Button("");
			p1.add(bbb);
		}
		sp.add(p1);
		frame.add(sp, BorderLayout.CENTER);
		frame.setBounds(50, 50, 600, 400);
		frame.setVisible(true);
		//frame.setResizable(false);
	}
	public static void main(String[] args) throws IOException 
	{
		new Server();				
		ServerSocket server=new ServerSocket(30005);
		Socket socket=null;
		while(true)
		{
			socket=server.accept();//监听请求；
			//--------------------新建一个服务器线程满足新连接的客户---------------------
			ServerThread serverThread=new ServerThread(socket);
			Thread thread=new Thread(serverThread);
			thread.start();
		}
		
	}
}
//----------------------------------------服务器线程类----------------------------------
class ServerThread implements Runnable
{

	private Socket socket=null;//和本线程相关的Socket
	private BufferedReader br=null;//该socket对应的输入流
	private OutputStream os=null;//该socket对应的输出流
	
	
	//----------------------------服务器线程-构造函数，获取socket和其输入流--------------
	public ServerThread(Socket socket) throws IOException 
	{
		this.socket=socket;
		br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	//----------------------------线程体，获取客户端发来的信息--------------------------
	public void run() 
	{	
		//存储 节点名
		String info=null;
		
		//节点IP地址
		InetAddress point=socket.getInetAddress();
		String pointIp=point.getHostAddress();
		
		//加入新的socket
		Server.socketList.add(socket);
		
		try 
		{
			while( (info=br.readLine())!=null )//循环读取 客户端（客户端每隔一秒发送一次）发来的信息
			{
				for(Socket socket:Server.socketList)
				{
					os=socket.getOutputStream();
					String massage=info+":"+pointIp+"\n";
					byte[] byteMesage=massage.getBytes();
					os.write(byteMesage);
				}
				
				String[] isCenter=info.split(" ");
				
				//System.out.println("->"+info+":"+point.getHostAddress());//前台显示收到的数据
				
				//Server.socketList.remove(Server.socketList.indexOf(socket));
				
				//如果  节点IP发生变化 or 节点第一次连接服务器
				if (  !point.getHostAddress().equals(Server.ipList.get(info))  || !Server.ipList.containsKey(info) 
					      )
				{
					if (isCenter.length==1) //当中心节点变成 边缘节点时，删除原来的中心节点
					{
						for (String stringD : Server.ipList.keySet()) 
						{
							String[] tempName=stringD.split(" ");
							if (tempName.length==2) {
								if (tempName[1].equals(info)) {
									Server.ipList.remove(stringD);
									break;
								}
							}
						}
					}
					if (isCenter.length==2) //当边缘节点变成 中心节点时，删除原来的边缘节点
					{
						for (String stringD : Server.ipList.keySet()) 
						{
							if (isCenter[1].equals(stringD)) {
								Server.ipList.remove(stringD);
								break;
							}
						}
					}
					//1、更新ip地址
					Server.ipList.put(info, pointIp);
					//控制台输出最新的节点信息列表
					System.out.println("最新的节点信息如下：");
					for (String temp1 : Server.ipList.keySet()) 
					{
						System.out.println(temp1+":"+Server.ipList.get(temp1));
					}
					
					//更新到UI
					Server.p1.removeAll();
					Server.sp.removeAll();
					if (Server.ipList.size()>19) {
						Server.p1.setLayout(new GridLayout(1+Server.ipList.size(), 3,0,0));
					}
					else
						Server.p1.setLayout(new GridLayout(20, 3,0,0));
					
					Server.p1.add(new Button("节点类型"));
					Server.p1.add(new Button("ID"));
					Server.p1.add(new Button("IP"));
					for (String temp1 : Server.ipList.keySet()) 
					{
						String[] isCenter1=temp1.split(" ");
						String id;
						if (isCenter1.length==2)
						{
							id=isCenter1[1].split("=")[1];
							Server.p1.add(new Button("中心"));
							Server.p1.add(new Button(id));
							Server.p1.add(new Button(Server.ipList.get(temp1)));
						}
						else
						{
							id=isCenter1[0].split("=")[1];
							Server.p1.add(new Button("边缘"));
							Server.p1.add(new Button(id));
							Server.p1.add(new Button(Server.ipList.get(temp1)));
						}
					}
					if (Server.ipList.size()<=19) 
					{
						for ( int i =3*Server.ipList.size()+3; i < 60; i++) {
							Server.p1.add(new Button(""));
						}
					}
					Server.sp.add(Server.p1);
					Server.frame.add(Server.sp,BorderLayout.CENTER);
					Server.frame.pack();									
				}				
			}
		} 
		catch (IOException e)//如果读取失败，去掉socket 和 IP
		{
			try 
			{
				String closedinfo = null;//存储掉线的 节点id，ip信息
				//找出异常socket的（id,ip）信息
				for (String temp : Server.ipList.keySet())
				{
					if (Server.ipList.get(temp).equals(pointIp))
					{
						String[] isCenter=temp.split(" ");
						if (isCenter.length==2) 
						{
							String nodeid=isCenter[1];
							closedinfo=nodeid+":"+pointIp+"\n";
						}
						break;
					}
				}
				
				String closedSocketIp=new String(pointIp);//连接异常的socket的IP
				for (Socket temp : Server.socketList)//通知各节点删除异常的socket连接
				{
					if (temp!=socket)
					{
						byte[] infomation=closedinfo.getBytes();
						
						os=temp.getOutputStream();
						os.write(infomation);
					}
				}
				socket.close();
			} catch (IOException e1) 
			{
				e1.printStackTrace();
			}

			Server.socketList.remove(socket);
			System.out.println(Server.socketList.isEmpty());
			
			for (String temp : Server.ipList.keySet()) 
			{
				
				if (Server.ipList.get(temp).equals(pointIp))
				{
					Server.ipList.remove(temp);
					break;
				}
			}
			System.out.println("move后的IP列表");
			for (String rname : Server.ipList.keySet()) 
			{
				System.out.println(rname+":"+Server.ipList.get(rname));
			}
			//更新到UI
			Server.p1.removeAll();
			Server.sp.removeAll();
			if (Server.ipList.size()>19) {
				Server.p1.setLayout(new GridLayout(1+Server.ipList.size(), 3,0,0));
			}
			else
				Server.p1.setLayout(new GridLayout(20, 3,0,0));
			
			Server.p1.add(new Button("节点类型"));
			Server.p1.add(new Button("ID"));
			Server.p1.add(new Button("IP"));
			for (String temp1 : Server.ipList.keySet()) 
			{
				String[] isCenter1=temp1.split(" ");
				String id;
				if (isCenter1.length==2)
				{
					id=isCenter1[1].split("=")[1];
					Server.p1.add(new Button("中心"));
					Server.p1.add(new Button(id));
					Server.p1.add(new Button(Server.ipList.get(temp1)));
				}
				else
				{
					id=isCenter1[0].split("=")[1];
					Server.p1.add(new Button("边缘"));
					Server.p1.add(new Button(id));
					Server.p1.add(new Button(Server.ipList.get(temp1)));
				}
			}
			if (Server.ipList.size()<=19) 
			{
				for ( int i =3*Server.ipList.size()+3; i < 60; i++) {
					Server.p1.add(new Button(""));
				}
			}
			Server.sp.add(Server.p1);
			Server.frame.add(Server.sp,BorderLayout.CENTER);
			Server.frame.pack();		
		}	
	}
	private String readFromClient()
	{
		try 
		{
			return br.readLine();
		}
		catch (IOException e) //如果捕获到异常，则表明该socket对应的客户端已经关闭
		{
			Server.socketList.remove(socket);
		}
		return null;
	}
}