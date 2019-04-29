package com.tcpsocketserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ProtocolSender extends AsyncTask<String, Void, String> { // <Params, Progress, Result>
    private SharedPreferences sp;

    Handler handler = new Handler();

    Map<String, User> users = new HashMap<>();

    // Directly to an user
    public ProtocolSender(User user) {
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);

        users.put(user.username, user);
    }

    // For all connected users
    public ProtocolSender(Map<String, User> users) {
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);

        handler = new Handler();

        this.users = users;
    }

    @Override
    protected String doInBackground(String... strings) {
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);
        short opcode = Short.parseShort(strings[0]);

        try {
            for (Map.Entry<String, User> targetEntry : users.entrySet()) {
                String targetKey = targetEntry.getKey();
                User targetUser = targetEntry.getValue();

                Socket socket = new Socket(targetUser.ip, port);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeShort(opcode);
                dataOutputStream.writeUTF(targetUser.username);

                if (opcode == MainActivity.OPCODE_STC_CONNECT) {
                    // Do nothing else

                } else if (opcode == MainActivity.OPCODE_STC_DISCONNECT) {
                    // Do nothing else

                } else if (opcode == MainActivity.OPCODE_STC_USERSMAPSIGNAL) {
                    dataOutputStream.writeInt(MainActivity.usersMap.size());
                    for (Map.Entry<String, User> entry : MainActivity.usersMap.entrySet()) {
                        String key = targetEntry.getKey();
                        User user = targetEntry.getValue();
                        dataOutputStream.writeUTF(user.username);
                    }
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
