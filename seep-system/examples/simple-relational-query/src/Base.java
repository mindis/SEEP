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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import operators.Processor;
import operators.Sink;
import operators.Source;
import uk.ac.imperial.lsds.seep.api.QueryBuilder;
import uk.ac.imperial.lsds.seep.api.QueryComposer;
import uk.ac.imperial.lsds.seep.api.QueryPlan;
import uk.ac.imperial.lsds.seep.operator.Connectable;
import uk.ac.imperial.lsds.seep.operator.compose.LocalConnectable;
import uk.ac.imperial.lsds.seep.operator.compose.MicroOperator;
import uk.ac.imperial.lsds.seep.operator.compose.StatelessMicroOperator;
import uk.ac.imperial.lsds.seep.operator.compose.SubOperator;

public class Base implements QueryComposer{

	public QueryPlan compose() {
		
		// Declare Source
		ArrayList<String> srcFields = new ArrayList<String>();
		srcFields.add("value1");
		srcFields.add("value2");
		srcFields.add("value3");
		Connectable src = QueryBuilder.newStatelessSource(new Source(), -1, srcFields);
			
		// Declare sink
		ArrayList<String> snkFields = new ArrayList<String>();
		snkFields.add("value1");
		snkFields.add("value2");
		Connectable snk = QueryBuilder.newStatelessSink(new Sink(), -2, snkFields);

		// Micro ops for first subquery
		MicroOperatorConnectable mOp1 = QueryBuilder.newMicroOperatorConnectable(null, 1, null);
		MicroOperatorConnectable mOp2 = QueryBuilder.newMicroOperatorConnectable(null, 1, null);
		mOp1.connectTo(mOp2);
		
		// Micro ops for second subquery
		MicroOperatorConnectable mOp3 = QueryBuilder.newMicroOperatorConnectable(null, 1, null);
		MicroOperatorConnectable mOp4 = QueryBuilder.newMicroOperatorConnectable(null, 1, null);
		mOp3.connectTo(mOp4);
		
		// Create subqueries
		Set<MicroOperatorConnectable> microOpConnectables = new HashSet<>();
		microOpConnectables.add(c1);
		microOpConnectables.add(c2);		
		
		QueryBuilder.newMultiOperator(connectors, 1, srcFields);

		
		// Connect subqueries

		LocalConnectable c1 = QueryBuilder.newStatelessMicroOperator(new Sink(), 1, null);
		LocalConnectable c2 = QueryBuilder.newStatelessMicroOperator(new Sink(), 2, null);
		
		c1.connectSubOperatorTo(1, c2);
		
		
		Set<LocalConnectable> connectors = new HashSet<>();
		connectors.add(c1);
		connectors.add(c2);
		
		QueryBuilder.newMultiOperator(connectors, 1, srcFields);

		
		
		src.connectSubOperatorTo(0, p);
		p.connectSubOperatorTo(0, snk);
		
		Set<SubOperator> subs = new HashSet<>();
		subs.add(src);
		subs.add(p);
		subs.add(snk);
		
		QueryBuilder.newMultiOperator(subs, 1, srcFields);
		
		

		
		/** Connect operators **/
		src.connectTo(p, true, 0);
//		p.connectTo(snk, true, 0);
		
		return QueryBuilder.build();
	}
}
