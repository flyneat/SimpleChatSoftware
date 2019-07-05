package com.example.socketcommdemo.client;

import com.example.socketcommdemo.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;


public class Client extends Socket {
    public static final int REMOTE_SERVER_SHUTDOWN = -2;
    public static final int SOCKET_CONNECT_FAIL = -1;
    public static final int SOCKET_CONNECTE_SUCC = 0;
    public static final int RECEIVE_NEW_MSG = 1;


    private PrintWriter mOut;
    private BufferedReader mIn;

    public Client(String host, int port) throws IOException {
        super(host, port);
        mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(getOutputStream())), true);
        mIn = new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    public Client() throws IOException {
        super();
    }

    public boolean connectServer(InetSocketAddress address, int timeout) throws IOException {
        connect(address, timeout);
        boolean ret = isConnected();
        if (ret) {
            mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(getOutputStream())), true);
            mIn = new BufferedReader(new InputStreamReader(this.getInputStream()));
        }
        return ret;
    }


    public void sendMsg(final String msg) {
        ThreadUtils.getInstance().run(new Runnable() {
            @Override
            public void run() {
                mOut.println(msg);
            }
        });
    }

    public String receiveMsg() throws IOException {
//        byte[] msgs = mIn.readLine().getBytes();
//        return new String(msgs, "GBK");
        return mIn.readLine();
    }
}
