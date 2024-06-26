
package com.raven.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.raven.model.Model_Client;
import com.raven.model.Model_Login;
import com.raven.model.Model_Message;
import com.raven.model.Model_Receive_Message;
import com.raven.model.Model_Register;
import com.raven.model.Model_Send_Message;
import com.raven.model.Model_User_Account;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;

public class Service {

    private static Service instance;
    private SocketIOServer server;
    private ServiceUser serviceUser;
    private List<Model_Client> listClient;
    private  JTextArea textArea;
    private final int PORT_NUMBER = 9999;

    public static Service getInstance(JTextArea textArea) {
        if (instance == null) {
            instance = new Service(textArea);
        }
        return instance;
    }

    private Service(JTextArea textArea) {
        this.textArea = textArea;
        serviceUser = new ServiceUser();
        listClient = new ArrayList<>();
    }

    public void startServer() {
        Configuration config = new Configuration();
        config.setPort(PORT_NUMBER);
        server = new SocketIOServer(config);
        server.addConnectListener((SocketIOClient sioc) -> {
            textArea.append("One client connected\n");
        });
        server.addEventListener("register", Model_Register.class, (SocketIOClient sioc, Model_Register t, AckRequest ar) -> {
            Model_Message message = serviceUser.register(t);
            ar.sendAckData(message.isAction(), message.getMessage(), message.getData());
            if (message.isAction()) {
                textArea.append("User has Register :" + t.getUserName() + " Pass :" + t.getPassword() + "\n");
                server.getBroadcastOperations().sendEvent("list_user", (Model_User_Account) message.getData());
                addClient(sioc, (Model_User_Account) message.getData());
            }
        });
        server.addEventListener("login", Model_Login.class, (SocketIOClient sioc, Model_Login t, AckRequest ar) -> {
            Model_User_Account login = serviceUser.login(t);
            if (login != null) {
                ar.sendAckData(true, login);
                addClient(sioc, login);
                userConnect(login.getUserID());
            } else {
                ar.sendAckData(false);
            }
        });
        server.addEventListener("list_user", Integer.class, (SocketIOClient sioc, Integer userID, AckRequest ar) -> {
            try {
                List<Model_User_Account> list = serviceUser.getUser(userID);
                sioc.sendEvent("list_user", list.toArray());
            } catch (SQLException e) {
                System.err.println(e);
            }
        });
        server.addEventListener("send_to_user", Model_Send_Message.class, (SocketIOClient sioc, Model_Send_Message t, AckRequest ar) -> {
            sendToClient(t);
        });
        server.addDisconnectListener((SocketIOClient sioc) -> {
            int userID = removeClient(sioc);
            if (userID != 0) {
                //  removed
                userDisconnect(userID);
            }
        });
        server.start();
        textArea.append("Server has Start on port : " + PORT_NUMBER + "\n");
    }

    private void userConnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, true);
    }

    private void userDisconnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, false);
    }

    private void addClient(SocketIOClient client, Model_User_Account user) {
        listClient.add(new Model_Client(client, user));
    }

    private void sendToClient(Model_Send_Message data) {
        for (Model_Client c : listClient) {
            if (c.getUser().getUserID() == data.getToUserID()) {
                c.getClient().sendEvent("receive_ms", new Model_Receive_Message(data.getMessageType(), data.getFromUserID(), data.getText()));
                break;
            }
        }
    }

    public int removeClient(SocketIOClient client) {
        for (Model_Client d : listClient) {
            if (d.getClient() == client) {
                listClient.remove(d);
                return d.getUser().getUserID();
            }
        }
        return 0;
    }

    public List<Model_Client> getListClient() {
        return listClient;
    }
}