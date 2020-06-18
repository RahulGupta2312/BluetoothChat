package in.forwardcode.bluetoothmessenger;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.forwardcode.bluetoothmessenger.R;

public class ChatActivity extends Activity {
    private ListView chatList;
    private EditText msg_field;
	private Button msg_send;
    private ArrayAdapter chatAdapter;
    private Button disconnect;
    private TextView chatName, instructions;
    private BluetoothAdapter bluetoothAdapter;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private StringBuffer outStringBuffer;
    private LinearLayout buttonSet, msgLayout;
    private String connectedDeviceName = null;
    private ChatService chatService = null;
    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
                            chatAdapter.clear();
                            break;
                        case ChatService.STATE_CONNECTING:
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatAdapter.add(connectedDeviceName + ":  " + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:

                    connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "Connected to " + connectedDeviceName,
                            Toast.LENGTH_SHORT).show();
//                    String info = data.getExtras().getString("info");
                    buttonSet.setVisibility(View.GONE);
                    disconnect.setVisibility(View.VISIBLE);
                    instructions.setVisibility(View.GONE);
                    chatList.setVisibility(View.VISIBLE);
                    chatName.setText("You are now chatting with: " + connectedDeviceName);
                    chatName.setVisibility(View.VISIBLE);
                    msgLayout.setVisibility(View.VISIBLE);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // initialization
        chatList = findViewById(R.id.chatList);
        msg_field = findViewById(R.id.msg_field);
        msg_send = findViewById(R.id.msg_send_btn);
        chatName = findViewById(R.id.tv_chatNAME);
        instructions = findViewById(R.id.tv_instructions);
        buttonSet = findViewById(R.id.btn_set_layout);
        msgLayout = findViewById(R.id.msg_field_layout);
        disconnect = findViewById(R.id.btn_disconnect);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // hiding keyboard
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        msg_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(msg_field.getText().toString());
            }
        });
    }

    private void sendMessage(String message) {
        Log.i("track", "sendMessage_ChatActivity");
        if (chatService.getState() != ChatService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatService.write(send);

            outStringBuffer.setLength(0);
            msg_field.setText(outStringBuffer);
        }

    }

    public void setupChat() {

        chatAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        chatList.setAdapter(chatAdapter);
        chatService = new ChatService(handler, disconnect, buttonSet, chatName, chatAdapter,
                ChatActivity.this, chatList, msgLayout, instructions);

        outStringBuffer = new StringBuffer();

    }

    public void connectDevice(Intent data) {
        String address = data.getExtras().getString(
                DeviceListActivity.DEVICE_ADDRESS);
//        String info = data.getExtras().getString("info");
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        chatService.connect(device);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.i("track", "onResume_ChatActivity");

        if (chatService != null) {
            if (chatService.getState() == ChatService.STATE_NONE) {
                chatService.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("track", "onDestroy_ChatActivity");
        if (chatService != null)
            chatService.stop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getRuntimePermissions();
        //EnableBT();
        MakeDiscoverable();
        

    }


    @TargetApi(23)
	public void getRuntimePermissions() {
        Log.e("testingabc", "getRuntimePermissions");
        int permissionCheckStorage = this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheckLocation = this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheckStorage != PackageManager.PERMISSION_GRANTED && permissionCheckLocation != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    @TargetApi(23)
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted.
//
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_LONG).show();
                }
            } else
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
            getRuntimePermissions();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        else {
//            Toast.makeText(ChatActivity.this, "Device not discoverable", Toast.LENGTH_LONG).show();
//            // invoke intent to make it discoverable again
//            makeDiscoverable();
//        }
        if (requestCode == 111 && resultCode == RESULT_OK) {
            connectDevice(data);
        }
        if (requestCode == 1 && resultCode == RESULT_OK) {
//            if bluetooth enabled, then check if discoverable or not
//            Toast.makeText(ChatActivity.this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();

        }
    }


    public void MakeDiscoverable() {
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent); // discoverable requestcode=333
        } else {
//            Toast.makeText(ChatActivity.this, "Bluetooth discoverable", Toast.LENGTH_SHORT).show();
        }

    }

    public void ConnectDevice(View v) {
        if (!bluetoothAdapter.isEnabled() && bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Toast.makeText(ChatActivity.this, "Please enable bluetooth & make discoverable", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, 111);
            Toast.makeText(ChatActivity.this, "Searching nearby device, please wait..", Toast.LENGTH_SHORT).show();

        }

        if (chatService == null) {
            setupChat();


        }

    }

    public void DisconnectDevice(View view) {
        if (chatService != null) {
            chatService.stop();
        }
        buttonSet.setVisibility(View.VISIBLE);
        disconnect.setVisibility(View.GONE);
        chatName.setText("");
        chatName.setVisibility(View.GONE);
        chatAdapter.clear();
        chatAdapter.notifyDataSetChanged();


    }

}
