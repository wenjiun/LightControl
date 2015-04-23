package my.wenjiun.yeelightblue;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;


/**
 * A placeholder fragment containing a simple view.
 */
@SuppressWarnings("deprecation")
public class MainActivityFragment extends Fragment {

    private static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTICS_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static final int REQUEST_ENABLE_BT = 0;

    private boolean isOn = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice yeelight;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGatt gatt;
    private boolean hasBle;
    private Handler mHandler = new Handler();
    private ArrayList<BluetoothDevice> devicesScanned = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> yeelights = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothGatt> gatts = new ArrayList<BluetoothGatt>();
    private ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<BluetoothGattCharacteristic>();
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private ProgressDialog progressDialog;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hasBle = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        if(hasBle) {
            BluetoothManager bluetoothManager = (BluetoothManager) (getActivity().getSystemService(Context.BLUETOOTH_SERVICE));
            mBluetoothAdapter = bluetoothManager.getAdapter();

            if(!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                getActivity().finish();
                Toast.makeText(getActivity(), "After Bluetooth is enabled, please relaunch the Light Control App", Toast.LENGTH_LONG).show();
            }

            connectYeelight();
        } else {
            Toast.makeText(getActivity(), "Bluetooth LE is not supported", Toast.LENGTH_LONG).show();
        }
        setHasOptionsMenu(true);
    }

    private void connectYeelight() {
        String yeelight_strings = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("devices", "");
        if(!yeelight_strings.equals("")) {
            yeelights.clear();
            String[] split = yeelight_strings.split(";");
            for(int i = 0;i < split.length; i++) {
                yeelight = mBluetoothAdapter.getRemoteDevice(split[i]);
                yeelights.add(yeelight);
            }
        } else {
            Toast.makeText(getActivity(), "No Yeelight Blue has been setup", Toast.LENGTH_LONG).show();
        }
        if(!yeelights.isEmpty()) {
            gatts.clear();
            characteristics.clear();
            for(BluetoothDevice device: yeelights) {
                gatt = device.connectGatt(getActivity(), true, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if (newState == BluetoothAdapter.STATE_CONNECTED) {
                            gatt.discoverServices();
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                        BluetoothGattService mService = gatt.getService(UUID.fromString(SERVICE_UUID));
                        mCharacteristic = mService.getCharacteristic(UUID.fromString(CHARACTERISTICS_UUID));
                        if(mCharacteristic!=null && gatt!=null) {
                            mCharacteristic.setValue(",,,0,,,,,,,,,,,,,,");
                            gatt.writeCharacteristic(mCharacteristic);
                        }
                        characteristics.add(mCharacteristic);
                    }
                });
                gatts.add(gatt);
            }
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        final ImageView image_lamp = (ImageView)view.findViewById(R.id.image_lamp);
        final ImageView image_switch = (ImageView)view.findViewById(R.id.image_switch);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOn = !isOn;
                if(isOn) {
                    if(yeelights.size() != 0) {
                        for(int j = 0;j < yeelights.size();j++) {
                            if(characteristics.size() > 0 && gatts.size() > 0) {
                                if(characteristics.get(j)!=null && gatts.get(j)!=null) {
                                    characteristics.get(j).setValue(",,,100,,,,,,,,,,,,");
                                    gatts.get(j).writeCharacteristic(characteristics.get(j));
                                }
                            }
                        }
                    }
                    image_lamp.setImageResource(R.drawable.lighton);
                    image_switch.setImageResource(R.drawable.button_off);
                    playSound();
                } else {
                    if(yeelights.size() != 0) {
                        for(int j = 0;j < yeelights.size();j++) {
                            if(characteristics.size() > 0 && gatts.size() > 0) {
                                if(characteristics.get(j)!=null && gatts.get(j)!=null) {
                                    mCharacteristic.setValue(",,,0,,,,,,,,,,,,,,");
                                    gatts.get(j).writeCharacteristic(characteristics.get(j));
                                }
                            }
                        }
                    }
                    image_lamp.setImageResource(R.drawable.lightoff);
                    image_switch.setImageResource(R.drawable.button_on);
                    playSound();
                }
            }
        };
        image_switch.setOnClickListener(onClickListener);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_setup_yeelight:
                if(hasBle) {
                    final StringBuilder builder = new StringBuilder();
                    progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Setting up Yeelight Blue ...");
                    progressDialog.show();
                    mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                        @Override
                        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                            if(device.getName().equals("Yeelight Blu")) {
                                boolean isDuplicate = false;
                                for(BluetoothDevice deviceScanned: devicesScanned) {
                                    if(deviceScanned.getAddress().equals(device.getAddress())) {
                                        isDuplicate = true;
                                    }
                                }
                                if(!isDuplicate) {
                                    devicesScanned.add(device);
                                    builder.append(device.getAddress() + ";");
                                }
                            }
                        }

                    };
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            if(progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            if(builder.length()!=0) {
                                builder.setLength(builder.length() - 1);
                                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().
                                        putString("devices", builder.toString()).apply();
                                Toast.makeText(getActivity(), devicesScanned.size() + " Yeelight Blue added",
                                        Toast.LENGTH_LONG).show();
                                connectYeelight();
                            } else {
                                Toast.makeText(getActivity(), "No Yeelight Blue detected",
                                        Toast.LENGTH_LONG).show();                            }
                        }
                    }, 10000);
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                } else {
                    Toast.makeText(getActivity(), "Bluetooth LE is not supported", Toast.LENGTH_LONG).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void playSound() {
        final MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), R.raw.lightswitch);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(mediaPlayer!=null) {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                }
            }
        });
    }
}
