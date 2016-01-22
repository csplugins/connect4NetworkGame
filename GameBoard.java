/**
   Java Networking Example Using Connect Four
   
   @author Cody Skala
   @version 1
*/
package connect4Network;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JComponent;

class GameBoard extends JComponent implements MouseListener, Runnable{
    private String ip = "localhost";
    private int port = 22222;
    private Thread thread;
    private Socket socket;
    private int errors = 0;
    private boolean isServer = true;
    private boolean unableToCommunicateWithOpponent = false;
    private DataOutputStream dos;
    private DataInputStream dis;
    private ServerSocket serverSocket;
    private boolean accepted = false;
    char gameOver = 'Q';
    char[][] board = new char[7][6];
    Graphics2D g2;
    boolean yourTurn = false;
    boolean once = true;
    int size;
    int wins = 0, loses = 0, ties = 0;
    int gameCount = 1;
    int gRow = 8, gColumn = 8;
    GameBoard(String ipAddress, int portNumber){
        ip = ipAddress;
        port = portNumber;
        if (!connect()) initializeServer();
        for(int i = 0; i < 7; i++)
            for(int j = 0; j < 6; j++){
                board[i][j] = 'Q';
            }
        addMouseListener(this);
        thread = new Thread(this, "Connect4");
        thread.start();
    }
    @Override
    public void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(238,238,238));
        Rectangle rect = new Rectangle(0, 0, getWidth(), getHeight()/7);
        g2.fill(rect);
        g2.draw(rect);
        rect = new Rectangle(0, getHeight()/7, getWidth(), getHeight()-getHeight()/7);
        g2.setColor(Color.yellow);
        g2.fill(rect);
        g2.draw(rect);
        if(rect.width < rect.height)
            size = rect.width/10;
        else size = rect.height/10;
        for(int i = 1; i < 8; i++)
            for(int j = 1; j < 7; j++){
                if(board[i-1][j-1] == 'x')
                    g2.setColor(Color.blue);
                else if(board[i-1][j-1] == 'o')
                    g2.setColor(Color.red);
                else g2.setColor(Color.gray);
                Ellipse2D ell = new Ellipse2D.Double((i*rect.width/8)-size/2, (j*rect.height/7)-size/2 + getHeight()/7, size, size);
                g2.fill(ell);
                g2.draw(ell);
            }
        checkForWin(g2);
        if(gameOver == 'x'){
            g2.setColor(Color.BLUE);
            g2.setFont(new Font("Verdana", Font.BOLD, size));
            int stringWidthT = g2.getFontMetrics().stringWidth("Winner");
            g2.drawString("Winner", getWidth() / 2 - stringWidthT / 2, getHeight()/7);
            if(once)
                wins++;
            once = false;
            playAgain(g2);
        }
        else if(gameOver == 'o'){
            g2.setColor(Color.RED);
            g2.setFont(new Font("Verdana", Font.BOLD, size));
            int stringWidthT = g2.getFontMetrics().stringWidth("Loser");
            g2.drawString("Loser", getWidth() / 2 - stringWidthT / 2, getHeight()/7);
            if(once)
                loses++;
            once = false;
            playAgain(g2);
        }
        else if(gameOver == 'T'){
            g2.setColor(new Color(255,0,255));
            g2.setFont(new Font("Verdana", Font.BOLD, size));
            int stringWidthT = g2.getFontMetrics().stringWidth("Tie");
            g2.drawString("Tie", getWidth() / 2 - stringWidthT / 2, getHeight()/7);
            if(once)
                ties++;
            once = false;
            playAgain(g2);
        }
        else if(!accepted){
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Verdana", Font.BOLD, size*4/5));
            int stringWidthT = g2.getFontMetrics().stringWidth("Waiting for opponent");
            g2.drawString("Waiting for opponent", getWidth() / 2 - stringWidthT / 2, getHeight()/7);
        }
        else if(yourTurn){
            g2.setColor(new Color(125,125,255));
            g2.setFont(new Font("Verdana", Font.BOLD, size));
            int stringWidthT = g2.getFontMetrics().stringWidth("Your turn");
            g2.drawString("Your turn", getWidth() / 2 - stringWidthT / 2, getHeight()/7);
        }
        else {
            g2.setColor(new Color(255,125,125));
            g2.setFont(new Font("Verdana", Font.BOLD, size));
            int stringWidthT = g2.getFontMetrics().stringWidth("Opponent's turn");
            g2.drawString("Opponent's turn", getWidth() / 2 - stringWidthT / 2, getHeight()/7);
        }
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Verdana", Font.BOLD, size/3));
        g2.drawString("Game " + gameCount, 0, size/3);
        g2.drawString("Record " + wins + "-" + loses + "-" + ties, 0, size*2/3);
    }
    
    public void playAgain(Graphics2D g2){
        Rectangle rect = new Rectangle(getWidth()/2 - size, 0, size*2, size*2/3);
        g2.setColor(Color.BLACK);
        g2.fill(rect);
        g2.draw(rect);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Verdana", Font.BOLD, size/3));
        int stringWidthT = g2.getFontMetrics().stringWidth("Play Again");
        g2.drawString("Play Again", getWidth() / 2 - stringWidthT / 2, size/2);
    }
    
    public void reset(){
        gameCount++;
        once = true;
        for(int i = 0; i < 7; i++)
            for(int j = 0; j < 6; j++)
                board[i][j] = 'Q';
        gameOver = 'Q';
        if(isServer && (gameCount % 2 == 1))
            yourTurn = true;
        else if(isServer && gameCount % 2 == 0)
            yourTurn = false;
        else if(!isServer && (gameCount % 2 == 1))
            yourTurn = false;
        else
            yourTurn = true;
        if(gRow < 8 && gColumn < 8){
            board[gRow][gColumn] = 'o';
            yourTurn = true;
        }
        gRow = 8;
        gColumn = 8;
        repaint();

    }
    
    public void checkForWin(Graphics2D g2){
        for(int i = 0; i < 7;i++){
            if(board[i][0] == 'Q')
                break;
            if(i == 6)
                gameOver = 'T';
        }
        
        for(int i = 0; i < 7; i++)
            for(int j = 0; j < 3; j++){
                if(board[i][j] == board[i][j+1] && board[i][j+1] == board[i][j+2] && board[i][j+2] == board[i][j+3] && ((board[i][j] == 'x') || (board[i][j] == 'o'))){
                    gameOver = board[i][j];
                    Point2D.Double point1 = new Point2D.Double(((i+1)*getWidth()/8), ((j+1)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Point2D.Double point2 = new Point2D.Double(((i+1)*getWidth()/8), ((j+4)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Line2D.Double win = new Line2D.Double(point1, point2);
                    g2.setColor(Color.white);
                    g2.fill(win);
                    g2.draw(win);
                }
            }
        
        for(int i = 0; i < 4; i++)
            for(int j = 0; j < 6; j++){
                if(board[i][j] == board[i+1][j] && board[i+1][j] == board[i+2][j] && board[i+2][j] == board[i+3][j] && ((board[i][j] == 'x') || (board[i][j] == 'o'))){
                    gameOver = board[i][j];
                    Point2D.Double point1 = new Point2D.Double(((i+1)*getWidth()/8), ((j+1)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Point2D.Double point2 = new Point2D.Double(((i+4)*getWidth()/8), ((j+1)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Line2D.Double win = new Line2D.Double(point1, point2);
                    g2.setColor(Color.white);
                    g2.fill(win);
                    g2.draw(win);
                }
            }
        
        for(int i = 0; i < 4; i++)
            for(int j = 0; j < 3; j++){
                if(board[i][j] == board[i+1][j+1] && board[i+1][j+1] == board[i+2][j+2] && board[i+2][j+2] == board[i+3][j+3] && ((board[i][j] == 'x') || (board[i][j] == 'o'))){
                    gameOver = board[i][j];
                    Point2D.Double point1 = new Point2D.Double(((i+1)*getWidth()/8), ((j+1)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Point2D.Double point2 = new Point2D.Double(((i+4)*getWidth()/8), ((j+4)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Line2D.Double win = new Line2D.Double(point1, point2);
                    g2.setColor(Color.white);
                    g2.fill(win);
                    g2.draw(win);
                }
            }
        for(int i = 0; i < 4; i++)
            for(int j = 3; j < 6; j++){
                if(board[i][j] == board[i+1][j-1] && board[i+1][j-1] == board[i+2][j-2] && board[i+2][j-2] == board[i+3][j-3] && ((board[i][j] == 'x') || (board[i][j] == 'o'))){
                    gameOver = board[i][j];
                    Point2D.Double point1 = new Point2D.Double(((i+1)*getWidth()/8), ((j+1)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Point2D.Double point2 = new Point2D.Double(((i+4)*getWidth()/8), ((j-2)*(getHeight()-getHeight()/7)/7) + getHeight()/7);
                    Line2D.Double win = new Line2D.Double(point1, point2);
                    g2.setColor(Color.white);
                    g2.fill(win);
                    g2.draw(win);
                }
            }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(gameOver == 'Q' && accepted && yourTurn){
            int column=-1;
            for(int i = 0; i < 7; i++){
                if(e.getX() > (i*2+1)*getWidth()/16 && e.getX() < (i*2+3)*getWidth()/16)
                    column = i;
            }
            if(column > -1){
                for(int i = 5; i > -1; i--){
                    if (board[column][i] == 'Q') {
                        board[column][i] = 'x';
                        try {
                            dos.writeInt(column);
                            dos.writeInt(i);
                            dos.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }
                        repaint();
                        yourTurn = false;
                        break;
                    }
                }
            }
        }else if(gameOver != 'Q'){
            if(e.getY() <= size*2/3 && e.getX() >= getWidth()/2 - size && e.getX() <= getWidth()/2 + size)
                reset();
        }
    }
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    
    private boolean connect() {
		try {
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
		} catch (IOException e) {
			System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
			return false;
		}
                isServer = false;
		System.out.println("Successfully connected to the server.");
		return true;
	}
    
    private void initializeServer() {
		try {
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		yourTurn = true;
                isServer = true;
	}
    private void listenForServerRequest() {
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
			System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    @Override
    public void run(){
        while (true) {
            tick();
            repaint();
            if (!accepted) {
                listenForServerRequest();
            }

        }
    }
    private void tick() {
		if (errors >= 10) unableToCommunicateWithOpponent = true;
		if (!yourTurn && !unableToCommunicateWithOpponent) {
                        try {
                            int row = dis.readInt();
                            int col = dis.readInt();
                            if(gameOver == 'Q'){
                                board[row][col] = 'o';
                                yourTurn = true;
                            }
                            else{
                                gRow = row;
                                gColumn = col;
                            }
			} catch (IOException e) {
                            e.printStackTrace();
                            errors++;
			}
		}
	}
}
