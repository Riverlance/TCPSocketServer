package com.tcpsocketserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ProtocolSender extends AsyncTask<String, Void, String> { // <Params, Progress, Result>
    private SharedPreferences sp;

    Handler handler = new Handler();

    Map<String, User> usersMap = new HashMap<>();

    // Directly to an user
    public ProtocolSender(User user) {
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);

        usersMap.put(user.username, user);
    }

    // For all connected users
    public ProtocolSender(Context context, Map<String, User> usersMap) {
        sp = context.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);

        handler = new Handler();

        this.usersMap = usersMap;
    }

    @Override
    protected String doInBackground(String... strings) {
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);
        short opcode = Short.parseShort(strings[0]);

        try {
            for (Map.Entry<String, User> entry : usersMap.entrySet()) {
                String key = entry.getKey();
                User user = entry.getValue();

                Socket socket = new Socket(user.ip, port);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeShort(opcode);
                dataOutputStream.writeUTF(user.username);
                if (opcode == MainActivity.OPCODE_STC_CONNECT) {
                    dataOutputStream.writeUTF(user.ip);
                    dataOutputStream.writeLong(user.lastActionTime);
                } else if (opcode == MainActivity.OPCODE_STC_DISCONNECT) {
                    // Do nothing else, its just a signal
                }

                dataOutputStream.close();
                socket.close();
            }
        } catch (IOException e) {

        }
        return null;
    }

    /* Not needed
    @Override
    protected void onPostExecute(String string) {
    }
    */
}