package com.tcpsocketserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tcpsocketserver.tcpsocketserver.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String APP_NAME = "TCP Socket Server";
    public static final int DEFAULT_PORT = 7171;
    public static final int DEFAULT_TIMETOKICK = 10000;

    // Opcodes (Operation Codes)
    // CTS - Client to Server
    public static final short OPCODE_CTS_CONNECT = 1;
    public static final short OPCODE_CTS_DISCONNECT = 2;
    public static final short OPCODE_CTS_USERSMAPSIGNAL = 3;
    // STC - Server to Client
    public static final short OPCODE_STC_CONNECT = 1;
    public static final short OPCODE_STC_DISCONNECT = 2;
    public static final short OPCODE_STC_USERSMAPSIGNAL = 3;

    public static MainActivity mainActivity;
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    public EditText ipEditText;
    public EditText portEditText;
    public EditText onlineUsersEditText;

    public static Map<String, User> usersMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivity = this;
        sp = getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        spe = sp.edit();

        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);
        onlineUsersEditText = findViewById(R.id.onlineUsersEditText);

        loadDefaultValues();

        Thread thread = new Thread(new ProtocolParser(this));
        thread.start();

        // Executes periodically, once per second
        Thread ticksThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onTick();
                            }
                        });
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        ticksThread.start();
    }

    private void onTick() {
        long time = System.currentTimeMillis();

        for (Map.Entry<String, User> entry : usersMap.entrySet()) {
            String key = entry.getKey();
            User user = entry.getValue();

            // If elapsed time > DEFAULT_TIMETOKICK, kick user
            if (time - user.lastActionTime > DEFAULT_TIMETOKICK) {
                onLogout(key);
            }
        }

        // Online users
        onlineUsersEditText.setText(String.format("%d", usersMap.size()));
    }

    public void onLogin(String username, String clientIP, long lastActionTime) {
        // Registering user into server data, without overwriting it
        if (!usersMap.containsKey(username)) {
            User user = new User();
            user.username = username;
            user.ip = clientIP;
            user.lastActionTime = lastActionTime;
            MainActivity.usersMap.put(user.username, user);
        }

        User user = usersMap.get(username);
        if (user == null) {
            return;
        }

        // Signal to client
        ProtocolSender protocolSender = new ProtocolSender(user);
        protocolSender.execute(String.format("%d", OPCODE_STC_CONNECT));

        // Server Log
        Toast.makeText(getApplicationContext(), String.format("%s iniciou a sessao.", username), Toast.LENGTH_SHORT).show();
    }

    public void onLogout(String username) {

        User user = usersMap.get(username);
        if (user == null) {
            return;
        }
        // Remove from list
        usersMap.remove(username);

        // Signal to client
        ProtocolSender protocolSender = new ProtocolSender(user);
        protocolSender.execute(String.format("%d", OPCODE_STC_DISCONNECT));

        // Server Log
        Toast.makeText(this, String.format("%s encerrou a sessao.", user.username), Toast.LENGTH_SHORT).show();
    }

    public void onClickUpdateButton(View view) {
        String port = portEditText.getText().toString();

        // Saving data at app preferences
        spe.putInt("port", !port.equals("") ? Integer.parseInt(port) : DEFAULT_PORT);
        spe.commit();
    }



    public void loadDefaultValues() {
        ipEditText.setText(Utils.getIPAddress(true));
        portEditText.setText(String.format("%d", sp.getInt("port", DEFAULT_PORT)));
    }
}
