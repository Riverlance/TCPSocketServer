package com.tcpsocketserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;

public class ProtocolParser implements Runnable {
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    Context context;
    Handler handler;

    public ProtocolParser(Context context) {
        sp = context.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);
        spe = sp.edit();

        this.context = context;

        handler = new Handler();
    }

    @Override
    public void run() {
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Esperando mensagens de clientes.", Toast.LENGTH_SHORT).show();
                }
            });

            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                short opcode = dataInputStream.readShort(); // Opcode Client to Server
                final String username = dataInputStream.readUTF(); // Username
                if (opcode == MainActivity.OPCODE_CTS_CONNECT) {
                    final String clientIP = dataInputStream.readUTF(); // Client IPv4

                    final long lastActionTime = System.currentTimeMillis();
                    final Calendar calendar = Calendar.getInstance(); // Last action time
                    calendar.setTimeInMillis(lastActionTime);
                    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    final int minute = calendar.get(Calendar.MINUTE);
                    final int day = calendar.get(Calendar.DAY_OF_MONTH);
                    final int month = calendar.get(Calendar.MONTH);
                    final int year = calendar.get(Calendar.YEAR);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // onLogin
                            MainActivity.mainActivity.onLogin(username, clientIP, lastActionTime);

                            //Toast.makeText(context, String.format("%s:%s/~%s : %02d:%02d-%02d/%02d/%04d", clientIP, port, username, hour, minute, day, month, year), Toast.LENGTH_LONG).show();
                            System.out.println(String.format("%s:%s/~%s : %02d:%02d-%02d/%02d/%04d", clientIP, port, username, hour, minute, day, month, year));
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_DISCONNECT) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mainActivity.onLogout(username);
                        }
                    });
                } else if (opcode == MainActivity.OPCODE_CTS_USERSMAPSIGNAL) {
                    // to do
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
handler.post(new Runnable() {
@Override
public void run() {
    Toast.makeText(context, String.format("%s:%s/~%s : %s %02d:%02d-%02d/%02d/%04d", clientIP, port, username, message, hour, minute, day, month, year), Toast.LENGTH_LONG).show();
}
});
*/
