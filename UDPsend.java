
// UDP send program

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UDPsend {
    InetAddress address;
    byte[] UDPbuffer = new byte[256];
    final int bufLength = UDPbuffer.length; // save original length because length property is changed with usage

    DatagramPacket packet;
    DatagramPacket packetReceive;
    DatagramSocket datagramSocket;

    public UDPsend(int port) {

        try {
            // TODO: add code to variably specify where to send messages
            // InetAddress address = InetAddress.getByAddress(IP);
            address = InetAddress.getByName("127.0.0.1"); // http://roborio-4237-frc.local/#Home
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            datagramSocket = new DatagramSocket();

            // Setting the port address to reuse can sometimes help and I can't think of how
            // it would hurt us if we only have one process running on a port.
            // Especially if the socket isn't closed, it can take some time to free the port
            // for reuse so if the program is restarted quickly and the port isn't noticed
            // to be free by the operating system,
            // there can be a socket exception if Reuse isn't set.
            // Example: socket = new DatagramSocket(port);
            // Example: socket.setReuseAddress(true);

            datagramSocket.setSoTimeout(2000); // robot response receive timeout in milliseconds check in case robot
                                               // isn't responding
        } catch (SocketException e) {
            e.printStackTrace();
        }

        packet = new DatagramPacket(UDPbuffer, bufLength, address, port);

        packetReceive = new DatagramPacket(UDPbuffer, bufLength, address, port);
    }

    public synchronized void Communicate(String message) {
 
        UDPbuffer = message.getBytes();

        packet.setData(UDPbuffer, 0, UDPbuffer.length);

        try {
            datagramSocket.send(packet); // send requested speed to robot
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
