package cc.linkmob.useblelibrarydemo;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cc.linkmob.bluetoothlowenergylibrary.BluetoothUtils;

public class MainActivity extends AppCompatActivity {

    private BluetoothUtils mBluetoothUtil;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private TextView statusTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initView();
        try {
            mBluetoothUtil = BluetoothUtils.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mBluetoothUtil.setOnBluetoothUtilStatusChangeLinsener(new BluetoothUtils.OnBluetoothUtilStatusChangeListener() {
            @Override
            public void onFindDevice(BluetoothDevice device) {

                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeServiceInitFailed() {
            }

            @Override
            public void onFindGattServices(List<BluetoothGattService> supportedGattServices) {
            }

            @Override
            public void onFindData(String uuid, String data) {
//                Toast.makeText(MainActivity.this, "FindData", Toast.LENGTH_SHORT).show();
//                dataValue.setText("接收：" + data + "/r/n");
                dataValue.append("接收：" + data + "\r\n");
            }

            @Override
            public void onConnected() {
                mListView.setVisibility(View.GONE);
                gattView.setVisibility(View.VISIBLE);
                isScanView = false;
                isConnected = true;
                statusTV.setText("status:connected");
                invalidateOptionsMenu();
            }

            @Override
            public void onDisconnected() {
                isConnected = false;
                statusTV.setText("status:disconnected");
                invalidateOptionsMenu();
            }

            @Override
            public void onFindGattService(BluetoothGattService supportedGattService) {
            }

            @Override
            public void onFindGattCharacteristic(BluetoothGattCharacteristic characteristic) {

                mNotifyCharacteristic = characteristic;
                mBluetoothUtil.setCharacteristicNotification(
                        characteristic, true);
            }

            @Override
            public void onSendData(String UUID, String data) {
                dataValue.append("发送：" + data + "\r\n");
            }
        });


    }

    /**
     * 打开蓝牙成功失败的回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            showToast("打开成功");
        } else {
            showToast("打开失败");

        }
    }

    Toast mToast = null;

    private synchronized void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private ListView mListView;

    //TODO 这里的cID和sID 分别为 远程ble硬件的参数，需要更具具体硬件设置
    /**
     * Characteristic's UUID. This is defined by hardware
     */
    UUID cId = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    /**
     * GattService's UUID. This is defined by hardware
     */
    UUID sId = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    private void initView() {
        mListView = (ListView) findViewById(R.id.listview);
        statusTV = (TextView) findViewById(R.id.tv_status);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                mAddress = device.getAddress();
                mBluetoothUtil.connectDevice(MainActivity.this, mAddress, sId, cId);
                Toast.makeText(MainActivity.this, "点击item", Toast.LENGTH_SHORT).show();
            }
        });

        gattView = findViewById(R.id.gattView);
        dataValue = (TextView) findViewById(R.id.data_value);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtSend = (EditText) findViewById(R.id.edtSend);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = edtSend.getText().toString();
                try {

                    if (!mBluetoothUtil.sendData(mNotifyCharacteristic, data)) {
                        showToast("发送失败！");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("发送失败！");
                }
                edtSend.setText("");
            }
        });

    }

    private String mAddress;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothUtil.onDestroy(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.scanLeDevice(this, true);
                }

                break;
            case R.id.menu_stop:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.scanLeDevice(this, false);
                }
                break;


            case R.id.menu_connect:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.connectDevice(this, mAddress, sId, cId);
                }
                break;
            case R.id.menu_disconnect:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.disconnecDevice();
                }
                break;

        }
        return true;
    }

    private boolean isScanView = true;
    private boolean isConnected = false;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (isScanView) {

            if (!mBluetoothUtil.isScanning()) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(
                        R.layout.actionbar_indeterminate_progress);
            }
        } else {
            if (isConnected) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
            }
        }
        return true;
    }


    @Override
    public void onBackPressed() {
        if (isScanView) {
            super.onBackPressed();

        } else {
            mBluetoothUtil.disconnecDevice();
            mListView.setVisibility(View.VISIBLE);
            gattView.setVisibility(View.GONE);
            dataValue.setText("");
            isScanView = true;
            invalidateOptionsMenu();
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
            LayoutInflater.from(MainActivity.this);
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }


        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private View gattView;

    private TextView dataValue;
    private Button btnSend;
    private EditText edtSend;
}
