import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Server {
    private HashMap<String, String> UserInfo = new HashMap<String, String>();
    private HashMap<String, Socket> socketList = new HashMap<String, Socket>();
    private HashMap<String, String> groupList = new HashMap<String, String>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public Server(int port) throws IOException {
        this.UserInfo = HashMapFromTextFile("UserInfo");
        this.groupList = HashMapFromTextFile("groupList");
        ServerSocket srvSocket = new ServerSocket(port);
        while(true) {
            print("Listening at port %d...\n", port);
            Socket clientSocket = srvSocket.accept();
            String userId = Login(clientSocket);

            synchronized (socketList) {
                socketList.put(userId, clientSocket);
                System.out.println(socketList);
            }

            Thread t = new Thread(()-> {
                try {
                    serve(userId, clientSocket);
                } catch (IOException ex) {
                    print("Connection drop!");
                    synchronized (socketList) {
                        socketList.remove(userId);
                        System.out.println(socketList);
                    }
                }
            });

            t.start();
        }
    }

    private void serve(String userId, Socket clientSocket) throws IOException {
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());

        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

        while(true) {
            String msg = receiveString(in);
            System.out.println(msg);

            // break the string into message and recipient part
            StringTokenizer str = new StringTokenizer(msg, ":");
            String receiverId = str.nextToken();
            String text = userId + ":" + str.nextToken();

            if(UserInfo.containsKey(receiverId)){
                forwardSingle(receiverId, text);
            }else if(groupList.containsKey(receiverId)){
                forwardGroup(receiverId, text);
            }
        }
    }

    public void forwardSingle(String receiverId, String msg) {
        // search for the recipient in the connected devices list.
        // ar is the vector storing client of active users
        synchronized (socketList) {
            try{
                DataOutputStream toReceiver = new DataOutputStream(socketList.get(receiverId).getOutputStream());
                sendString(msg, toReceiver);
            } catch (IOException ex) {
                print("Unable to forward message to %s:%d\n",
                        socketList.get(receiverId).getInetAddress().getHostName(), socketList.get(receiverId).getPort());
            }
        }
    }

    public void forwardGroup(String group, String msg) throws IOException {
        String members = groupList.get(group);
        String [] member = members.split(",");
        synchronized (socketList) {
            for (int i = 0; i < member.length; i++) {
                try {
                    if (UserInfo.containsKey(member[i])) {
                        if (socketList.containsKey(member[i])) {
                            DataOutputStream toReceiver = new DataOutputStream(socketList.get(member[i]).getOutputStream());
                            sendString(msg, toReceiver);
                        }
                    }
                } catch (IOException ex) {
                    print("Unable to forward message to %s:%d\n",
                            socketList.get(member[i]).getInetAddress().getHostName(), socketList.get(member[i]).getPort());
                }
            }
        }
    }

//    private void forward(String msg){
//        synchronized (socketList) {
//            for (Socket socket : socketList) {
//                try {
//                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    out.writeInt(msg.length());
//                    out.write(msg.getBytes(), 0, msg.length());
//                } catch (IOException ex) {
//                    print("Unable to forward message to %s:%d\n",
//                            socket.getInetAddress().getHostName(), socket.getPort());
//                }
//            }
//        }
//    }

    private String Login(Socket clientSocket) throws IOException {
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        String userId ="";
        String userPw ="";
        boolean valid = false;
        while(!valid) {
            System.out.println("abc");
            userId = receiveString(in);
            userPw = receiveString(in);
            if (!socketList.containsKey(userId) && UserInfo.containsKey(userId)) {
                valid = UserInfo.get(userId).equals(userPw);
            }
            out.writeBoolean(valid);
        }
        return userId;
    }

    public void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
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

    public static HashMap<String, String> HashMapFromTextFile(String filename)
    {
        HashMap<String, String> map
                = new HashMap<String, String>();
        BufferedReader br = null;
        try {
            // create file object
            File file = new File(filename);
            // create BufferedReader object from the File
            br = new BufferedReader(new FileReader(file));
            String line = null;
            // read file line by line
            while ((line = br.readLine()) != null) {
                // split the line by :
                String[] parts = line.split(":");
                // first part is name, second is number
                String name = parts[0].trim();
                String number = parts[1].trim();
                // put name, number in HashMap if they are not empty
                if (!name.equals("") && !number.equals(""))
                    map.put(name, number);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Always close the BufferedReader
            if (br != null) {
                try {
                    br.close();
                }
                catch (Exception e) {
                };
            }
        }
        return map;
    }

    public static void main(String[] args) throws Exception {
        int port = 123;
        new Server(port);
    }


}
