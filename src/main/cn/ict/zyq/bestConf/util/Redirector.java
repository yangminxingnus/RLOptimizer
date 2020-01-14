package cn.ict.zyq.bestConf.util;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;


public class Redirector implements Runnable {

    private int port;
    private String newSite;

    public Redirector(String site, int port){
        this.port=port;
        this.newSite=site;
    }

    @Override
    public void run() {
        try {
            ServerSocket server=new ServerSocket(port);
            System.out.println("Redirecting connection on port"
                    +server.getLocalPort()+" to "+newSite);

            while (true) {
                try {
                    Socket socket=server.accept();
                    Thread thread=new RedirectThread(socket);
                    thread.start();
                } catch (IOException e) {
                    // TODO: handle exception
                }
            }
        } catch (BindException e) {
            System.err.println("Could not start server. Port Occupied");
        }catch (IOException e) {
            System.err.println(e);
        }

    }

    class RedirectThread extends Thread {

        private Socket connection;

        RedirectThread(Socket s) {
            this.connection=s;
        }

        public void run() {
            try {
                Writer out=new BufferedWriter(
                        new OutputStreamWriter(connection.getOutputStream(),"ASCII"));
                Reader in=new InputStreamReader(
                        new BufferedInputStream(connection.getInputStream()));

                StringBuffer request=new StringBuffer(80);
                while (true) {
                    int c=in.read();
                    if (c=='\t'||c=='\n'||c==-1) {
                        break;
                    }
                    request.append((char)c);
                }

                String get=request.toString();
                int firstSpace=get.indexOf(' ');
                int secondSpace=get.indexOf(' ', firstSpace+1);
                String theFile=get.substring(firstSpace+1, secondSpace);

                if (get.indexOf("HTTP")!=-1) {
                    out.write("HTTP/1.0 302 FOUND\r\n");
                    Date now=new Date();
                    out.write("Date: "+now+"\r\n");
                    out.write("Server: Redirector 1.0\r\n");
                    out.write("Location: "+newSite+theFile+"\r\n");
                    out.write("Content-Type: text/html\r\n\r\n");
                    out.flush();
                }

                //并非所有的浏览器都支持重定向，
                //所以我们需要生成一个适用于所有浏览器的HTML文件，来描述这一行为
                out.write("<HTML><HEAD><TITLE>Document moved</TITLE></HEAD>\r\n");
                out.write("<BODY><H1>Document moved</H1></BODY>\r\n");
                out.write("The document "+theFile
                        +" has moved to \r\n<A HREF=\""+newSite+theFile+"\">"
                        +newSite+theFile
                        +"</A>.\r\n Please update your bookmarks");
                out.write("</BODY></HTML>\r\n");
                out.flush();
            } catch (IOException e) {
            }finally{
                try {
                    if (connection!=null) {
                        connection.close();
                    }
                } catch (IOException e2) {

                }
            }
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        int thePort;
        String theSite;

        try {
            theSite=args[0];

            //如果结尾有'/'，则去除
            if (theSite.endsWith("/")) {
                theSite=theSite.substring(0,theSite.length()-1);
            }
        } catch (Exception e) {
            System.out.println("Usage: java Redirector http://www.newsite.com/ port");
            return;
        }

        try {
            thePort=Integer.parseInt(args[1]);
        } catch (Exception e) {
            thePort=80;
        }

        Thread t=new Thread(new Redirector(theSite, thePort));
        t.start();

    }

}

