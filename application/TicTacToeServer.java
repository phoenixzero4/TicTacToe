package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class TicTacToeServer extends Application implements TicTacToeConstants{
	private int sessionNo = 1;
	
	@Override
	public void start(Stage primaryStage) {
		
		
		TextArea taLog = new TextArea();
		
		Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
		primaryStage.setTitle("TicTacToeServer");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		new Thread( () -> {
			try {
				ServerSocket serverSocket = new ServerSocket(8000);
				Platform.runLater( () -> taLog.appendText("Server started at " + new Date() + "\n"));
				
				while(true) {
					
				Platform.runLater( () -> taLog.appendText(new Date() + ": wait for players to join session " + sessionNo + "\n"));
				
				// Connect player 1
				Socket player1 = serverSocket.accept();
				
				Platform.runLater( () -> {
					taLog.appendText(new Date() + " Player 1 joined session " + sessionNo + "\n");
					taLog.appendText("Player 1's IP Address " + player1.getInetAddress().getHostAddress() + " \n");
				});
				
				new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);
				
				// Connect player 2
				Socket player2 = serverSocket.accept();
				
				Platform.runLater( () -> {
					taLog.appendText(new Date() + " Player 2 joined session " + sessionNo + "\n");
					taLog.appendText("Player 2's IP Address " + player2.getInetAddress().getHostAddress() + " \n");
				});
				
				new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);
				
				
				Platform.runLater( () -> taLog.appendText(new Date() + " : Starting thread for session "  + sessionNo + " \n"));
				
				// Start thread for this session
				new Thread(new HandleSession(player1, player2)).start();
				}
			}catch(IOException ex) {
				ex.printStackTrace();
			}
		}).start();
	}
	
	class HandleSession implements Runnable, TicTacToeConstants{
		private Socket player1;
		private Socket player2;
		
		private char[][] cell = new char[3][3];
		
		private DataInputStream fromPlayer1;
		private DataOutputStream toPlayer1;
		private DataInputStream fromPlayer2;
		private DataOutputStream toPlayer2;
		
		private boolean continueToPlay = true;
		
		public HandleSession(Socket player1, Socket player2) {
			this.player1 = player1;
			this.player2 = player2;
			
			for(int i = 0; i < 3; i++) {
				for(int j = 0; j < 3; j++) {
					cell[i][j] = ' ';
				}
			}
		}
		
		public void run() {
			try {
				
				DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
				DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
				DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
				DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());
				
				toPlayer1.writeInt(1);
				
				// run the game
				while(true) {
					
					// player 1 moves
					int row = fromPlayer1.readInt();
					int column = fromPlayer1.readInt();
					cell[row][column] = 'X';
					
					// check if player1 has won
					if(isWon('X')) {
						toPlayer1.writeInt(PLAYER1_WON);
						toPlayer2.writeInt(PLAYER1_WON);
						sendMove(toPlayer2, row, column);
						break;
					}
					else if(isFull()) {
						toPlayer1.writeInt(DRAW);
						toPlayer2.writeInt(DRAW);
						sendMove(toPlayer2, row, column);
						break;
					}else {
						// player 2's turn
						toPlayer2.writeInt(CONTINUE);
						sendMove(toPlayer2, row, column);
					}
					
					// player 2 moves
					row = fromPlayer2.readInt();
					column = fromPlayer2.readInt();
					cell[row][column] = 'O';
					
					// check if player1 has won
					if(isWon('O')) {
						toPlayer1.writeInt(PLAYER2_WON);
						toPlayer2.writeInt(PLAYER2_WON);
						sendMove(toPlayer1, row, column);
						break;
					}
					else if(isFull()) {
						toPlayer1.writeInt(DRAW);
						toPlayer2.writeInt(DRAW);
						sendMove(toPlayer1, row, column);
						break;
					}else {
						// player 2's turn
						toPlayer1.writeInt(CONTINUE);
						sendMove(toPlayer1, row, column);
					}
				}
			}
			catch(IOException ex) {
				ex.printStackTrace();
			}
		}
		
		// send the move to the other player
		private void sendMove(DataOutputStream out, int row, int column) throws IOException{
			out.writeInt(row);
			out.writeInt(column);
		}
		
		// check if all cells are occupied
		private boolean isFull() {
			for(int i = 0; i < 3; i++) {
				for(int j = 0; j < 3; j++) {
					if(cell[i][j] == ' ') {
						return false;
					}
				}
			}
			return true;
		}
		
		// check if player with specified token has won
		private boolean isWon(char token) {
			
			// check all rows
			for(int i = 0; i < 3; i++) {
				if(( cell[i][0] == token) && (cell[i][1] == token) && (cell[i][2] == token)) {
					return true;
				}
			}
			
			// check all columns
			for(int j = 0; j < 3; j++) {
				if((cell[0][j] == token) && (cell[1][j] == token) && cell[2][j] == token) {
					return true;
				}
			}
			
			// check major diagonal
			if( (cell[0][0] == token) && (cell[1][1] == token) && (cell[2][2] == token)){
				return true;
			}
			
			// check subdiagonal
			if( (cell[0][2] == token) && (cell[1][1] == token) && (cell[2][0] == token)){
				return true;
			}
			
			// everything checked, no winner
			return false;
		}
	}
}
