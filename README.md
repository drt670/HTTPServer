# HTTPServer

Description:

Functional HTTP Server/Client programs written in Java. Utilizes sockets for communication between client and server. Supports the usage of GET, HEAD and POST commands with MIME types. Available implementation of CGI scripts as well. Includes a tester.jar file to run multiple test cases, showcasing its ability to perform all commands.

----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

How to Run:

1. Store all files in same directory, and open terminal within that directory
2. Compile all java files with "javac *.java"
3. Create new port and initiate server connection with "java -cp . PartialHTTP1Server 3456"
4. Run tester through jar file with same port number and name of server host ip using "java -jar HTTPServerTester.jar localhost 3456"
