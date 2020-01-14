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
package cn.ict.zyq.bestConf.cluster.InterfaceImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

import ch.ethz.ssh2.Connection;
import cn.ict.zyq.bestConf.cluster.Interface.ConfigWrite;
import cn.ict.zyq.bestConf.cluster.Utils.PropertiesUtil;
import cn.ict.zyq.bestConf.cluster.Utils.SFTPUtil;

public class MySQLConfigWrite implements ConfigWrite {
	
	private Connection connection;
	private String server;
	private String username;
	private String password;
	private String remotePath;
	private String localPath;
	private String mysqltargetfilePath = "data/my.cnf";
	private String remoteconffilename = "my.cnf";
	
	public Connection getConnection() {
		try {
			connection = new Connection(server);
			connection.connect();
			boolean isAuthenticated = connection.authenticateWithPassword(
					username, password);
			if (isAuthenticated == false) {
				throw new IOException("Authentication failed...");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return connection;
	}

	public void closeConnection() {
		try {
			if (connection.connect() != null) {
				connection.close();
			}
		} catch (IOException e) {

		} finally {
			connection.close();
		}
	}
	
	@Override
	public void initial(String server, String username, String password, String localPath, String remotePath) {
		// TODO Auto-generated method stub
		this.server = server;
		this.username = username;
		this.password = password;
		this.localPath = localPath;
		this.remotePath = remotePath;
	}

	public void uploadConfigFile() {
		ChannelSftp sftp = null;
		Session session;
		Channel channel;
		session = null;
		channel = null;
		try {
			removeRemoteConfigFile(remoteconffilename);
			session = SFTPUtil.connect(server, 22, username, password);
			channel = session.openChannel("sftp");
			channel.connect();
			sftp = (ChannelSftp) channel;
			SFTPUtil.upload(remotePath, localPath + "/" + remoteconffilename, sftp);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sftp != null)
				sftp.disconnect();
			if (session != null)
				session.disconnect();
		}
	}

	public void removeRemoteConfigFile(String filename) {
		String cmdRemove = "cd " + remotePath + "; rm -f " + filename;
		try {
			ch.ethz.ssh2.Session session = this.getConnection().openSession();
			session.execCommand(cmdRemove);
			System.out.println("Here is SUT start information:");
			System.out.println("Configuration file had been successfully removed！");
			if (session != null)
				session.close();
			closeConnection();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to remove configuration file！");
		}
	}

	@Override
	public void writetoConfigfile(HashMap hm) {
		// TODO Auto-generated method stub
		File file = new File(mysqltargetfilePath);
		// if file doesn't exist, then create it
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		FileWriter fw = null;
		try {
			fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
		
			Iterator it_client=hm.entrySet().iterator();
			bw.write("#\n" +
					"# The MySQL database server configuration file.\n" +
					"#\n" +
					"# You can copy this to one of:\n" +
					"# - \"/etc/mysql/my.cnf\" to set global options,\n" +
					"# - \"~/.my.cnf\" to set user-specific options.\n" +
					"# \n" +
					"# One can use all long options that the program supports.\n" +
					"# Run program with --help to get a list of available options and with\n" +
					"# --print-defaults to see which it would actually understand and use.\n" +
					"#\n" +
					"# For explanations see\n" +
					"# http://dev.mysql.com/doc/mysql/en/server-system-variables.html\n" +
					"\n" +
					"#\n" +
					"# * IMPORTANT: Additional settings that can override those from this file!\n" +
					"#   The files must end with '.cnf', otherwise they'll be ignored.\n" +
					"#\n" +
					"\n" +
					"!includedir /etc/mysql/conf.d/\n" +
					"!includedir /etc/mysql/mysql.conf.d/\n" +
					"\n" +
					"[mysqld]\n" +
					"sql_mode=NO_ENGINE_SUBSTITUTION\n" +
					"performance_schema=ON\n" +
					"general_log_file        = /var/log/mysql/mysql.log\n" +
					"general_log             = 1\n");

			while(it_client.hasNext()) {
				Map.Entry entry = (Map.Entry) it_client.next();
				String key = entry.getKey().toString();
				String value = entry.getValue().toString();
				bw.write(key + " = " + value + "\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
