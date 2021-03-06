package com.ykan.sdk.example;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.google.gson.reflect.TypeToken;
import com.yaokan.sdk.api.JsonParser;
import com.yaokan.sdk.model.DeviceDataStatus;
import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.listener.IDeviceControllerListener;
import com.yaokan.sdk.wifi.listener.LearnCodeListener;
import com.ykan.sdk.example.other.AnimStudy;

public class YKWifiDeviceControlActivity extends Activity implements IDeviceControllerListener, LearnCodeListener {

    protected static final String TAG = YKWifiDeviceControlActivity.class.getSimpleName();

    /**
     * The tv MAC
     */
    private TextView tvMAC;

    /**
     * The GizWifiDevice device
     */
    private GizWifiDevice device;

    private String rcCommand = "";

    private GridView gridView;

    private HashMap<String, KeyCode> codeDatas = new HashMap<String, KeyCode>();

    private List<String> codeKeys = new ArrayList<String>();

    private DeviceController driverControl = null;

    protected AnimStudy animStudy;

    private int currLearning = -1;

    JsonParser jsonParser = new JsonParser();

    private boolean isStudy = false;
//    private int tid = 0;
//    private int bid = 0;
//    private String rid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_control);
        animStudy = new AnimStudy(this);
        initDevice();
        initView();
    }

    private void initView() {
        driverControl = new DeviceController(getApplicationContext(), device, this);
        //获取设备硬件相关信息
        driverControl.getDevice().getHardwareInfo();
        if (isStudy) {//学习模式需要初始化
            driverControl.initLearn(this);
        }
        //修改设备显示名称
        driverControl.getDevice().setCustomInfo("alias", "遥控中心产品");
        tvMAC = (TextView) findViewById(R.id.tvMAC);
        gridView = (GridView) findViewById(R.id.codeGridView);
        if (null != device) {
            tvMAC.setText("MAC: " + device.getMacAddress().toString());
        }
        if (!Utility.isEmpty(rcCommand)) {
            codeDatas = new HashMap<String, KeyCode>();
            Type type = new TypeToken<HashMap<String, KeyCode>>() {
            }.getType();
            //解析数据
            codeDatas = jsonParser.parseObjecta(rcCommand, type);
            codeKeys = new ArrayList<String>(codeDatas.keySet());
            ExpandAdapter expandAdapter = new ExpandAdapter(getApplicationContext(), codeKeys);
            gridView.setAdapter(expandAdapter);

            //点击按钮 如果按钮上有code 就会把code发送到遥控中心
            gridView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String key = codeKeys.get(position);
                    KeyCode keyCode = codeDatas.get(key);
                    String code;
                    if (isStudy) {
                        code = keyCode.getLearnCode();//学习模式下学习到的code
                    } else {
                        code = keyCode.getSrcCode();//从Api中取到的code
                    }
                    if (!TextUtils.isEmpty(code))
                        driverControl.sendCMD(code);
                }
            });

            //如果是学习模式 长按按钮 按键会闪烁并且进入学习状态
            if (isStudy)
                gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent,
                                                   View view, int position, long id) {
                        currLearning = position;
                        TextView keyTv = (TextView) view.findViewById(R.id.key_btn);
                        keyTv.setTag("small_square");
                        animStudy.startAnim(keyTv);
                        driverControl.startLearn();    //开始学习。
                        return true;
                    }

                });
        }
    }

    /**
     * 接收intent传过来的数据
     */
    private void initDevice() {
        Intent intent = getIntent();
        device = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
        rcCommand = intent.getStringExtra("rcCommand");
        isStudy = intent.getBooleanExtra("type", false);
//        tid = intent.getIntExtra("tid", 0);
//        bid = intent.getIntExtra("bid", 0);
//        rid = intent.getStringExtra("rid");
    }


    public class ExpandAdapter extends BaseAdapter {

        private Context mContext;

        private LayoutInflater inflater;

        public List<String> keys;

        public ExpandAdapter(Context mContext, List<String> keys) {
            super();
            this.mContext = mContext;
            this.keys = keys;
            inflater = LayoutInflater.from(mContext);

        }

        @Override
        public int getCount() {
            return keys.size();
        }

        @Override
        public Object getItem(int position) {
            return keys.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.yk_ctrl_adapter_expand, null);
            }
            TextView keyBtn = (TextView) convertView.findViewById(R.id.key_btn);
            keyBtn.setText(keys.get(position));
            return convertView;
        }
    }


    @Override
    public void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
        switch (device.getNetStatus()) {
            case GizDeviceOffline:
                Logger.d(TAG, "设备下线");
                break;
            case GizDeviceOnline:
                Logger.d(TAG, "设备上线");
                break;
            default:
                break;
        }
    }

    @Override
    public void didReceiveData(DeviceDataStatus dataStatus, String data) {
        // TODO Auto-generated method stub
        switch (dataStatus) {
            case DATA_LEARNING_SUCCESS://学习成功
                String studyValue = data;//data 表示学习接收到的数据
                KeyCode keyCode = codeDatas.get(codeKeys.get(currLearning));
                keyCode.setLearnCode(studyValue);
                Logger.d(TAG, "学习成功:" + studyValue);
                animStudy.stopAnim(1);
                Toast.makeText(getApplicationContext(), "学习成功", Toast.LENGTH_SHORT).show();
                break;
            case DATA_LEARNING_FAILED://学习失败
                Logger.d(TAG, "学习失败");
                animStudy.stopAnim(1);
                Toast.makeText(getApplicationContext(), "学习失败", Toast.LENGTH_SHORT).show();
                break;
            case DATA_SEND_OK:
                if (data != null && data.startsWith("YK")) {
                    Logger.d(TAG, "发送成功 " + data);
                } else {

                }
                break;
            default:
                break;
        }

    }

    @Override
    public void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device, ConcurrentHashMap<String, String> hardwareInfo) {
        Logger.d(TAG, "获取设备信息 :");
        if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
            Logger.d(TAG, "获取设备信息 : hardwareInfo :" + hardwareInfo);
        } else {
        }
    }

    @Override
    public void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
        Logger.d(TAG, "自定义设备信息回调");
        if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
            Logger.d(TAG, "自定义设备信息成功");
        }
    }
}
