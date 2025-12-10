package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class TicTacToeClient extends Application implements TicTacToeConstants {

	private boolean myTurn = false;
	
	private char myToken = ' ';
	
	private char otherToken = ' ';
	
	private Cell[][] cell= new Cell[3][3];
	
	private Label lblTitle = new Label();
	
	private Label lblStatus = new Label();
	
	private int rowSelected;
	private int columnSelected;
	
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	
	private boolean continueToPlay = true;
	
	private boolean waiting = true;
	
	private String host = "localhost";
	
	@Override
	public void start(Stage primaryStage) {
		
		GridPane pane = new GridPane();
		for(int i  = 0; i < 3; i++)
			for(int j = 0; j < 3; j++)
				pane.add(cell[i][j] = new Cell(i, j), j, i);
		
		BorderPane borderPane = new BorderPane();
		borderPane.setTop(lblTitle);
		borderPane.setCenter(pane);
		borderPane.setBottom(lblStatus);
		
		Scene scene = new Scene(borderPane, 320, 350);
		primaryStage.setTitle("Tic Tac Toe Client");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		connectToServer();
	}
	
	private void connectToServer() {
		
		try {
			// Create a socket to connect to server
			Socket socket = new Socket(host, 8000);
			
			fromServer = new DataInputStream(socket.getInputStream());
			
			toServer = new DataOutputStream(socket.getOutputStream());
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		// Create a separate thread to control the game
		new Thread(() -> {
			try {
				
				int player = fromServer.readInt();
				
				if(player == PLAYER1) {
					myToken = 'X';
					otherToken = 'O';
					Platform.runLater( () -> {
						lblTitle.setText("Player 1 with token X");
						lblStatus.setText("Waiting for player 2 to join");
					});
					
					fromServer.readInt();
					
					Platform.runLater( () ->
						lblStatus.setText("Player 2 has joined. Player 1 starts"));
					
					myTurn = true;
				}
				else if( player == PLAYER2 ) {
					myToken = 'O';
					otherToken = 'X';
					Platform.runLater( () -> {
						lblTitle.setText("Player 2 with token O");
						lblStatus.setText("Waiting for player 1 to move");
					});
				}
				
				// Continue play
				while(continueToPlay) {
					if(player == PLAYER1) {
						waitForPlayerAction();
						sendMove();
						receiveInfoFromServer();
					}
					else if(player == PLAYER2) {
						receiveInfoFromServer();
						waitForPlayerAction();
						sendMove();
					}
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		});
	}
	
	// wait for player to mark a cell
	private void waitForPlayerAction() throws InterruptedException{
		while(waiting) {
			Thread.sleep(100);
		}
		waiting = true;
	}
	
	// send this players move to server
	private void sendMove() throws IOException{
		toServer.writeInt(rowSelected);
		toServer.writeInt(columnSelected);
	}
	
	// receive info from server
	private void receiveInfoFromServer() throws IOException{
		
		int status = fromServer.readInt();
		
		if(status == PLAYER1_WON) {
			
			continueToPlay = false;
			if(myToken == 'X') {
				Platform.runLater( () -> lblStatus.setText("I won! (X)"));
			}
			else if(myToken == 'O') {
				Platform.runLater( () -> lblStatus.setText("Player 1 has won! (X)"));
				receiveMove();
			}
		}
		else if(status == PLAYER2_WON) {
			continueToPlay = false;
			if(myToken == 'O') {
				Platform.runLater( () -> lblStatus.setText("I won! (O)"));
			}
			else if(myToken == 'X') {
				Platform.runLater( () -> lblStatus.setText("Player 2 has won! (O)"));
				receiveMove();
			}
		}
		else if(status == DRAW) {
			continueToPlay = false;
			Platform.runLater( () -> lblStatus.setText("Game is over, no winner"));
			
			if(myToken == 'O') {
				receiveMove();
			}
		}
		else {
			receiveMove();
			Platform.runLater( () -> lblStatus.setText("My Turn"));
			myTurn = true;
		}
	}
	
	private void receiveMove() throws IOException{
		
		int row = fromServer.readInt();
		int column = fromServer.readInt();
		Platform.runLater( () -> cell[row][column].setToken(otherToken));
	}
	
	public class Cell extends Pane{
		
		private int row;
		private int column;
		
		private char token = ' ';
		
		public Cell(int row, int column) {
			this.row = row;
			this.column = column;
			this.setPrefSize(2000, 2000);
			setStyle("-fx-border-color: black");
			this.setOnMouseClicked( e -> handleMouseClick());
		}
		
		public char getToken() {
			return token;
		}
		
		public void setToken(char c) {
			token = c;
			repaint();
		}
		
		
		protected void repaint() {
			
			int n = 10;
			
			if(token == 'X') {
				Line line1 = new Line(n, n, this.getWidth() - n, this.getHeight() - n);
				line1.endXProperty().bind(this.widthProperty().subtract(n));
				line1.endYProperty().bind(this.widthProperty().subtract(n));
				
				Line line2 = new Line(n, this.getHeight() - n, this.getWidth() - n, n);
				line2.endXProperty().bind(this.widthProperty().subtract(n));
				line2.endYProperty().bind(this.widthProperty().subtract(n));
				
				this.getChildren().addAll(line1, line2);
			}
			else if( token == 'O') {
				Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2, this.getWidth() /2 - n, this.getHeight() / 2 - n);
				ellipse.centerXProperty().bind(this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(this.widthProperty().divide(2));
				ellipse.radiusXProperty().bind(this.widthProperty().divide(2).subtract(n));
				ellipse.radiusYProperty().bind(this.heightProperty().divide(2).subtract(n));
				ellipse.setStroke(Color.BLACK);
				ellipse.setFill(Color.WHITE);
				getChildren().add(ellipse);
			}
		}
		
		private void handleMouseClick() {
			
			if(token == ' ' && myTurn) {
				setToken(myToken);
				myTurn = false;
				rowSelected = row;
				columnSelected = column;
				lblStatus.setText("Waiting for the other player to move");
				waiting = false;
			}
		}
	}
}
