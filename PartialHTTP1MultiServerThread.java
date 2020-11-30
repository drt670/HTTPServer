import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.nio.file.*;

public class PartialHTTP1MultiServerThread extends Thread {
    private Socket socket = null;

    public PartialHTTP1MultiServerThread(Socket socket) {
        super("PartialHTTP1MultiServerThread");
        this.socket = socket;
    }

    @Override
    public void run() {

        try (DataOutputStream  out = new DataOutputStream(socket.getOutputStream());
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());) {
            String inputLine = "", outputLine;
            PartialHTTP1Protocol php = new PartialHTTP1Protocol();
            try{
            socket.setSoTimeout(5000);

                while(in.available()>0) {
         
                    // read the byte and convert the integer to character
                    char c = (char)in.read();
        
                    // print the characters
                   inputLine += c;
                 }

                int port = socket.getLocalPort();

                // sends the input line request from the client in order to process the correct output
                outputLine = php.processInput(inputLine,port);
               
                if (!outputLine.equals("")) {
                    
                    out.writeBytes(outputLine);
                    
                }

                // if GET or HEAD method, parses out the resource location in order to correctly send out the payload.
                if(inputLine.contains("GET") || inputLine.contains("HEAD"))
            {
                String delims = " ";
                String[] tokens = inputLine.split(delims);
                
                String resource = tokens[1];
                resource = resource.substring(1, resource.length());
                File file = new File(resource);
                if(file.exists() && file.canRead()){
                byte[] bs = Files.readAllBytes(file.toPath());
                out.write(bs);
                }
            }
                
            // immediately flushes and closes the output and input streams respectively
            out.flush();
            Thread.sleep(250);
            out.close();
            in.close();
            socket.close();
        
        }catch (SocketTimeoutException e) {
            out.writeBytes("HTTP/1.0 408 Request Timeout\r\n");
            out.flush();
            Thread.sleep(250);
            out.close();
            socket.close();
        }
        
        }catch (IOException e) {
            e.printStackTrace();
        }catch (InterruptedException e){
           e.printStackTrace();
        }
    }
}