/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

// UDP receive program - test receiver instead of using the roboRIO

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class UdpReceive implements Runnable
{
    private static final String pId = new String("[UdpReceive]");

    public static String lastDataReceived = "";
    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected boolean moreQuotes = true;

    public UdpReceive(int port)
    {
        // super(name);
        try
        {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(500); // set receive timeout in milliseconds in case RPi is dead
        } catch (SocketException e)
        {
            // do something when something bad happens
            e.printStackTrace();
        }

        // Setting the port address to reuse can sometimes help and I can't think of how
        // it would hurt us if we only have one process running on a port.
        // Especially if the socket isn't closed, it can take some time to free the port
        // for reuse so if the program is restarted quickly and the port isn't noticed
        // to be free by the operating system,
        // there can be a socket exception if Reuse isn't set.
        // Example: socket = new DatagramSocket(port);
        // Example: socket.setReuseAddress(true);
    }

    public void run()
    {
        System.out.println(pId + " packet listener thread started");
        byte[] buf = new byte[256];
        final int bufLength = buf.length; // save original length because length property is changed with usage
        DatagramPacket packet = new DatagramPacket(buf, bufLength);

        while (true)
        {
            try
            {
                // receive request
                packet.setLength(bufLength);
                socket.receive(packet); // always receive the packets
                byte[] data = packet.getData();
                lastDataReceived = new String(data, 0, packet.getLength());
                // System.out.println(pId + " >" + lastDataReceived + "<");

                if (lastDataReceived.startsWith("Bumper "))
                {
                    String message = new String(lastDataReceived.substring("Bumper ".length()));
                    TargetDataReceive receivedTargetB = new TargetDataReceive();
                    receivedTargetB.fromJson(message);
                    System.out.println(pId + " Bumper " + receivedTargetB);   
                }

                else if (lastDataReceived.startsWith("Elevator "))
                {
                    String message = new String(lastDataReceived.substring("Elevator ".length()));
                    TargetDataReceive receivedTargetE = new TargetDataReceive();
                    receivedTargetE.fromJson(message);
                    System.out.println(pId + " Elevator " + receivedTargetE);   
                }

                else
                {
                    System.out.println(pId + " Unknown class received UDP " + lastDataReceived);
                }
            } catch (SocketTimeoutException e)
            {
                // do something when no messages for awhile
                System.out.println(pId + " hasn't heard from any vision pipeline for awhile");
            } catch (IOException e)
            {
                e.printStackTrace();
                // could terminate loop but there is no easy restarting
            }
        }
        // socket.close();
    }
}