import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class PartialHTTP1Server {
    public static void main(String[] args) throws IOException {

    if (args.length != 1) {
        System.err.println("Usage: java PartialHTTP1Server <port number>");
        System.exit(1);
    }

        int portNumber = Integer.parseInt(args[0]);
        boolean listening = true;
        int threadCount = 0;
        
        //while loop enclosed in a try catch block to accept incoming connections onto server socket
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            ExecutorService threadPool = new ThreadPoolExecutor(5, 50, 5000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            while (listening) {
                threadPool.execute(new PartialHTTP1MultiServerThread(serverSocket.accept()));
	        }
	    } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }
    }
}