package com.example.socketcommdemo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.socketcommdemo.client.Client;
import com.example.socketcommdemo.utils.InputUtils;
import com.example.socketcommdemo.utils.ThreadUtils;
import com.example.socketcommdemo.utils.ToastUtils;
import com.moranc.utils.LogUtils;
import com.moranc.utils.LogcatHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivity";
    ScrollView svContainer;
    LinearLayout llMsgLayout;
    EditText etMsgInput;
    Button btnMsgSend;

    private Client mClient;
    /**
     * activity的destroy状态标记
     */
    private volatile boolean mActivityDestroy = false;

    private String ip;
    private int port;

    private Runnable connectServer = new Runnable() {
        @Override
        public void run() {
            boolean connSucc = false;
            int connCount = 0;
            InetSocketAddress address = new InetSocketAddress(ip, port);
            LogUtils.d("wait for connect server: " + address.getHostString());
            while (!connSucc && connCount < 3 && !mActivityDestroy) {
                try {
                    // 创建一个socket对象，并连接服务器
                    mClient = new Client();
                    connSucc = mClient.connectServer(address, 10 * 1000);
                    if (connSucc) {
                        if (mHandler != null) {
                            mHandler.sendEmptyMessage(Client.SOCKET_CONNECTE_SUCC);
                        }
                    }
                } catch (IOException e) {
                    connCount++;
                    SystemClock.sleep(1000);
                    Log.d(TAG, String.format("connect tcp server failed, failTime = %d, retry...", connCount));
                }
            }
            if (!connSucc) {
                Log.e(TAG, "cannot connect tcp server!");
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(Client.SOCKET_CONNECT_FAIL);
                }
                return;
            }
            while (!mActivityDestroy) {
                // 循环接收服务器响应消息
                if (!receiveMsg()) {
                    // 如果接收消息失败，关闭当前Socket，线程运行结束
                    closeSocket();
                    return;
                }
            }
        }
    };


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case Client.SOCKET_CONNECTE_SUCC:
                    ToastUtils.show("tip:服务器连接成功!");
                    break;
                case Client.RECEIVE_NEW_MSG:
                    String msg = (String) message.obj;
                    System.out.println("receive message: " + msg);
                    msg = "server" + formatTime(new Date()) + "\n" + " " + msg;
                    showMsg(msg, 2);
                    break;
                case Client.SOCKET_CONNECT_FAIL:
                    ToastUtils.showLong("tip:无法连接服务器，请重新启动应用尝试连接");
                    break;
                case Client.REMOTE_SERVER_SHUTDOWN:
                    Snackbar.make(btnMsgSend, "Warning:远程服务已断开", Snackbar.LENGTH_INDEFINITE)
                            .setAction("确认", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // 重新连接服务器
                                    Log.d(TAG, "开始重新连接服务器...");
                                    ThreadUtils.getInstance().run(connectServer);
                                }
                            }).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_input_server_address) {
            LogUtils.d("输入服务器地址");
            View contentView = View.inflate(this, R.layout.input_server_address, null);
            final EditText etHostName = contentView.findViewById(R.id.et_input_hostname);
            final EditText etPort = contentView.findViewById(R.id.et_input_port);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("配置参数")
                    .setView(contentView)
                    .setCancelable(true)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ip = etHostName.getText().toString().trim();
                            String strPort = etPort.getText().toString().trim();
                            if (TextUtils.isEmpty(strPort)) {
                                port = -1;
                            }
                            port = Integer.valueOf(strPort);
                            // 开始连接服务器
                            ThreadUtils.getInstance().run(connectServer);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            LogUtils.d("取消配置服务器参数");
                        }
                    });
            builder.create().show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityDestroy = true;
        mHandler.removeCallbacksAndMessages(null);
        if (mClient != null) {
            try {
                mClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ThreadUtils.getInstance().shutDown();
        LogcatHelper.getInstance().stop();
        mHandler = null;

    }

    private void init() {
        LogUtils.configPrint(LogUtils.LEVEL_D);
        String logPath = getExternalFilesDir(null).getAbsolutePath() + "/"
                + getPackageName() + "/log";
        LogcatHelper.getInstance().start(logPath);
        initData();
        initView();
        initListener();
    }

    private void initData() {
        ip = "";
        port = 0;
    }

    private void initView() {
        svContainer = findViewById(R.id.sv_container);
        llMsgLayout = findViewById(R.id.ll_self_msg_container);
        etMsgInput = findViewById(R.id.et_input_msg);
        btnMsgSend = findViewById(R.id.btn_send_msg);
    }


    private void initListener() {
        btnMsgSend.setOnClickListener(this);

        svContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    InputUtils.hideInputMethod(etMsgInput);
                }
                return false;
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send_msg:
                doSendMsg();
                break;
            default:
                break;
        }
    }


    private void doSendMsg() {
        String msg = etMsgInput.getText().toString();
        etMsgInput.setText("");
        if (TextUtils.isEmpty(msg)) {
            ToastUtils.show("tip：发送消息不能为空");
            return;
        }
        if (!isConnected()) {
            ToastUtils.showLong("发送消息失败，原因：未连接服务器");
            return;
        }
        showMsg("self" + formatTime(new Date()) + "\n" + " " + msg);
        sendMsg(msg);
    }

    private void sendMsg(final String msg) {
        LogUtils.d("send message: " + msg);
        mClient.sendMsg(msg);
    }

    private boolean receiveMsg() {
        try {
            String msg = mClient.receiveMsg();
            if (msg == null) {
                return false;
            }
            LogUtils.d("receive msg:" + msg);
            mHandler.obtainMessage(Client.RECEIVE_NEW_MSG, msg).sendToTarget();
            return true;
        } catch (Exception e) {
            Log.d(TAG, "接收消息出现异常");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 服务器是否连接成功
     */
    private boolean isConnected() {
        return mClient != null && !mClient.isClosed() && mClient.isConnected();
    }

    private void closeSocket() {
        // 服务器对外停止服务了，所以需要关闭客户端的Socket
        try {
            if (mClient != null) {
                mClient.close();
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(Client.REMOTE_SERVER_SHUTDOWN);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mClient = null;
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("(HH:mm:ss):", Locale.US).format(date);
    }

    private void showMsg(String msg, int msgType) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 4; // 4px
        TextView tvMsg = new TextView(this, null, R.attr.chatViewStyle);
        if (msgType == 2) {
            tvMsg.setTextColor(ContextCompat.getColor(this, R.color.green));
            lp.gravity = Gravity.END;
            tvMsg.setGravity(Gravity.END);
        }
        tvMsg.setText(msg);
        llMsgLayout.addView(tvMsg, lp);
    }

    private void showMsg(String msg) {
        showMsg(msg, 1);
    }

}
