import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.SimpleFormatter;

import utils.ThreadUtils;

public class Server {
	private static CopyOnWriteArrayList<Socket> mClients;

	public static void main(String[] args) {
		mClients = new CopyOnWriteArrayList<>();
		startServer();
	}

	public static void startServer() {
		ThreadUtils.getInstance().execute(start);
	}

	private static Runnable start = new Runnable() {
		@Override
		public void run() {
			System.out.println("开始启动server...");
			ServerSocket serverSocket = null;
			int tryCount = 0;
			boolean isSucc = false;
			while (!isSucc && tryCount < 3) {
				try {
					serverSocket = new ServerSocket(8688);
					System.out.println("serverSocket:" + serverSocket);
					isSucc = true;
				} catch (IOException e) {
					tryCount++;
					System.out.println("start tcp server failed, port:8688, fail " + tryCount + " times, try start again...");
					e.printStackTrace();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
			if (!isSucc) {
				System.out.println("启动tcp server 失败，请稍后重试");
				return;
			}
			System.out.println("开始监听客户端请求...");
			while (serverSocket.isBound()) {
				try {
					// 等待客户端请求，accept()是一个阻塞方法
					final Socket client = serverSocket.accept();
					mClients.add(client);
					System.out.println("accept" + client.toString());
					responseClient(client);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	};

	private static void responseClient(final Socket client) {
		ThreadUtils.getInstance().execute(new Runnable() {
			@Override
			public void run() {
				try {
					// 用于接收客户端的消息
					BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
					// 用于向客户端发送消息
					PrintWriter out = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
					out.println("欢迎来到聊天室");
					StringBuilder sb = new StringBuilder();
					while (true) {
						String line = in.readLine();
						if (line == null) {
							break;
						}
						sb.append(line).append("\n");
						// 一个ready()方法搞定读取多行文本信息
						if (!in.ready()) {
							System.out.println("receive msg from " + client.getRemoteSocketAddress());
							String transMsg = sb.toString().trim();
							System.out.println("receive msg：" + sb.toString().trim());
							sb.setLength(0);
							transmitMsg(client, transMsg);
							// System.out.print(",");
							// int index = new Random().nextInt(mResponseMsgs.length);
							// String msg = mResponseMsgs[index];
							// out.println(msg);
							// System.out.println("send msg:" + msg);
							// System.out.println("******************");
						}
					}
					System.out.println("client quit");
					mClients.remove(client);
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});
	}

	private static void transmitMsg(final Socket client, final String transMsg) {
		ThreadUtils.getInstance().execute(new Runnable() {
			@Override
			public void run() {
				for (Socket otherClient : mClients) {
					if (otherClient != null && otherClient != client) {
						// 用于向客户端发送消息
						try {
							PrintWriter out = new PrintWriter(
									new BufferedWriter(new OutputStreamWriter(otherClient.getOutputStream())), true);
							String host = otherClient.getInetAddress().getHostAddress();
							out.println("[" + host + "]" + transMsg);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});

	}
}
