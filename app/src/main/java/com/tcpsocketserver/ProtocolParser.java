package com.tcpsocketserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;

public class ProtocolParser implements Runnable {
    // Needed stuffs
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    // Allows to use UI within this background task
    // This is needed because this class is executed in another thread that is different of the main UI thread
    Handler handler;

    public ProtocolParser(Context context) {
        // Needed stuffs
        sp = context.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);
        spe = sp.edit();
        handler = new Handler();
    }

    @Override
    public void run() {
        // Params to listen a client
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);

        try {
            // Socket connection
            ServerSocket serverSocket = new ServerSocket(port);

            // Execute in UI
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Context context = MainActivity.mainActivity.getApplicationContext();
                    Toast.makeText(context, "Esperando mensagens de clientes.", Toast.LENGTH_SHORT).show();
                }
            });

            while (true) {
                // Stream
                Socket socket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                // Basic data
                short opcode = dataInputStream.readShort(); // Opcode Client to Server
                final String username = dataInputStream.readUTF(); // Username

                if (opcode == MainActivity.OPCODE_CTS_SELFCONNECT) {
                    final String clientIP = dataInputStream.readUTF(); // Client IPv4

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mainActivity.login(username, clientIP);
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_SELFDISCONNECT) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mainActivity.logout(username);
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_UPDATEDUSERSLIST) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            User user = MainActivity.mainActivity.usersMap.get(username);
                            if (user != null) {
                                ProtocolSender protocolSender = new ProtocolSender(user);
                                protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_UPDATEDUSERSLIST));
                            }
                        }
                    });
                }
            }

            // Never happens because Server is always listening the Client
            // dataInputStream.close();
            // serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
