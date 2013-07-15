/*******************************************************************************
 * Copyright (c) 2013 Raul Castro Fernandez (Ra).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Ra - Design and initial implementation
 ******************************************************************************/
package uk.co.imperial.lsds.seep.operator;

import java.util.ArrayList;

import uk.co.imperial.lsds.seep.comm.serialization.DataTuple;

public interface AfterBarrierProcessing {

	public void setUp();
	
	public void processData(ArrayList<DataTuple> ldt);
}