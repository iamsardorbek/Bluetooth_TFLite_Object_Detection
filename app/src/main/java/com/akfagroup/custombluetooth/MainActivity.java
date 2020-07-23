package com.akfagroup.custombluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity im{
    public static final String MyPREFERENCES = "MyPrefs" ;
    Button onBtn, offBtn, disconnectBtn,setMacBtn;
    EditText macAddressEditText;
    String address = "00:21:13:04:FE:84"; //need some activity to set it by user or save it
    TextView lumn;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    SharedPreferences sharedpreferences;
    BroadcastReceiver mBluetoothReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action!=null && action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    msg("Соединение с Arduino прервано, соединяюсь снова.");
                    isBtConnected = false;
                    new ConnectBT().execute();
                }
            }
        };

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedpreferences.edit();
        if(sharedpreferences.getString("address", null) == null) {
            editor.putString("address", address);
            editor.apply();
        }
        else
        {
            address = sharedpreferences.getString("address", null);
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBluetoothReceiver, filter);

        onBtn = (Button) findViewById(R.id.on_btn);
        offBtn = (Button) findViewById(R.id.off_btn);
        setMacBtn = findViewById(R.id.set_mac);
        disconnectBtn = (Button) findViewById(R.id.disconnect_btn);
        lumn = (TextView) findViewById(R.id.textview);
        macAddressEditText = findViewById(R.id.mac_address);
        macAddressEditText.setText(address);
        new ConnectBT().execute();

        onBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                sendSignal("1");
            }
        });

        offBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                sendSignal("2");
            }
        });

        disconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Disconnect();
            }
        });

        setMacBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newMac = macAddressEditText.getText().toString();
                if(!newMac.isEmpty())
                {
                    if(newMac.length() == 17)
                    {
                        if(newMac.charAt(2) == ':' && newMac.charAt(5) == ':' && newMac.charAt(8) == ':' && newMac.charAt(11) == ':' && newMac.charAt(14) == ':')
                        {
                            boolean containsCorrectChars = true;
                            for(Character c : newMac.toCharArray())
                            {
                                if(Character.isLetter(c))
                                    if(!(c >= 41 && c <= 46))
                                        containsCorrectChars = false;
                                if(!Character.isAlphabetic(c) && !Character.isLetter(c) && c != ':')
                                    containsCorrectChars = false;

                            }
                            if(!containsCorrectChars) {
                                Disconnect();
                                isBtConnected = false;
                                address = newMac;
                                editor.putString("address", address);
                                editor.apply();
                                new ConnectBT().execute();
                            }
                            else {
                                msg("Неверный MAC-адрес");
                            }
                        }
                        else msg("Неверный MAC-адрес");
                    }
                    else msg("Неверный MAC-адрес");
                }
                else msg("Неверный MAC-адрес");
            }
        });
    }

    private void sendSignal ( String number ) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(number.toString().getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                isBtConnected = false;
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

    }

    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
//            progress = ProgressDialog.show(getApplicationContext(), "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                    isBtConnected = true;
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            catch (IllegalArgumentException iae)
            {
                iae.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Подключение не удалось. Пробую снова.");
                new ConnectBT().execute();
            } else {
                msg("Соединение с Bluetooth установлено");
                isBtConnected = true;
            }
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothReceiver);
    }
}