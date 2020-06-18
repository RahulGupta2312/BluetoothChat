package in.forwardcode.bluetoothmessenger;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.forwardcode.bluetoothmessenger.R;

public class DeviceListAdapter extends BaseAdapter {
	
	ArrayList<DeviceData> dataList= new ArrayList<DeviceData>();
	Activity context;
	BluetoothDevice device;
	DeviceData dataClass;
	ArrayList<BluetoothDevice> pairedDevices=new ArrayList<BluetoothDevice>();
	DeviceListAdapter(Activity context, ArrayList<DeviceData> dataList,ArrayList<BluetoothDevice> pairedDevices )
	{
		this.context=context;
		this.dataList=dataList;
		this.pairedDevices=pairedDevices;
		
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return dataList.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return dataList.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(final int arg0, View arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		LayoutInflater inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view= inflater.inflate(R.layout.single_device_layout,arg2,false);
		TextView deviceDetail=(TextView)view.findViewById(R.id.deviceDetail);
		Button unpair=(Button)view.findViewById(R.id.btn_unpair);
		Button chat=(Button)view.findViewById(R.id.btn_chat);
		
		dataClass=dataList.get(arg0);
		deviceDetail.setText(dataClass.getName()+ "\n"+ dataClass.getAddress());
		
		unpair.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	            //...
	        	try {
	        		device=pairedDevices.get(arg0);
	        		boolean status=removeBond(device);
	        		if(status)
	        		{
	        			pairedDevices.remove(device);
	        			notifyDataSetChanged();
	        			Toast.makeText(context,dataClass.getName()+" : unpaired",Toast.LENGTH_SHORT).show();
	        		
	        			context.finish();
	        			
	        		}
	        		
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    });
		
		chat.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	            //...
	        	Intent intent = new Intent();
                intent.putExtra("device_address", dataClass.getAddress());
                intent.putExtra("info", dataClass.getName());
                context.setResult(Activity.RESULT_OK, intent);
                context.finish();
	        }
	    });
		
		return view;
	}
  public boolean removeBond(BluetoothDevice device) throws Exception  
  { 

  	Class btClass = Class.forName("android.bluetooth.BluetoothDevice");
  	Method removeBondMethod = btClass.getMethod("removeBond");  
  	Boolean returnValue=(Boolean) removeBondMethod.invoke(device);
  	return returnValue.booleanValue(); 
     
  } 
	

}
