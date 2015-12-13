/*
 Copyright (c) 2010-2012 Nathan Rajlich
 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
 */
//Copyright notice for websocket api used for purpose of this class
package middleserver;

//imports and stuff
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import javax.websocket.EncodeException;
import java.util.concurrent.*;
/**
 *
 * @author Matthew Bulat
 */
//endpoint annotation
@ServerEndpoint("/start")
public class MiddleServer {

    private WebSocketClient mWebSocketClient;
    private final Message1 serial = new Message1();
    private final Enc_Dec secret = new Enc_Dec();
    private Session x;
    private final Logging log = new Logging();
    private ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();
    private final LoginCheck loginCheck = new LoginCheck();
    private final AdminCheck adminCheck = new AdminCheck();
    private final SystemConfiguration data = new SystemConfiguration();

    //method used for purpose of opening new sessions
    //and checking if the databases are running fine

    @OnOpen
    public void onOpen(Session session) {
        try {
            session.getBasicRemote().sendText(data.check());
        } catch (IOException e) {
            log.writeLog(" " + e);
        }
        log.writeLog(" " + session.getId() + " has opened a connection");
    }

    //on message method which uses string for communication mainly webpage
    //when first connected it will check if admins credentials are correct
    //then it uses switch to pick the right action

    @OnMessage
    public void onMessage(String message, Session session) {
        String[] splitter = message.split(",");
        int splitterLength = splitter.length;
        boolean confirmCredential = false;

        try {
            //checks if admins credentials are correct
            if (!(splitter[0].equals("Login") || splitter[0].equals("reLogin") || splitter[0].equals("config") || splitter[0].equals("newAdminCredentials"))) {
                log.writeLog(" " + "Confirming user " + splitter[splitterLength - 2] + " ,token with database");
                String[] confirm = new String[2];
                confirm[0] = splitter[splitterLength - 2];
                confirm[1] = splitter[splitterLength - 1];
                String check = adminCheck.confirmAdmin(confirm);
                switch (check) {
                    case "proceed":
                        confirmCredential = true;
                        break;
                    case "AuthFailed":
                        session.getBasicRemote().sendText(check);
                        confirmCredential = false;
                        break;
                }
            } else {
                confirmCredential = true;
            }
        } catch (IOException e) {
            log.writeLog("Socket Input/Output exception when admin was login " + e);
        }
        if (confirmCredential) {
            //switch which checks request from webpage    
            switch (splitter[0]) {
                //Login request which confirms admins credentials
                case "Login":
                    try {
                        session.getBasicRemote().sendText(adminCheck.adminLogin(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when admin was login " + e);
                    }
                    break;
                //Search request which searches for user
                case "Search":
                    try {
                        session.getBasicRemote().sendText(loginCheck.search(splitter[1]));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when admin was searching " + e);
                    }
                    break;
                //userUpdate request which updates user credentials
                case "userUpdate":
                    try {
                        session.getBasicRemote().sendText(loginCheck.userUpdate(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when user was being updated " + e);
                    }
                    break;
                //reLogin request for when server losses websocket connection
                case "reLogin":
                    try {
                        session.getBasicRemote().sendText("re" + adminCheck.adminLogin(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when Admin was re-login " + e);
                    }
                    break;
                //Purpose of this case is to add new user into the system
                case "newUser":
                    try {
                        session.getBasicRemote().sendText(loginCheck.addUser(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when new user was being added " + e);
                    }
                    break;
                //removeUser magicaly, fantastically, awesomely... removes user from system, I know right?!
                case "removeUser":
                    try {
                        session.getBasicRemote().sendText("re" + loginCheck.removeUser(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when user was removed " + e);
                    }
                    break;
                //showAll request which provides all users from database
                case "showAll":
                    try {
                        session.getBasicRemote().sendText(loginCheck.showAll());
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when admin was requesting all users in the system " + e);
                    }
                    break;
                //updateAdminPassword request which updates admin's password
                case "updateAdminPassword":
                    try {
                        session.getBasicRemote().sendText(adminCheck.passwordUpdate(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when admin was updating his password " + e);
                    }
                    break;
                //config request which provides system config to the website
                case "config":
                    try {
                        session.getBasicRemote().sendText(data.initialWriteConfigArray(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when system config was requested by the website " + e);
                    }
                    break;
                case "getUserDatabaseFields":
                    try {
                        session.getBasicRemote().sendText(data.getUserDatabaseCredentials("dataWithoutPassword"));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when admin database credentials without of password were requested " + e);
                    }
                    break;
                case "getAdminDatabaseFields":
                    try {
                        session.getBasicRemote().sendText(data.getAdminDatabaseCredentials("dataWithoutPassword"));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when admin database credentials without of password were requested " + e);
                    }
                    break;
                case "userDatabaseCredentialsUpdate":
                    try {
                        session.getBasicRemote().sendText(data.setUserDatabaseCredentials(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when database credentials were updated " + e);
                    }
                    break;
                case "updateUserDatabaseForSure":
                    try {
                        session.getBasicRemote().sendText(data.setUserDatabaseCredentials(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when updating user database " + e);
                    }
                    break;
                case "adminDatabaseCredentialsUpdate":
                    try {
                        session.getBasicRemote().sendText(data.setAdminDatabaseCredentials(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when updating admin database credentials " + e);
                    }
                    break;
                case "updateAdminDatabaseForSure":
                    try {
                        session.getBasicRemote().sendText(data.setAdminDatabaseCredentials(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when updating admin database credentials " + e);
                    }
                    break;
                case "newAdminCredentials":
                    try {
                        session.getBasicRemote().sendText(adminCheck.newAdmin(splitter));
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception when providing  new admin credentials " + e);
                    }
                    break;
                default:
                //implement default responce to the client
            }
            confirmCredential = false;
        }

    }

    @OnMessage
    public void onMessage(byte[] message, Session session) throws InterruptedException {
        URI uri = null;
        String hostIP = null;
        x = session;
        byte[] data = secret.Decrypt(message);
        String req = serial.getRequest(data);

        LoginCheck login = new LoginCheck();
        boolean confirmCredential = false;
        String[] confirm = new String[2];
        if (!(req.equals("login") || (req.equals("autolog")))) {
            confirm[0] = serial.getEmail(data);
            confirm[1] = serial.getToken(data);
            boolean confirmUser = login.confirmUser(confirm);
            confirmCredential = confirmUser;
        } else {
            confirmCredential = true;
        }
        if (!confirmCredential) {
            serial.setRequest("*#check_failed#*");
            reply(session);
        } else {
            String da = serial.getData(data);
            String[] splitter;
            switch (req) {
                case "login":
                    splitter = da.split(":");
                    log.writeLog("Loggin request received from " + splitter[0]);

                    String[] reply = login.login(splitter);
                    switch (reply[0]) {
                        case "#*try_again_user*#":
                            log.writeLog("Incorrect Loggin ID");
                            serial.setRequest(reply[0]);
                            reply(session);
                            break;
                        case "#*try_again*#":
                            log.writeLog("Login Failed, User " + splitter[0] + " asked for password again");
                            serial.setRequest(reply[0]);
                            serial.setData(reply[2]);
                            reply(session);
                            break;
                        case "#*blocked*#":
                            serial.setRequest(reply[0]);
                            log.writeLog("Account" + splitter[0] + "is locked and is still requesting to login");
                            reply(session);
                            break;
                        case "#*acc_blocked*#":
                            serial.setRequest(reply[0]);
                            log.writeLog("Acount " + splitter[0] + " was blocked message sent to user:");
                            reply(session);
                            break;
                        case "#*new_token*#":
                            serial.setRequest(reply[0]);
                            serial.setToken(reply[1]);
                            log.writeLog("Login successful , User " + splitter[0] + " provided with new token");
                            reply(session);
                            break;
                        case "#*ok*#":
                            serial.setRequest(reply[0]);
                            serial.setToken(reply[1]);
                            serial.setData(reply[1]);
                            log.writeLog("Login successful , User " + splitter[0] + " provided with old token");
                            reply(session);
                            break;
                    }
                    break;
                case "autolog":
                    splitter = da.split(":");
                    log.writeLog("Loggin request received from " + splitter[0]);
                    String[] autologdata = {serial.getAutoEmail(), serial.getAutoToken(data)};
                    String check = login.getLogin(autologdata);
                    switch (check) {
                        case "#*ok*#":
                            serial.setRequest(check);
                            log.writeLog("AutoLogin successful, User " + splitter[0] + " can proceed");
                            reply(session);
                            break;
                        case "#*failed*#":
                            serial.setRequest(check);
                            log.writeLog("AutoLogin unsuccessful , User " + splitter[0] + " asked to login manually ");
                            reply(session);
                            break;
                    }
                    break;
                case "HostAvailability":
                    hostIP = da;
                    log.writeLog("Checking host avaiablity " + hostIP + " for user " + confirm[0]);
                    try {
                        uri = new URI("ws://" + hostIP + ":8080/LocalServer/data");
                    } catch (URISyntaxException e) {
                        log.writeLog("URI exception on HostAvailability case " + hostIP + " " + e);
                    }
                    mWebSocketClient = new WebSocketClient(uri) {
                        @Override
                        public void onOpen(ServerHandshake serverHandshake) {//Open Socket
                        }

                        @Override
                        public void onClose(int i, String s, boolean b) {//close Socket
                        }

                        @Override
                        public void onError(Exception e) {//Error Socket
                            log.writeLog("onError HostAvaibility " + e);
                        }

                        @Override
                        public void onMessage(ByteBuffer x) {//message Socket
                        }

                        @Override
                        public void onMessage(String x) {//message Socket
                        }
                    };
                    mWebSocketClient.connect();
                    int testLocalServerConnectionState = mWebSocketClient.getReadyState();
                    int waiting = 0;
                    while (waiting != 1000 && testLocalServerConnectionState != 1) {
                        waiting += 100;
                        testLocalServerConnectionState = mWebSocketClient.getReadyState();
                        Thread.sleep(waiting);
                    }
                    if (testLocalServerConnectionState != 1) {
                        log.writeLog("Host " + hostIP + " not responding");
                        serial.setRequest("HostNotResponding");
                        mWebSocketClient.close();
                        reply(session);
                    } else {
                        log.writeLog("Host " + hostIP + " responding");
                        serial.setRequest("HostResponding");
                        mWebSocketClient.close();
                        reply(session);
                    }
                    break;
                case "hostData":
                    hostIP = da;
                    log.writeLog("Request LocalServer data for host " + hostIP + " from user " + confirm[0]);
                    try {
                        uri = new URI("ws://" + hostIP + ":8080/LocalServer/data");
                    } catch (URISyntaxException e) {
                        log.writeLog("URI exception on hostData case " + hostIP + " " + e);
                    }
                    mWebSocketClient = new WebSocketClient(uri) {
                        @Override
                        public void onOpen(ServerHandshake serverHandshake) {//Open Socket
                        }

                        @Override
                        public void onClose(int i, String s, boolean b) {//close Socket
                        }

                        @Override
                        public void onError(Exception e) {//Error Socket
                            log.writeLog("onError hostData " + e);
                        }

                        @Override
                        public void onMessage(ByteBuffer x) {//message Socket
                            queue.add(secret.Encrypt(x.array()));
                        }

                        @Override
                        public void onMessage(String x) {//message Socket
                        }
                    };
                    mWebSocketClient.connect();
                    testLocalServerConnectionState = mWebSocketClient.getReadyState();
                    while (testLocalServerConnectionState != 1) {
                        testLocalServerConnectionState = mWebSocketClient.getReadyState();
                    }
                    new Thread(new Message()).start();
                    break;
                case "stopConnection":
                    mWebSocketClient.send("close".getBytes());
                    try {
                        session.close();
                    } catch (IOException e) {
                        log.writeLog("Socket Input/Output exception on stopConnection " + e);
                    }
                    break;
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.writeLog("Session " + session.getId() + " has ended");
    }

    public void reply(Session session) {
        try {
            session.getBasicRemote().sendObject(secret.Encrypt(serial.getBytes()));
        } catch (IOException | EncodeException e) {
            log.writeLog("Socket Input/Output exception or EncodeException on reply method " + e);
        }
    }
//new thread used for purpose of sending data on seperate thread

    class Message implements Runnable {

        @Override
        public void run() {
            try {
                for (Session sess : x.getOpenSessions()) {
                    if (sess.isOpen()) {
                        mWebSocketClient.send("data".getBytes());
                    }
                    while (sess.isOpen()) {
                        if ((x.isOpen()) && (!queue.isEmpty())) {
                            x.getBasicRemote().sendObject(queue.poll());
                        } else {
                            Thread.sleep(500);
                        }
                    }
                }
            } catch (InterruptedException | IOException | EncodeException e) {//find correct exception
                log.writeLog("Message thread exception " + e);
            }
        }
    }

}
