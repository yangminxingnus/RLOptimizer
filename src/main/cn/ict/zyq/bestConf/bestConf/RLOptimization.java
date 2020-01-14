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
package cn.ict.zyq.bestConf.bestConf;

import cn.ict.zyq.bestConf.bestConf.optimizer.Optimization;
import cn.ict.zyq.bestConf.util.QueryProcessor;
import weka.core.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class RLOptimization implements Optimization {


	private BestConf bestconf;

	private int max_episodes, max_ep_steps, memory_capacity, batch_size;

	private double lr_a, lr_c, gamma, tau;

	///////for a resumable optimization process/////////////
	private static final String memory = "data/memory";

	public RLOptimization(BestConf bestconf) {
		this.bestconf = bestconf;
	}

	public RLOptimization(BestConf bestconf, int max_episodes, int max_ep_steps, double lr_a, double lr_c, double gamma,
						  double tau, int memory_capacity, int batch_size){
		this.bestconf = bestconf;
		this.max_episodes = max_episodes;
		this.max_ep_steps = max_ep_steps;
		this.lr_a = lr_a;
		this.lr_c = lr_c;
		this.gamma = gamma;
		this.tau = tau;
		this.memory_capacity = memory_capacity;
		this.batch_size = batch_size;
		
//		this.learner = new RLLearner(this.max_ep_steps, this.max_ep_steps, this.lr_a, this.lr_c, this.gamma, this.tau,
//				this.memory_capacity, this.batch_size);
	}

	private ArrayList<Attribute> atts=null;

	@Override
	public void optimize(String preLoadDatasetPath) {
		String sender = "";
		String data = "";
		String fileName = "config.txt";
		String fileName1 = "EAndR.txt";
		File file = null;
		File file1 = null;
		FileWriter fw = null;
		FileWriter fw1 = null;
		System.out.println("here");
		atts = bestconf.getAttributes();

		try {
			Instance defltSettings = bestconf.defltSettings.get(0);

			HashMap hm = new HashMap();
			for (int j = 0; j < defltSettings.numAttributes(); j++) {
				hm.put(defltSettings.attribute(j).name(), defltSettings.value(defltSettings.attribute(j)));
			}
			String resultString = "";

			resultString = this.bestconf.getManager().test(hm, 0, false);

			boolean flag1 = true;
			HashMap<String, Double> resultMap = new HashMap<String, Double>();

			while (flag1) {
				try {
					resultMap = QueryProcessor.processPeformance(resultString);
					flag1 = false;
				} catch (ArrayIndexOutOfBoundsException e) {
					try {
						System.out.println("1 waited");
						Thread.sleep(20000);
					} catch (InterruptedException e1) {

					}
				}
			}


			NumberFormat format = NumberFormat.getInstance();
			format.setMinimumFractionDigits(2);
			format.setGroupingUsed(false);

			file = new File("/Users/minxingyang/Desktop/" + fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			fw = new FileWriter(file);
			fw.write("a_dim" + " " + atts.size() + "\n");
			fw.write("s_dim" + " " + (resultMap.size() - 1) + "\n");

			fw.write("action and range\n");
			for (Attribute i : atts) {
				fw.write(i.name() + " " + defltSettings.toString(i)+ " " + format.format(i.getLowerNumericBound()) + " " + format.format(i.getUpperNumericBound()) + "\n");
			}
			fw.write("initial state\n");
			for (String s : resultMap.keySet()) {
				if (!s.equals("totalTime")) {
					fw.write(s + " " + format.format(resultMap.get(s)) + "\n");
				}
			}
			fw.write("reward\n");
			fw.write( "totalTime " + resultMap.get("totalTime") + "\n");
			fw.flush();
			fw.close();

			Double t0 = resultMap.get("totalTime");
			Double tl = t0;

			file1 = new File("/Users/minxingyang/Desktop/data.txt");

			/*监听端口号，只要是8888就能接收到*/
			ServerSocket ss = new ServerSocket(8888);
//			Process proc = Runtime.getRuntime().exec("python3 /Users/minxingyang/Desktop/trial.py");
//			proc.waitFor();
			System.out.println("system started");
			while (true) {
				/*实例化客户端，固定套路，通过服务端接受的对象，生成相应的客户端实例*/
				Socket socket = ss.accept();
				/*获取客户端输入流，就是请求过来的基本信息：请求头，换行符，请求体*/
				BufferedReader bd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				fw1 = new FileWriter(file1, true);
				/**
				 * 接受HTTP请求，并解析数据
				 */
				String requestHeader;
				int contentLength = 0;
				String condition = "";
				Instances nextAction = new Instances("nextAction", atts, 1);
				while ((requestHeader = bd.readLine()) != null && !requestHeader.isEmpty()) {
					System.out.println(requestHeader);
					/**
					 * 获得GET参数
					 */
					if (requestHeader.startsWith("GET")) {
						int begin = requestHeader.indexOf("/?") + 2;
						int end = requestHeader.indexOf("HTTP/");
						condition = requestHeader.substring(begin, end);
						System.out.println("GET参数是：" + condition);
						String[] actions = condition.split("&");
						Instance temp = new DenseInstance(atts.size());
						for(int i=0; i < actions.length; i++){
							fw1.write(actions[i].split("=")[1] + " ");
							temp.setValue(atts.get(i), Double.valueOf(actions[i].split("=")[1]));
						}
						nextAction.add(temp);
						temp.setDataset(nextAction);
						fw1.write("\n");
					}

					/**
					 * 获得POST参数
					 * 1.获取请求内容长度
					 */
					if (requestHeader.startsWith("Content-Length")) {
						int begin = requestHeader.indexOf("Content-Lengh:") + "Content-Length:".length();
						String postParamterLength = requestHeader.substring(begin).trim();
						contentLength = Integer.parseInt(postParamterLength);
						System.out.println("POST参数长度是：" + Integer.parseInt(postParamterLength));
					}

					if (requestHeader.startsWith("sender")) {
						sender = requestHeader.split(":")[1].trim();
					}

//					if (requestHeader.startsWith("data")) {
//						System.out.println("#####");
//						data = requestHeader.split(":")[1].trim();
//					}
//
//					System.out.println("?????");
//					System.out.println(requestHeader);

				}

				StringBuffer sb = new StringBuffer();
				if (contentLength > 0) {
					for (int i = 0; i < contentLength; i++) {
						sb.append((char) bd.read());
					}
					System.out.println("POST参数是：" + sb.toString());
				}

				if (sender.equals("java")) {
					PrintWriter pw = new PrintWriter(socket.getOutputStream());

					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-type:text/html");
					pw.println();
					pw.println(data);

					pw.flush();
					socket.close();
				} else if (sender.equals("python")) {
					PrintWriter pw = new PrintWriter(socket.getOutputStream());

					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-type:text/html");
					pw.println();

//					hm = new HashMap();
					Instance next = nextAction.get(0);
					for (int j = 0; j < next.numAttributes(); j++) {
						hm.put(next.attribute(j).name(), next.value(next.attribute(j)));
					}
					//修改

					resultString = this.bestconf.getManager().test(hm, 0, false);
					boolean flag = true;

					while (flag) {
						try {
							resultMap = QueryProcessor.processPeformance(resultString);
							flag = false;
						} catch (ArrayIndexOutOfBoundsException e) {
							try {
								System.out.println("2 waited");
								Thread.sleep(20000);
							} catch (InterruptedException e1) {

							}

						}
					}


					String stateAndPerf = "";

					for (String k : resultMap.keySet()) {
						if(!k.equals("totalTime")) {
							stateAndPerf = stateAndPerf + format.format(resultMap.get(k)) + " ";
						}
					}

					Double term1 = (-resultMap.get("totalTime") + t0) / t0;
					Double term2 = (-resultMap.get("totalTime") + tl) / tl;
					Double rr = 0.0;

					if (term1 > 0) {
						rr = ((1 + term1) * (1 + term1) - 1) * Math.abs(1 + term2);
					} else {
						rr = (-1) * ((1 - term1) * (1 - term1) - 1) * Math.abs(1 - term2);
					}

					if (rr > 0 && term2 < 0) {
						rr = 0.0;
					}

					stateAndPerf += rr;
//					stateAndPerf = stateAndPerf + " " + resultMap.get("totalTime");
					tl = resultMap.get("totalTime");
					fw1.write(stateAndPerf + " " + tl + " \n");
					fw1.close();
					pw.println(stateAndPerf);
					pw.flush();
					socket.close();
				} else {
					/*发送回执*/
					PrintWriter pw = new PrintWriter(socket.getOutputStream());

					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-type:text/html");
					pw.println();
					pw.println("<h1>successful</h1>");

					pw.flush();
					socket.close();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		System.err.println("We are ending the optimization experiments!");
		System.err.println("Please wait and don't shutdown!");
		System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

		//output the best
//		Map<Attribute,Double> attsmap = BestConf.instanceToMap(opParams.currentIns);
//		System.out.println(attsmap.toString());
//
//		//set the best configuration to the cluster
//		System.err.println("The best performance is : "+opParams.currentBest);
//
//		System.out.println("=========================================");
//		TxtFileOperation.writeToFile("bestConfOutput_RRS",attsmap.toString()+"\n");
//
//		System.out.println("=========================================");
//
//		//output the whole trainings dataset
//		try {
//			DataIOFile.saveDataToArffFile("data/trainingAllRSS.arff", bestconf.allInstances);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

}
