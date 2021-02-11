package com;
// Set up a client that will receive a connection from a server, send
// a string to the server, and close the connection.

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Client extends Application {
    // Enable option Allow run in parallel in the Run configuartion to allow running multiple Client instances
    private TextField txtInputLink; // inputs message from user
    private TextArea txaChatText;// display information to user
    private ObjectOutputStream output; // output stream to server
    private ObjectInputStream input; // input stream from server
    private String chatServer; // host server for this application
    private Socket client; // socket to communicate with server
    private String message = ""; // message from server
    private Button Extract;
    private Button Register;
    private TextField userName;
    private PasswordField password;
    private Button submit;
    // set up GUI

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(14));
        root.setSpacing(8);
        txaChatText = new TextArea();
        txaChatText.setMaxSize(1.7976931348623157E308, 1.7976931348623157E308);

        userName = new TextField();
        userName.setPromptText("Enter username");
        password = new PasswordField();
        password.setPromptText("Enter password");
        submit = new Button("Submit");
        submit.setOnAction(event -> {
            Pattern pattern = Pattern.compile("^[^\s]+$");
            Matcher userNameMatcher = pattern.matcher(userName.getText());
            Matcher passwordMatcher = pattern.matcher(password.getText());
            boolean matchFound = userNameMatcher.matches()&&passwordMatcher.matches();
            if(!matchFound){
                displayMessage("\nInvalid user name or password");
            }
            else {
                sendData("1 " + userName.getText() + " " + password.getText());
            }});
        root.getChildren().addAll(userName,password, txaChatText,submit);
        Scene scene = new Scene(root, 350, 250, Color.web("#666970"));
        // shutdown thread gracefully
        primaryStage.setOnCloseRequest(evt -> stop());
        primaryStage.setTitle("Client");
        primaryStage.setScene(scene);
        primaryStage.show();
        Thread thread = new Thread(() -> runClient());
        thread.start();
    }

    // connect to server and process messages from server
    public void runClient() {
        try // connect to server, get streams, process connection
        {
            connectToServer(); // create a Socket to make connection
            getStreams(); // get the input and output streams
            processConnection(); // process connection
        } // end try
        catch (EOFException eofException) {
            displayMessage("\nClient terminated connection");
        } // end catch
        catch (IOException ioException) {
            System.out.println("Client IOexception    " + ioException.getMessage());
        } // end catch
        finally {
            closeConnection(); // close connection
        } // end finally
    } // end method runClient

    // connect to server
    private void connectToServer() throws IOException {
        displayMessage("Attempting connection\n");

        // create Socket to make connection to server
        client = new Socket(InetAddress.getByName(chatServer), 12345);
        if (client == null) {
              Platform.exit();
              System.exit(0);
            stop();
        }
        displayMessage("Connected to: "
                + client.getInetAddress().getHostName());
    }

    // get streams to send and receive data
    private void getStreams() throws IOException {
        // set up output stream for objects
        output = new ObjectOutputStream(client.getOutputStream());
        output.flush(); // flush output buffer to send header information

        // set up input stream for objects
        input = new ObjectInputStream(client.getInputStream());

        displayMessage("\nGot I/O streams\n");
    } // end method getStreams

    // process connection with client
    private void processConnection() throws IOException {

        sendData("Success."); // send connection successful message

        // enable enterField so server user can send messages
        do // process messages sent from client
        {
            try // read message and display it
            {
                message = (String) input.readObject(); // read new message
                displayMessage("\n" + message); // display message
                if(message.equals("valid user")){
                    Platform.runLater(
                            () -> {
                                Stage stage = new Stage();
                                VBox root = new VBox();
                                root.setAlignment(Pos.TOP_CENTER);
                                root.setPadding(new Insets(14));
                                root.setSpacing(8);
                                txtInputLink = new TextField();
                                txtInputLink.setPromptText("Type text");
                                txaChatText = new TextArea();
                                txaChatText.setMaxSize(1.7976931348623157E308, 1.7976931348623157E308);
                                txtInputLink.setOnAction(event -> {

                                            sendData(txtInputLink.getText());
                                            txtInputLink.setText("");
                                        }
                                );
                                Extract = new Button("Extract");
                                Extract.setOnAction(event -> {
                                    Pattern pattern = Pattern.compile("^[0-9]{16}$");
                                    Matcher matcher = pattern.matcher(txtInputLink.getText());
                                    boolean matchFound = matcher.matches();
                                    if(!matchFound){
                                        displayMessage("\nInvalid Token");
                                    }
                                    else{
                                    sendData("2 "+txtInputLink.getText());
                                    }
                                });
                                Register = new Button("Register");
                                Register.setOnAction(event -> {
                                    Pattern pattern = Pattern.compile("^[0-9]{16}$");
                                    Matcher matcher = pattern.matcher(txtInputLink.getText());
                                    boolean matchFound = matcher.matches();
                                    if(!matchFound){
                                        displayMessage("\nInvalid Card number");
                                    }
                                    else{
                                    sendData("3 "+txtInputLink.getText());
                                    }
                                });
                                root.getChildren().addAll(txtInputLink,txaChatText,Extract,Register);
                                Scene scene = new Scene(root, 350, 250, Color.web("#666970"));
                                stage.setScene(scene);
                                stage.setTitle("Client");
                                stage.show();
                            }
                    );

                }
            } // end try
            catch (ClassNotFoundException classNotFoundException) {
                displayMessage("\nUnknown object type received");
            } // end catch
            catch (SocketException s) {
                break;
            }
        } while (!message.toUpperCase().equals("CLIENT>>> TERMINATE"));
    } // end method processConnection

    // close streams and socket
    private void closeConnection() {
        displayMessage("\nTerminating connection\n");
        try {
            if (output != null) {
                output.close(); // close output stream
            }
            if (input != null) {
                input.close(); // close input stream
            }
            if (client != null) {
                client.close(); // close socket
            }
        } // end try
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
    } // end method closeConnection

    // send message to client
    private void sendData(String message) {
        try // send object to client
        {
            output.writeObject(message);
            output.flush(); // flush output to client
            displayMessage("\n"+message);
        } // end try
        catch (IOException ioException) {
            txaChatText.appendText("\nError writing object");
        } // end catch
    } // end method sendData

    // manipulates displayArea in the event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        Platform.runLater(()
                        ->
                {
                    txaChatText.appendText(messageToDisplay);
                }

        );
    }

    private void setTextFieldEditable(final boolean editable) {
        Platform.runLater(()
                        -> // sets enterField's editability
                {
                    txtInputLink.setEditable(editable);
                } // end method run

        );
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}// end class Server
