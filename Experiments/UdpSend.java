// UDP send program

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpSend
{
    private static final String pId = new String("[UdpSend]");

    InetAddress address;
    byte[] UDPbuffer = new byte[256];
    final int bufLength = UDPbuffer.length; // save original length because length property is changed with usage

    DatagramPacket packet;
    DatagramSocket datagramSocket;

    public UdpSend(int port, String URL)
    {
        try
        {
            //address = InetAddress.getByName("rkt-laptop.local");
            System.out.println("Sending UDP messages to >" + URL + "<");
            address = InetAddress.getByName(URL);
            //  public String UDPreceiver = "rkt-laptop.local"; // rkt laptop
            // 0.0.0.0 doesn't work for other computers - they don't see any packets
            // String UDPreceiver = "0.0.0.0"; // anywhere - wild card
            // address = InetAddress.getByName("127.0.0.1"); // here
            // address = InetAddress.getByName("roborio-4237-frc.local"); // there

        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        }

        try
        {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
            // Setting the port address to reuse can sometimes help and I can't think of how
            // it would hurt us if we only have one process running on a port.
            // Especially if the socket isn't closed, it can take some time to free the port
            // for reuse so if the program is restarted quickly and the port isn't noticed
            // to be free by the operating system,
            // there can be a socket exception if Reuse isn't set.
            // Example: socket = new DatagramSocket(port);
            // Example: socket.setReuseAddress(true);

            // datagramSocket.setSoTimeout(2000); // robot response receive timeout in
            // milliseconds check in case robot
            // isn't responding. Not used if no attempt to receive a response
        } catch (SocketException e)
        {
            e.printStackTrace();
        }

        packet = new DatagramPacket(UDPbuffer, bufLength, address, port);
    }

    public synchronized void Communicate(String message)
    {
        UDPbuffer = message.getBytes();

        packet.setData(UDPbuffer, 0, UDPbuffer.length);

        try
        {
            datagramSocket.send(packet); // send target information to robot
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}