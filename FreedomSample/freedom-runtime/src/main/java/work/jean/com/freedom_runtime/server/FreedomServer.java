package work.jean.com.freedom_runtime.server;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import work.jean.com.freedom_runtime.receiver.ReStartAppReceiver;
import work.jean.com.freedom_runtime.service.FreedomService;
import work.jean.com.freedom_runtime.util.Constant;

/**
 * Created by rantianhua on 17/4/16.
 */

public class FreedomServer implements Runnable {

    private static final int PORT_START = 16666;
    private static final String LOG_TAG = "FreedomServer";

    private static FreedomServer sFreedomServer;

    private final int mPort;
    private final ServerSocket mServerSocket;

    private FreedomServer(int port, ServerSocket serverSocket) {
        mPort = port;
        mServerSocket = serverSocket;
        new Thread(this, "FreedomServer").start();
    }

    public static void startServer() {
        if (sFreedomServer != null) {
            Log.d(LOG_TAG, "Freedom server has started!");
            return;
        }
        for (int i = 0; i < 20; i++) {
            try {
                final ServerSocket serverSocket = new ServerSocket(PORT_START + i);
                sFreedomServer = new FreedomServer(PORT_START + i, serverSocket);
                break;
            } catch (Exception e) {
                Log.e(LOG_TAG, "error in start freedom server running", e);
            }
        }
    }

    public static void stopServer() {
        if (sFreedomServer == null) return;
        if (sFreedomServer.mServerSocket == null) return;
        try {
            sFreedomServer.mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            sFreedomServer = null;
        }
    }

    @Override
    public void run() {
        Log.d(LOG_TAG, "start freedom server, port is " + mPort);
        StringBuilder sb = new StringBuilder();
        while (sFreedomServer != null) {
            Socket client = null;
            try {
                Log.d(LOG_TAG, "waiting a client...");
                client = mServerSocket.accept();
                Log.d(LOG_TAG, "accept a client.");

                InputStream inputStream = client.getInputStream();
                int len;
                byte[] bytes = new byte[256];
                Log.d(LOG_TAG, "start to read client input.");
                while ((len = inputStream.read(bytes)) != -1) {
                    String str = new String(bytes, 0, len);
                    sb.append(str);
                }

                inputStream.close();

                final String msg = sb.toString();
                Log.d(LOG_TAG, "client message:" + msg);
                handleReceiveMsg(msg);

                sb.delete(0, sb.length());
                client.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error in freedom server running", e);
            }
        }
        Log.d(LOG_TAG, "stop freedom server");
    }

    private void handleReceiveMsg(String msg) {
        try {

            JSONObject jsonObject = new JSONObject(msg);
            boolean haveDex = false, haveRes = false;

            if (jsonObject.has("dex")) {
                File dir = new File(FreedomService.sContext.getCacheDir(), Constant.FREEDOM_DEX_PATCH_DIR);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(LOG_TAG, "create " + dir.getAbsolutePath() + " failed");
                        return;
                    }
                }

                JSONObject dexObj = jsonObject.getJSONObject("dex");
                JSONArray arrName = dexObj.getJSONArray("name");
                JSONArray arrContent = dexObj.getJSONArray("content");

                for (int i = 0; i < arrName.length(); i++) {
                    String fileName = (String) arrName.get(i);
                    String fileContent = (String) arrContent.get(i);

                    File file = new File(dir, fileName);
                    if (file.exists()) {
                        if (!file.delete()) {
                            Log.e(LOG_TAG, "cannot delete " + file.getAbsolutePath());
                            return;
                        }
                    }

                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(Base64.decode(fileContent, Base64.DEFAULT));
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    Log.d(LOG_TAG, "save dex file in " + file.getAbsolutePath());
                }

                haveDex = true;
            }

            if (jsonObject.has("res")) {
                JSONObject resObj = jsonObject.getJSONObject("res");
                String patchName = resObj.getString("name");
                String patchContent = resObj.getString("content");

                File dir = new File(FreedomService.sContext.getCacheDir(), Constant.FREEDOM_RES_PATCH_DIR);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(LOG_TAG, "create " + dir.getAbsolutePath() + " failed");
                        return;
                    }
                }

                File file = new File(dir, patchName);
                if (file.exists()) file.delete();

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(Base64.decode(patchContent, Base64.DEFAULT));
                fileOutputStream.flush();
                fileOutputStream.close();

                Log.d(LOG_TAG, "save res patch in " + file.getAbsolutePath());

                haveRes = true;
            }

            final boolean haveDexExtra = haveDex;
            final boolean haveResExtra = haveRes;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(FreedomService.sContext,
                            ReStartAppReceiver.class);
                    intent.putExtra(Constant.FREEDOM_EXTRA_HAVE_DEX, haveDexExtra);
                    intent.putExtra(Constant.FREEDOM_EXTRA_HAVE_RES, haveResExtra);
                    FreedomService.sContext.sendBroadcast(intent);
                }
            });

        } catch (Exception e) {
            Log.e(LOG_TAG, "error in handleReceiveMsg", e);
        }
    }
}
