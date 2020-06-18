package in.forwardcode.bluetoothmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatService {
    private static final String NAME = "BluetoothMessenger";

    private static final UUID MY_UUID = UUID
            .fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private Handler handler1;
    private AcceptThread secureAcceptThread;
    private AcceptThread insecureAcceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private Context context;
    private Button disconnect;
    private LinearLayout buttonSet, msgLayout;
    private TextView chatName, instructions;
    private ArrayAdapter chatAdapter;
    private ListView chatList;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1; // listening connection
    public static final int STATE_CONNECTING = 2; // initiate outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // connected to remote device

    public ChatService(Handler handler, Button disconnect, LinearLayout buttonSet, TextView chatName, ArrayAdapter chatAdapter,
                       Context context, ListView chatList, LinearLayout msgLayout, TextView instructions) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
        state = STATE_NONE;
        this.buttonSet = buttonSet;
        this.chatName = chatName;
        this.disconnect = disconnect;
        this.context = context;
        this.chatAdapter = new ArrayAdapter(this.context, android.R.layout.simple_list_item_1);
        this.chatAdapter = chatAdapter;
        this.msgLayout = msgLayout;
        this.chatList = chatList;
        this.instructions = instructions;
    }

    // Set the current state of the chat connection
    private synchronized void setState(int state) {
        this.state = state;

        handler.obtainMessage(ChatActivity.MESSAGE_STATE_CHANGE, state, -1)
                .sendToTarget();
    }

    // get current connection state
    public synchronized int getState() {
        return state;
    }

    // start service
    public synchronized void start() {
        // Cancel any thread
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any running thread
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
//        if (secureAcceptThread == null) {
//            secureAcceptThread = new AcceptThread(true);
//            secureAcceptThread.start();
//        }
        if (insecureAcceptThread == null) {
            insecureAcceptThread = new AcceptThread();
            insecureAcceptThread.start();
        }
    }

    // initiate connection to remote device
    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread
        Log.i("track", "connect_CharServiceActivity");
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel running thread
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    // manage Bluetooth connection
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device, final String socketType) {
        Log.i("track", "connected_CharServiceActivity");
        // Cancel the thread
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel running thread
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (secureAcceptThread != null) {
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }
        if (insecureAcceptThread != null) {
            insecureAcceptThread.cancel();
            insecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(ChatActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    // stop all threads
    public synchronized void stop() {
        Log.i("track", "stop_CharServiceActivity");
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (secureAcceptThread != null) {
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }

        if (insecureAcceptThread != null) {
            insecureAcceptThread.cancel();
            insecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out) {
        Log.i("track", "write_CharServiceActivity");
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED)
                return;
            r = connectedThread;
        }
        r.write(out);
    }

    private void connectionFailed() {
        Log.i("track", "connectionFailed_CharServiceActivity");
        Message msg = handler.obtainMessage(ChatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        ChatService.this.start();
    }

    private void connectionLost() {
        Log.i("track", "connectionLost_CharServiceActivity");
        Message msg = handler.obtainMessage(ChatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.TOAST, "Device disconnected");
        msg.setData(bundle);
        handler.sendMessage(msg);
        handler1 = new Handler(Looper.getMainLooper());
        handler1.post(new Runnable() {
            @Override
            public void run() {
                buttonSet.setVisibility(View.VISIBLE);
                disconnect.setVisibility(View.GONE);
                chatName.setText("");
                chatName.setVisibility(View.GONE);
                instructions.setVisibility(View.VISIBLE);
                chatList.setVisibility(View.GONE);
                msgLayout.setVisibility(View.GONE);
//                Toast.makeText(context, chatAdapter.getItem(0).toString(), Toast.LENGTH_SHORT).show();
                chatAdapter.clear();
                chatAdapter.notifyDataSetChanged();
            }
        });


        // Start the service over to restart listening mode
        ChatService.this.start();
    }

    // runs while listening for incoming connections
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private String socketType;

        public AcceptThread() {
            Log.i("track", "AcceptThread_CharServiceActivity");
            BluetoothServerSocket tmp = null;
            socketType = "Insecure";

            try {

                tmp = bluetoothAdapter
                        .listenUsingInsecureRfcommWithServiceRecord(
                                NAME, MY_UUID);

            } catch (IOException e) {
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.i("track", "run_accept_CharServiceActivity");
            setName("AcceptThread" + socketType);

            BluetoothSocket socket = null;

            while (state != STATE_CONNECTED) {
                try {
//                    if (serverSocket == null) {
//                        Toast.makeText(context, "Connection lost", Toast.LENGTH_SHORT).show();
//                        ChatService.this.start();
//
//                    } else
                        socket = serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (ChatService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        socketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate
                                // new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            Log.i("track", "cancel_accept_CharServiceActivity");
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    // runs while attempting to make an outgoing connection
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private String socketType;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;

            try {
                Log.i("track", "ConnectThread_CharServiceActivity");
                tmp = device
                        .createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            socket = tmp;
        }

        public void run() {
            Log.i("track", "run_connect_CharServiceActivity");
            setName("ConnectThread" + socketType);

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ChatService.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(socket, device, socketType);
        }

        public void cancel() {
            Log.i("track", "cancel_connect_CharServiceActivity");
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    // runs during a connection with a remote device
    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            this.bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(ChatActivity.MESSAGE_READ, bytes, -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    // Start the service over to restart listening mode
                    ChatService.this.start();
                    break;
                }
            }
        }

        // write to OutputStream
        public void write(byte[] buffer) {
            Log.i("track", "write_connected_CharServiceActivity");
            try {
                outputStream.write(buffer);
                handler.obtainMessage(ChatActivity.MESSAGE_WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            Log.i("track", "cancel_connected_CharServiceActivity");
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
