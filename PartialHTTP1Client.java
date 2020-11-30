import java.io.*;
import java.net.*;

public class PartialHTTP1Client {
    public static void main(String[] args) throws IOException {
        
        if (args.length != 2) {
            System.err.println(
                "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
            Socket phpSocket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(phpSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(phpSocket.getInputStream()));
        ) {
            BufferedReader stdIn =
                new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;

            while ((fromServer = in.readLine()) != null) {
                

                if(!"".equals(fromServer))
                {
                    System.out.println(fromServer);
                    
                }

                 fromUser = stdIn.readLine();
                
                if (fromUser != null) 
                    out.println(fromUser);

            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        }
    }
}