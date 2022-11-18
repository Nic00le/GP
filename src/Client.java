import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class Client extends Application {
    private static Stage stg;


    private String ip;
    private int port = 123;
    public static Map<String, Object> controllers = new HashMap<String, Object>();
    //==============================Login fxml element=======================================================
    @FXML
    private Button button;
    @FXML
    private Label wrongLogin;
    @FXML
    private TextField username;
    @FXML
    private TextField password;
    //=================================================================================================
    //==============================After Login fxml element=======================================================
    @FXML
    private ScrollPane userList;
    @FXML
    public Button buttonSend;
    @FXML
    public TextArea messageBox;
    @FXML
    public ListView chatPane;
    @FXML
    public BorderPane borderPane;
    //=================================================================================================


    private Socket socket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stg = primaryStage;
        primaryStage.setResizable(false);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("loginForm.fxml"));
        loader.setController(this);
        controllers.put(this.getClass().getSimpleName(), this);
        FXMLLoader.load(getClass().getResource("loginForm.fxml"));
        Parent root = FXMLLoader.load(getClass().getResource("loginForm.fxml"));
        Scene scene = new Scene(root, 600, 450);
        primaryStage.setTitle("Welcome");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void userLogin() throws Exception {
            try {
                socket = new Socket("192.168.50.69", 123);
                String userInputName = username.getText().toString();
                String userInputPassword = password.getText().toString();
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                sendString(userInputName, out);
                sendString(userInputPassword, out);


                boolean validUser = in.readBoolean();
                System.out.println(validUser);
                //===================================JUMP TO  CHAT ROOM=================================================
                if(validUser) {
                    changeScene("Chat.fxml");
                }
                //======================================================================================================
            } catch (Exception e) {
                System.out.println("error " + e.getMessage());
            }
    }

    public void changeScene(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        stg.getScene().setRoot(root);
    }


    @FXML
    protected void initialize() throws IOException {
        //===================================get the local machine ip address==========================================
        try (final DatagramSocket socket = new DatagramSocket()) {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println(localHost.getHostAddress());
            ip = localHost.getHostAddress();
        }
//===================================get the local machine ip address==========================================
    }

    public String receiveString(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        String str = "";
        int len = in.readInt();
        while (len > 0) {
            int l = in.read(buffer, 0, Math.min(len, buffer.length));
            str += new String(buffer, 0, l);
            len -= l;
        }
        return str;
    }

    public void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
    }
    public static void print(String str) {
        System.out.print(str);
    }

    public static void println(String str) {
        System.out.println(str);
    }
}