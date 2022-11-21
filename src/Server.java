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
    private String receiverId;

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public Server(int port) throws IOException {
        createGroup("3:A,B,C");
        this.UserInfo = HashMapFromTextFile("UserInfo");
        this.groupList = HashMapFromTextFile("groupList");
        ServerSocket srvSocket = new ServerSocket(port);
        while (true) {
            print("Listening at port %d...\n", port);
            Socket clientSocket = srvSocket.accept();
            String userId = Login(clientSocket);

            synchronized (socketList) {
                socketList.put(userId, clientSocket);

            }

            Thread t = new Thread(() -> {
                try {
                    serve(userId, clientSocket);
                } catch (IOException ex) {
                    print("Connection drop!");
                }

                synchronized (socketList) {
                    socketList.remove(userId);
                }
            });
            t.start();
        }
    }

    private void serve(String userId, Socket clientSocket) throws IOException {
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());
        DataInputStream in;
        DataOutputStream out;
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());

        // send the number of user except himself
        out.writeInt((UserInfo.size() - 1));
        //================sending user list except the current user====================================
        UserInfo.forEach((key, value) -> {
            try {
                if (!key.equals(userId)) {
                    sendString(key, out);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //==================================================================================================

        while (true) {
            String command = receiveString(in);
            System.out.println(command);
//            String msg = receiveString(in);
//            System.out.println(msg);
            switch (command) {
                case "messageComing":
                    forwardMessage(userId, in, out);
                    break;
                case "file":
                    forwardFile(in);
                    break;
                case "createGroup":
                    createGroup(command);
                    break;
            }

        }

    }

    public void createGroup(String str) throws IOException {
        FileWriter fw = new FileWriter("groupList", true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.newLine();
        bw.write(str);
        bw.close();
    }

    private void forwardFile(DataInputStream in) throws IOException {
        File f = storeFile(in);
        System.out.println(f);
        if (socketList.containsKey(receiverId)) {
            DataOutputStream toReceiver = new DataOutputStream(socketList.get(receiverId).getOutputStream());
            sendString("file", toReceiver);
            sendFile(f.getAbsolutePath(), toReceiver);
            System.out.println(f.delete());
        } else {
//TODO-> when the user login again send the image immediately
        }


  /*      TODO ->
        If the receiver is offline, the files will be stored on the server side
        If the receiver is online, image files will be transferred immediately, and the
        delivered images will be displayed immediately
        */


    }

    private File storeFile(DataInputStream in) throws IOException {
        //receive name and length
        receiverId = receiveString(in);
        String fileName = receiveString(in);
        long fileLength = in.readLong();
        String currentPath = System.getProperty("user.dir") + "/src/File";
        File directory = new File(currentPath);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes = new byte[(int) fileLength];
        in.read(bytes, 0, bytes.length);
        fos.write(bytes, 0, (int) fileLength);
        fos.flush();
//        int length = 0;
//        while (
//TODO-> BUG-> client need to close the output stream ?? to finished
//                (length = in.read(bytes, 0, bytes.length)) != -1
//
//        ) {
//            System.out.println(length != -1);
//            fos.write(bytes, 0, length);
//            fos.flush();
//        }
        System.out.println("store completed");
        return file;
    }

    private void sendFile(String path, DataOutputStream out) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        File file = new File(path);
        int fileLength = (int) file.length();
        sendString(file.getName(), out);
        out.writeLong(file.length());
        byte[] bytes = new byte[fileLength];
        fis.read(bytes, 0, bytes.length);
        out.write(bytes, 0, fileLength);
        out.flush();
//            byte[] bytes = new byte[1024];
//            int length = 0;
//            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
//                out.write(bytes, 0, length);
//                out.flush();
//            }
    }

    private void forwardMessage(String userId, DataInputStream in, DataOutputStream out) throws IOException {
        String msg = receiveString(in);
        System.out.println(msg);
        // break the string into message and recipient part
        StringTokenizer str = new StringTokenizer(msg, ":");
        String receiverId = str.nextToken();
        String text = userId + ":" + str.nextToken();
//            String text =  str.nextToken();
        if (UserInfo.containsKey(receiverId)) {
            forwardSingle(receiverId, text);
        } else if (groupList.containsKey(receiverId)) {
//                forwardGroup(receiverId, text);
        }
    }

    public void forwardSingle(String receiverId, String msg) {
        //sending msg to a particular user
        synchronized (socketList) {
            try {
                //check if the person online
                if (socketList.containsKey(receiverId)) {
                    DataOutputStream toReceiver = new DataOutputStream(socketList.get(receiverId).getOutputStream());
                    sendString("messageComing", toReceiver);
                    sendString(msg, toReceiver);
                }
            } catch (IOException ex) {
                print("Unable to forward message to %s:%d\n",
                        socketList.get(receiverId).getInetAddress().getHostName(), socketList.get(receiverId).getPort());
            }
        }
    }

    public void forwardGroup(String group, String msg) throws IOException {
        //sending msg to a group
        //get the group members from the group list
        String members = groupList.get(group);
        String[] member = members.split(",");
        synchronized (socketList) {
            for (int i = 0; i < member.length; i++) {
                try {
                    //loop over every member to send msg one by one
                    //check if they are online
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
        String userId = "";
        String userPw = "";
        boolean valid = false;
        while (!valid) {
            try {
                userId = receiveString(in);
                userPw = receiveString(in);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //offline and
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

    public static HashMap<String, String> HashMapFromTextFile(String filename) {
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Always close the BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
                ;
            }
        }
        return map;
    }


    public static void main(String[] args) throws Exception {
        int port = 123;
        new Server(port);
    }


}