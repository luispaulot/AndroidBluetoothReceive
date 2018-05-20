package com.example.luis.blue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLED = 0;
    public static final int REQUEST_DISCOVERABLE = 0;

    public static final int MESSAGE_READ = 3;

    StringBuilder dadosBluetooth = new StringBuilder();

    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Button b_on, b_list, b_off, b_disc;
    ListView list;

    BluetoothAdapter bluetoothAdapter = null;

    BluetoothDevice meuBluetooth = null;

    BluetoothSocket meuBluetoothSocket = null;

    ConnectedThread connectedThread;

    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b_on = (Button) findViewById(R.id.b_on);
        b_off = (Button) findViewById(R.id.b_off);
        b_disc = (Button) findViewById(R.id.b_disc);
        b_list = (Button) findViewById(R.id.b_list);
        list = (ListView) findViewById(R.id.list);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            Toast.makeText(this, "Brucutu não suportado", Toast.LENGTH_SHORT).show();
            finish();
        }

        b_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLED);
            }
        });

        b_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.disable();
            }
        });

        b_disc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bluetoothAdapter.isDiscovering()){
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVERABLE);
                }
            }
        });

        b_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                ArrayList<String> devices = new ArrayList<String>();

                for(BluetoothDevice bt : pairedDevices){
                    devices.add(bt.getName()+"\n"+bt.getAddress());
                }

                ArrayAdapter arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, devices);

                list.setAdapter(arrayAdapter);
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // selected item
                String selected = ((TextView) view).getText().toString();

                String endereco = selected.substring(selected.length() - 17);

                meuBluetooth = bluetoothAdapter.getRemoteDevice(endereco);

                try{
                    meuBluetoothSocket = meuBluetooth.createInsecureRfcommSocketToServiceRecord(uuid);
                    meuBluetoothSocket.connect();

                    connectedThread = new ConnectedThread(meuBluetoothSocket);
                    connectedThread.start();

                    Toast.makeText(getApplicationContext(), "Ready to pair... Device conected!", Toast.LENGTH_SHORT).show();
                }catch (IOException e){
                    Toast.makeText(getApplicationContext(), "Ocorreu um erro ao conectar: " + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }

                //Toast.makeText(getApplicationContext(), endereco, Toast.LENGTH_SHORT).show();



            }
        });

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String recebido = null;
                if (msg.what == MESSAGE_READ) {
                    recebido = (String) msg.obj;
                    dadosBluetooth.append(recebido);

                    int fimInformacao = dadosBluetooth.indexOf("");
                    if (fimInformacao > 0) {
                        String dadosCompletos = dadosBluetooth.substring(0, fimInformacao);
                        Toast.makeText(MainActivity.this, dadosCompletos, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    String dadosBt = new String(buffer, 0, bytes);

                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dadosBt).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String enviar) {
            byte[] msgBuffer = enviar.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) { }
        }


    }
}
