package com;

import com.thoughtworks.xstream.XStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.scene.control.Button;

public class Server extends Application {

    private class User{
        private String userName;
        private String password;
        private boolean canRegister;
        private boolean canExtract;
        public User(){
            this("","",false,false);
        }
        public User(String userName, String password, boolean canRegister, boolean canExtract) {
            setUserName(userName);
            setPassword(password);
            setCanRegister(canRegister);
            setCanExtract(canExtract);
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isCanRegister() {
            return canRegister;
        }

        public void setCanRegister(boolean canRegister) {
            this.canRegister = canRegister;
        }

        public boolean isCanExtract() {
            return canExtract;
        }

        public void setCanExtract(boolean canExtract) {
            this.canExtract = canExtract;
        }
    }
    private class CardTokenPair{
        private String card;
        private String token;

        public CardTokenPair(String card, String token) {
            this.card = card;
            this.token = token;
        }

        public String getCard() {
            return card;
        }

        public void setCard(String card) {
            if(validCard(card)) {
                this.card = card;
            }
            else{
                System.out.println("Error:Invalid Card!");
            }
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            if(validToken(token,card)) {
                this.token = token;
            }
            else{
                System.out.println("Error:InvalidToken");
            }
        }

        @Override
        public String toString() {
            return card + '\'' + token;
        }
    }
    private boolean validCard(String card){
        Pattern pattern = Pattern.compile("^[3-6][0-9]{15}$");
        Matcher matcher = pattern.matcher(card);
        boolean matchFound = matcher.matches();
        if(!matchFound){
            return false;
        }
        int nDigits = 16;

        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--)
        {

            int d = card.charAt(i) - '0';

            if (isSecond)
                d = d * 2;

            // We add two digits to handle
            // cases that make two digits
            // after doubling
            nSum += d / 10;
            nSum += d % 10;

            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }
    private boolean validToken(String token,String card){
        Pattern pattern = Pattern.compile("^[^3-6][0-9]{15}$");
        Matcher matcher = pattern.matcher(token);
        boolean matchFound = matcher.matches();
        if(!matchFound){
            return false;
        }
        int nDigits = 16;

        int nSum = 0;
        for (int i = nDigits - 1; i >= 0; i--)
        {

            int d = token.charAt(i) - '0';
            if(i<=nDigits-1 && i>=nDigits-4){
                if(token.charAt(i)!=card.charAt(i)){
                    return false;
                }
            }
            else{
                if(token.charAt(i)==card.charAt(i)){
                    return false;
                }
            }
            nSum+=d;
        }
        return (nSum % 10 != 0);
    }
    private String generateToken(String card){
        char[] tokenArray = new char[16];
        int indexSum=0;
        for (int i = 12; i <=15 ; i++) {
            tokenArray[i] = card.charAt(i);
            indexSum+=tokenArray[i]-'0';
        }
        int newIndexSum=0;
        Random random = new Random();
        boolean unique=true;
        String token;
        do{
            int temp=0;
            newIndexSum=indexSum;
            do{
                temp=random.nextInt(10);
            }while (temp == 3 || temp == 4 || temp ==5 || temp == 6);

            tokenArray[0]= (char) (temp+'0');
            newIndexSum+=temp;

            for (int i = 1; i < 12; i++) {
                    do{
                        temp=random.nextInt(10);
                    }while (temp!=card.charAt(i)-'0');
                    tokenArray[i]= (char) (temp+'0');
                    newIndexSum+=temp;
            }
            token = new String(tokenArray);
            for(CardTokenPair pair: pairs){
                if (token.equals(pair.getToken())) {
                    unique = false;
                    break;
                }
            }
        }
        while(newIndexSum%10==0 || !unique);
        return token;
    }
    private ArrayList<User> users;
    private ArrayList<CardTokenPair> pairs;
    private XStream xStream;
    private ArrayList<String> usersXML;
    private ArrayList<String> pairsXML;
    private TextArea logText;
    private Button sortByToken;
    private Button sortByCard;
    private ExecutorService threadExecutor;
    private ServerSocket server;
    private int counter = 1;
    private RunClient client;
    private Hashtable<Thread, ObjectOutputStream> writers;
    @Override
    public void start(Stage primaryStage) throws Exception {
            xStream = new XStream();
            writers = new Hashtable<>();
            threadExecutor = Executors.newCachedThreadPool();
            VBox root = new VBox();
            root.setAlignment(Pos.TOP_CENTER);
            pairs = new ArrayList<>();
            users = new ArrayList<>();
            users.add(new User("George_Smith","12345",false,true));
            usersXML.add(xStream.toXML(new User("George_Smith","12345",false,true)));
            users.add(new User("Ivan_Ivanov","ilovedogs",false,true));
            usersXML.add(xStream.toXML(new User("Ivan_Ivanov","ilovedogs",false,true)));
            users.add(new User("Dan_Brown","password123",true,true));
            usersXML.add(xStream.toXML(new User("Dan_Brown","password123",true,true)));
            users.add(new User("Catherine_Jones","dreams68",true,false));
            usersXML.add(xStream.toXML(new User("Catherine_Jones","dreams68",true,false)));

            root.setPadding(new Insets(14));
            root.setSpacing(8);
            logText = new TextArea();
            logText.setMaxSize(1.7976931348623157E308, 1.7976931348623157E308);
            sortByCard = new Button("Sort by Card Number");
            sortByCard.setOnAction(event -> {
                    Path path = Paths.get("./pairs_Sorted_By_CardNo.txt");
                    if(Files.exists(path)){
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                try {
                    Files.createFile(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<CardTokenPair> List = pairs.stream().sorted(((o1, o2) -> o1.card.compareTo(o2.card))).collect(Collectors.toList());
                StringBuilder stringBuilder = new StringBuilder();
                for (CardTokenPair pair : List){
                    stringBuilder.append(pair.card).append("\t").append(pair.token).append("\n");
                }
                try {
                    Files.writeString(path,"CardNo\t\t\tToken\n"+stringBuilder.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            sortByToken = new Button("Sort by Token Number");
            sortByToken.setOnAction(event -> {
                Path path = Paths.get("./pairs_Sorted_By_Token.txt");
                if(Files.exists(path)){
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Files.createFile(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<CardTokenPair> List = pairs.stream().sorted(((o1, o2) -> o1.token.compareTo(o2.token))).collect(Collectors.toList());
                StringBuilder stringBuilder = new StringBuilder();
                for (CardTokenPair pair : List){
                    stringBuilder.append(pair.token).append("\t").append(pair.card).append("\n");
                }
                try {
                    Files.writeString(path,"Token\t\t\tCardNo\n"+stringBuilder.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }



            });
            root.getChildren().addAll(logText,sortByCard,sortByToken);
            Scene scene = new Scene(root, 350, 250, Color.web("#666970"));
            primaryStage.setOnCloseRequest(evt -> {
                try {
                    stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            primaryStage.setTitle("Server");
            primaryStage.setScene(scene);
            primaryStage.show();
            // start the server in a separate Thread
            new Thread(() -> runServer()).start();
    }



    public void runServer() {

        try // set up server to receive connections; process connections
        {
            server = new ServerSocket(12345, 100); // create ServerSocket
            displayMessage("Waiting for connection\n");
            while (true) {

                waitForConnection(); // wait for a connection

            } // end while
        } // end try
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
    }
    public class RunClient implements Runnable {

        private ObjectOutputStream output; // output stream to client
        private ObjectInputStream input; // input stream from client
        private final Socket connection; // connection to client
        private final int counter;
        private boolean canRegister=false,
                canExtract=false;
        public RunClient(Socket connection, int counter) {

            this.connection = connection;
            this.counter = counter;

        }

        private void getStreams() throws IOException {
            output = new ObjectOutputStream(connection.getOutputStream());
            output.flush();

            input = new ObjectInputStream(connection.getInputStream());
            writers.put(Thread.currentThread(), output);
            displayMessage("\nGot I/O streams\n");

        }

        @Override
        public void run() {

            displayMessage("Connection " + counter + " received from: "
                    + connection.getInetAddress().getHostName());

            try {
                getStreams();
                processConnection();
            } catch (EOFException eofException) {
                displayMessage("\nServer terminated connection");
            }
            catch (IOException ex) {
                displayMessage("\nClient terminated connection");
            } finally {
                closeConnection();

            }

        }

        private void processConnection() throws IOException {
            String message = "\nSERVER>>> Connection successful";
            output.writeObject(message); // send connection successful message
            displayMessage(message);
            displayMessage("\nSERVER>>> Waiting for new connection\n");
            do
            {
                try
                {
                    message = (String) input.readObject();
                    displayMessage("\n" + message); // display message
                    String[] strings = message.split(" ");
                    if(strings[0].equals("1")){
                        boolean isValid=false;
                        for(User user:users){
                            if(user.getUserName().equals(strings[1]) && user.getPassword().equals(strings[2])){
                                sendMessage("valid user");
                                canExtract = user.canExtract;
                                canRegister = user.canRegister;
                                displayMessage("\n valid user and password");
                                isValid=true;
                            }
                        }
                        if(!isValid){
                            sendMessage("\ninvalid user or password");
                        }

                    }
                    else if(strings[0].equals("2")){
                        displayMessage("\nExtract "+strings[1]);
                        if(canExtract){
                            boolean registered=false;
                            for(CardTokenPair pair: pairs){
                                if(strings[1].equals(pair.token)){
                                    sendMessage(pair.card);
                                    registered=true;
                                }
                            }
                            if(!registered){
                                sendMessage("Token isn't registered");
                            }
                        }
                        else {
                            sendMessage("Error: you do not have the rights to extract a card number!");
                        }

                    }
                    else if(strings[0].equals("3")){
                        displayMessage("\n Register "+strings[1]);
                        if(canRegister) {
                            if (validCard(strings[1])) {
                                String token = generateToken(strings[1]);
                                sendMessage(token);
                                pairs.add(new CardTokenPair(strings[1], token));
                                pairsXML.add(xStream.toXML(new CardTokenPair(strings[1], token)));
                            } else {
                                sendMessage("Invalid card number");
                            }
                        }
                        else{
                            sendMessage("Error: you do not have the right to register a token!");
                        }
                    }
                }
                catch (ClassNotFoundException classNotFoundException) {
                    displayMessage("\nUnknown object type received");
                }

            } while (!message.equals("CLIENT>>> TERMINATE"));
        }

        // close streams and socket
        private void closeConnection() {
            displayMessage("\nTerminating connection No." + this.counter + "\n");
            try {
                if (output != null) {
                    output.close(); // close output stream
                }
                if (output != null) {
                    writers.remove(Thread.currentThread());
                }
                if (input != null) {
                    input.close(); // close input stream
                }
                if (connection != null) {
                    connection.close(); // close socket
                }
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end method closeConnection
        private void sendMessage(String message){
            try // broadcast message to clients
            {
                    writers.get(Thread.currentThread()).writeObject(message);
                    writers.get(Thread.currentThread()).flush(); // flush output to client

            } // end try
            catch (IOException ioException) {
                logText.appendText("\nError writing object");
            } // end catch
        }
    }
    private void displayMessage( String messageToDisplay) {
        Platform.runLater(()
                        -> // updates displayArea
                {
                    logText.appendText(messageToDisplay); // append message
                } // end method run

        ); // end call to Platfrom.runLater
    } // end method displayMessage
    private void waitForConnection() throws IOException {

        Socket s = server.accept();

        client = new RunClient(s, counter);
        threadExecutor.execute(client);

        counter++;
    } // end method waitForConnection
    private void sendData(String message) {
        try // broadcast message to clients
        {
            displayMessage("\nSERVER>>> " + message);
            Set<Thread> threads = writers.keySet();
            for (Thread thread : threads) {
                writers.get(thread).writeObject("SERVER>>> " + message);
                writers.get(thread).flush(); // flush output to client

            }
        } // end try
        catch (IOException ioException) {
            logText.appendText("\nError writing object");
        } // end catch
    } // end method
    @Override
    public void stop() {
        Platform.exit();
        threadExecutor.shutdown();
        System.exit(0);
    }
    public static void main(String[] args) { launch(args); }
}
