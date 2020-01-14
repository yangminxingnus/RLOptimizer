/**
 * Copyright (c) 2017 Institute of Computing Technology, Chinese Academy of Sciences, 2017 
 * Institute of Computing Technology, Chinese Academy of Sciences contributors. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package cn.ict.zyq.bestConf.COMT2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import weka.classifiers.trees.m5.PreConstructedLinearModel;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import com.google.common.collect.Range;

/**
 * Branch2
 *
 */

public class Branch2 {
	
	private HashMap<Attribute,Range<Double>> rangeMap;		//ranges of the attributes in the M5P tree å±žæ?åŒºé—´
	private ArrayList<PreConstructedLinearModel> linearModelList; 		//linearModels of one leaf çº¿æ?æ¨¡åž‹
	
	public Branch2() {
		rangeMap = new HashMap<Attribute,Range<Double>>();	
	}

	public Branch2(HashMap<Attribute, Range<Double>> rangeMap, PreConstructedLinearModel linearModel) {
		this.rangeMap = rangeMap;
		this.linearModelList = new ArrayList<PreConstructedLinearModel>();
		this.linearModelList.add(linearModel);
	}
	
	public Branch2(HashMap<Attribute, Range<Double>> rangeMap, ArrayList<PreConstructedLinearModel> linearModels) {
		this.rangeMap = rangeMap;
		this.linearModelList = linearModels;
	}
	
	public Instance maxPoint(Instances dataset) throws Exception{
		Instance max = new DenseInstance(dataset.numAttributes());
		max.setDataset(dataset);
		
		double[] combinedCoefs = null;
		int len=0;
		for(PreConstructedLinearModel model : linearModelList){
			//initialization
			if(combinedCoefs==null){
				len = model.coefficients().length;
				combinedCoefs = new double[len];
				for(int i=0;i<len;i++)
					combinedCoefs[i]=0;
			}
			
			for(int i=0;i<len;i++)
				combinedCoefs[i]+=model.coefficients()[i];
		}
		
		//the max value is obtained at ends of a range
		for(Map.Entry<Attribute, Range<Double>> ent: rangeMap.entrySet()){
			int attIdx = ent.getKey().index();
			if(combinedCoefs[attIdx]>0){
				//use the upper bound
				if(ent.getValue().hasUpperBound())
					max.setValue(attIdx, ent.getValue().upperEndpoint());
			}else if(combinedCoefs[attIdx]<0){
				//use the lower bound
				if(ent.getValue().hasLowerBound())
					max.setValue(attIdx, ent.getValue().lowerEndpoint());
			}
		}
		
		//now we set the predicted values
		double y = 0;
		for(PreConstructedLinearModel model : linearModelList){
			y += model.classifyInstance(max);
		}
		y /= linearModelList.size();
		max.setClassValue(y);
		
		return max;
	}

	public HashMap<Attribute, Range<Double>> getRangeMap() {
		return rangeMap;
	}

	public void setRangeMap(HashMap<Attribute, Range<Double>> rangeMap) {
		this.rangeMap = rangeMap;
	}

	public ArrayList<PreConstructedLinearModel> getLinearModels() {
		return linearModelList;
	}

	public void setLinearModels(ArrayList<PreConstructedLinearModel> linearModel) {
		this.linearModelList = linearModel;
	}

}
