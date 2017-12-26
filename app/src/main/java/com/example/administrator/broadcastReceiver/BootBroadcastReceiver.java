package com.example.administrator.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.administrator.activity.CameraActivity;
import com.example.administrator.activity.CameraActivity2;
import com.example.administrator.activity.SetupActivity;
import com.example.administrator.activity.main.MainActivity;


/**
 * Created by admin on 2017/11/2.
 */

public class BootBroadcastReceiver extends BroadcastReceiver{
    public static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
          if(intent.getAction().equals(ACTION)){
			  Intent in  = new Intent(context,CameraActivity2.class);
			  in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			  context.startActivity(in);
		  }
    }
}
