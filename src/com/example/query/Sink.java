/*******************************************************************************
 * Copyright (c) 2014 Imperial College London
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Raul Castro Fernandez - initial API and implementation
 ******************************************************************************/
package com.example.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.imperial.lsds.seep.comm.NodeManagerCommunication;
import uk.ac.imperial.lsds.seep.comm.serialization.DataTuple;
import uk.ac.imperial.lsds.seep.operator.DistributedApi;
import uk.ac.imperial.lsds.seep.operator.Operator;
import uk.ac.imperial.lsds.seep.operator.StatelessOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.example.android_seep_worker.MainActivity;
public class Sink implements StatelessOperator {
	Logger LOG = LoggerFactory.getLogger(Sink.class);
	private static final long serialVersionUID = 1L;
	Handler myHandler, myHandler2;
	DistributedApi api = new DistributedApi();

	public void setUp() {
		myHandler = MainActivity.getTextViewHandler();
		myHandler2 = MainActivity.getImageViewHandler();
		LOG.info(">>>>>>>>>>>>>>>>>>>Sink set up");
	}

	// time control variables
	long currentTime;
	
	public void processData(DataTuple dt) {
		int i = dt.getInt("value0");
		byte[] bytes = (byte[])dt.getValue("value1");
		int rows = dt.getInt("value2");
		int cols = dt.getInt("value3");
		int type = dt.getInt("value4");
		String name = dt.getString("value5");
		long timeStamp = dt.getLong("value6");
		int x = dt.getInt("value7");
		int y = dt.getInt("value8");
		int width = dt.getInt("value9");
		int height = dt.getInt("value10");
		
		currentTime = System.currentTimeMillis();
		long pastTimeMillis = currentTime - timeStamp;
		int fps = (int) Math.round((float)(1000/pastTimeMillis));
		if (fps == 0)
			fps = 1;
		LOG.info(">>>Sink receive ["+i+"] at "+currentTime);
		Message msg = myHandler.obtainMessage();
		msg.obj = name + "(" + fps + " fps)";
		myHandler.sendMessage(msg);

		Message msg2 = myHandler2.obtainMessage();
		msg2.what = 2;
		Bundle b = new Bundle(4);
		 
        //add integer data to the bundle, everyone with a key
        b.putInt("x", x);
        b.putInt("y", y);
        b.putInt("width", width);				
        b.putInt("height", height);	        
        msg2.setData(b);
		myHandler2.sendMessage(msg2);
		
	}
	
	public void processData(List<DataTuple> arg0) {
	}
	
	public void setCallbackOp(Operator op){
		this.api.setCallbackObject(op);
	}
}