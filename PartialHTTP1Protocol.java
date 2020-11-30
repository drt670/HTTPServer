import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.ParseException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PartialHTTP1Protocol {

    private static final int WAITING = 0;

    private int state = WAITING;

    public String processInput(String theInput,int port) throws IOException, InterruptedException{        
       
        HashSet<String> validMeth = new HashSet<String>();
        validMeth.add("GET");
        validMeth.add("HEAD");
        validMeth.add("POST");
        validMeth.add("PUT");
        validMeth.add("DELETE");
        validMeth.add("LINK");
        validMeth.add("UNLINK");

        HashSet<String> text = new HashSet<String>();
        text.add("html");
        text.add("plain");

        HashSet<String> image = new HashSet<String>();
        image.add("jpeg");
        image.add("png");
        image.add("gif");

        HashSet<String> application = new HashSet<String>();
        application.add("zip");
        application.add("pdf");
        application.add("gz");

        String theOutput = "";
 

        if (state == WAITING) {
            if (theInput != null) {

                String delims;
                String[] tokens;

                // parses input string to retrieve method, request URI, and HTTP version
                // handles based on single length request vs. multiple request from client
                if(theInput.contains("POST") || theInput.contains("If-Modified"))
                {
                    delims = " |\\n";
                    tokens = theInput.split(delims);
                }
                else
                {
                    delims = " ";
                    tokens = theInput.split(delims);
                }

                if(tokens[0].equals("POST"))
                {
                    if(!theInput.contains("Content-Type"))
                        return "HTTP/1.0 500 Internal Server Error\r\n";
                    if(!theInput.contains("Content-Length"))
                        return "HTTP/1.0 411 Length Required\r\n";
                }

                String ifMod = "";

                // if contains if-modified, proceeds to process request
                if(tokens[0].equals("GET") || tokens[0].equals("HEAD"))
                {
                    if(tokens.length > 3){
                        if(tokens[3].equals("If-Modified-Since:")){
                            for(int i = 4; i<tokens.length; i++){
                                ifMod = ifMod + tokens[i] + " ";                                                  
                            }
                        }                    
                        else{
                            return "HTTP/1.0 400 Bad Request\r\n";
                        }
                    }
                    else if (tokens.length != 3){
                        return "HTTP/1.0 400 Bad Request\r\n";
                    }
                }

                // split the method, request URI, HTTP version
                String method = tokens[0];
                String resource = tokens[1];
                String version = tokens[2];

                resource = resource.substring(1, resource.length());

                // get the IP address of current host server
                InetAddress ip;
                String hostIP;
                        
                ip = InetAddress.getLocalHost();
                hostIP = ip.getHostAddress();

                // if a POST request has the correct length, decodes the URI in order to pass to the CGI script
                if (tokens[0].equals("POST") && tokens.length == 13) {
                    if (tokens[12] != null) {
                        String temp = "";
                        int k;
                        for (int i = 0; i < tokens[12].length(); i++) {
                            k = i + 1;
                            if (tokens[12].charAt(i) != '!') {
                                temp = temp + tokens[12].charAt(i);
                            } else {
                                if (k < tokens[12].length()) {
                                    if (tokens[12].charAt(k) == '!' || tokens[12].charAt(k) == '*'
                                            || tokens[12].charAt(k) == '(' || tokens[12].charAt(k) == ')'
                                            || tokens[12].charAt(k) == ';' || tokens[12].charAt(k) == ':'
                                            || tokens[12].charAt(k) == '@' || tokens[12].charAt(k) == '$'
                                            || tokens[12].charAt(k) == '+' || tokens[12].charAt(k) == ','
                                            || tokens[12].charAt(k) == '/' || tokens[12].charAt(k) == '?'
                                            || tokens[12].charAt(k) == '#' || tokens[12].charAt(k) == '['
                                            || tokens[12].charAt(k) == ']' || tokens[12].charAt(k) == '\''
                                            || Character.isWhitespace(tokens[12].charAt(k))) {
                                        temp = temp + tokens[12].charAt(k);
                                        i = k;
                                    } else {
                                        temp = temp + tokens[12].charAt(i);
                                    }
                                }
                            }
                        }
                        tokens[12] = temp;
                    }
                }


                String finalLine = "";

                // If a POST request, processes CGI script
                if(tokens[0].equals("POST"))
                {
                    String resource2 = "./" + resource;
                    String resourceForScript = "/" + resource;

                    File file = new File(resource2);
                    boolean exists = file.exists();

                    if (exists == false) {
                        return theOutput = "HTTP/1.0 405 Method Not Allowed\r\n";
                    }

                    // create object Process Builder and pass CGI script as arg
                    ProcessBuilder processBuilder = new ProcessBuilder(resource2);
                    // Map containing the environment variables to pass into CGI script
                    Map<String,String> envVar = processBuilder.environment();
                    envVar.put("CONTENT_LENGTH",tokens[10]);
                    envVar.put("SCRIPT_NAME",resourceForScript);
                    envVar.put("SERVER_NAME", hostIP);
                    envVar.put("SERVER_PORT",Integer.toString(port));
                    envVar.put("HTTP_FROM",tokens[4]);
                    envVar.put("HTTP_USER_AGENT",tokens[6]);

                    // try, catch block using a input & output stream in order to send argument along with CGI script in process builder. Uses inputstreams readLine() to get the results
                    try{
                        Process process = processBuilder.start();
                        OutputStream pbArgs = process.getOutputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                        // if POST request 
                        if(tokens.length == 13)
                        {
                            pbArgs.write(tokens[12].getBytes());
                        }
                        pbArgs.close();
                        String line = "";

                        line = reader.readLine();
                        if(line == null)
                            return "HTTP/1.0 204 No Content\r\n";
                        finalLine += line;

                        while((line = reader.readLine()) != null){
                            finalLine += line;
                        }
                        reader.close();
                    }
                    catch(IOException e){
                        return "HTTP/1.0 403 Forbidden\r\n";
                    }
                }


                // parses request URI in order to determine file type of file
                String delim = "[/\\.]";
                String[] tok1 = resource.split(delim);
                String fileType = "";

                for (int i = 0; i < tok1.length; i++) {
                    if (text.contains(tok1[i])) {
                        fileType = "text/" + tok1[i];
                        break;
                    } else if (image.contains(tok1[i])) {
                        fileType = "image/" + tok1[i];
                        break;
                    } else if (application.contains(tok1[i])) {
                        fileType = "application/" + tok1[i];
                        break;
                    } else {
                        fileType = "application/octet-stream";
                    }
                }

                // parses HTTP version to check if version number is supported
                String delimiter = "/";
                String[] tok2 = version.split(delimiter);
                float f = Float.parseFloat(tok2[1]);

                if (f > 1.0 || f < 0.1)
                    return "HTTP/1.0 505 HTTP Version Not Supported\r\n";

                File file = new File(resource);
                boolean exists = file.exists();

                if(tokens[0].equals("GET") || tokens[0].equals("HEAD"))
                {
                    if (exists == false) {
                        return theOutput = "HTTP/1.0 404 Not Found\r\n";
                    }
                
                    if(!file.canRead()){
                        return theOutput = "HTTP/1.0 403 Forbidden\r\n";
                    }
                }
                
                long lastMod = file.lastModified();

                // determines the date of last modifications applied to the file
                Date dt = new Date(lastMod);
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                SimpleDateFormat expireDateFormat = new SimpleDateFormat("EEE, dd MMM 2021 HH:mm:ss zzz");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String date = dateFormat.format(dt);
                String expiresDate = expireDateFormat.format(dt);

                //switch case method that returns correct output depending on the type of method given by the client
                if (validMeth.contains(method)) {
                    switch (method) {
                        
                        case "GET":
                        if(ifMod.equals(""))
                        {
                                theOutput = "HTTP/1.0 200 OK\r\n" + "Content-Type: " + fileType + "\r\n"
                                        + "Content-Length: " + file.length() + "\r\n" + "Last-Modified: " + date
                                        + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Allow: GET, POST, HEAD"
                                        + "\r\n" + "Expires: " + expiresDate + "\r\n\r\n";
                            break;
                        }
                        else if(!ifMod.equals("")){
                        try{
                            Date ifModDate = dateFormat.parse(ifMod);

                            if(ifModDate.compareTo(dt) >= 0){
                                return theOutput = "HTTP/1.0 304 Not Modified"  + "\r\n" + "Expires: " + expiresDate + "\r\n";
                            }
                            else{
                                theOutput = "HTTP/1.0 200 OK\r\n" + "Content-Type: " + fileType + "\r\n"
                                        + "Content-Length: " + file.length() + "\r\n" + "Last-Modified: " + date
                                        + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Allow: GET, POST, HEAD"
                                        + "\r\n" + "Expires: " + expiresDate + "\r\n\r\n";
                            }
                            break;
                        }
                        catch(ParseException e){
                             theOutput = "HTTP/1.0 200 OK\r\n" + "Content-Type: " + fileType + "\r\n"
                                        + "Content-Length: " + file.length() + "\r\n" + "Last-Modified: " + date
                                        + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Allow: GET, POST, HEAD"
                                        + "\r\n" + "Expires: " + expiresDate + "\r\n\r\n";
                        }
                            break;
                    }

                        case "POST":
                        theOutput = "HTTP/1.0 200 OK\r\n" + "Content-Type: text/html\r\n"
                                        + "Content-Length: " + finalLine.length() + "\r\n" + "Last-Modified: " + date
                                        + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Allow: GET, POST, HEAD"
                                        + "\r\n" + "Expires: " + expiresDate + "\r\n\r\n" + finalLine;
                            break;
                        case "HEAD":
                            theOutput = "HTTP/1.0 200 OK\r\n" + "Content-Type: " + fileType + "\r\n"
                                    + "Content-Length: " + file.length() + "\r\n" + "Last-Modified: " + date + "\r\n"
                                    + "Content-Encoding: identity" + "\r\n" + "Allow: GET, POST, HEAD" + "\r\n"
                                    + "Expires: " + expiresDate + "\r\n";
                            break;
                        default:
                            theOutput = "HTTP/1.0 501 Not Implemented\r\n";
                    }
                } else
                    theOutput = "HTTP/1.0 400 Bad Request\r\n";

            }
        }

        return theOutput;
    }
}