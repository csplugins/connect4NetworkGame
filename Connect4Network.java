/**
   Java Networking Example Using Connect Four
   
   @author Cody Skala
   @version 1
*/
package connect4Network;

import java.util.Scanner;
import javax.swing.*;

public class Connect4Network {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(500, 500);
        frame.setTitle("Connect 4 - Network Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        String ip;
        int port;
        Scanner in = new Scanner(System.in);
        System.out.print("Enter the IP address: ");
        ip = in.next();
        System.out.print("Enter the port: ");
        port = in.nextInt();
        GameBoard GB = new GameBoard(ip, port);
        frame.add(GB);
        
        frame.setVisible(true);
    }
}