package com.github.douglasjunior.bluetoothsample;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothDeviceDecorator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by dougl on 10/04/2017.
 */

public class DeviceItemAdapter extends RecyclerView.Adapter<DeviceItemAdapter.ViewHolder> {

    private final Context mContext;
    private final List<BluetoothDeviceDecorator> mDevices;
    private final LayoutInflater mInflater;
    private OnAdapterItemClickListener mOnItemClickListener;


    public DeviceItemAdapter(Context context, List<BluetoothDevice> devices) {
        super();
        mContext = context;
        mDevices = decorateDevices(devices);
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static List<BluetoothDeviceDecorator> decorateDevices(Collection<BluetoothDevice> btDevices) {
        List<BluetoothDeviceDecorator> devices = new ArrayList<>();
        for (BluetoothDevice dev : btDevices) {
            devices.add(new BluetoothDeviceDecorator(dev, 0));
        }
        return devices;
    }

    public DeviceItemAdapter(Context context, Set<BluetoothDevice> devices) {
        this(context, new ArrayList<>(devices));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder vh = new ViewHolder(mInflater.inflate(R.layout.device_item, parent, false));
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final BluetoothDeviceDecorator device = mDevices.get(position);

        holder.tvName.setText(TextUtils.isEmpty(device.getName()) ? "---" : device.getName());
        holder.tvAddress.setText(device.getAddress());
        holder.tvRSSI.setText(device.getRSSI() + "");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onItemClick(device, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public List<BluetoothDeviceDecorator> getDevices() {
        return mDevices;
    }

    public void setOnAdapterItemClickListener(OnAdapterItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public interface OnAdapterItemClickListener {
        public void onItemClick(BluetoothDeviceDecorator device, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvAddress;
        private final TextView tvRSSI;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
            tvAddress = (TextView) itemView.findViewById(R.id.tv_address);
            tvRSSI = (TextView) itemView.findViewById(R.id.tv_rssi);
        }
    }
}
