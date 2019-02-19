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

import com.google.gson.Gson;

public class UdpReceive extends Thread
{

    class TargetData
    {
        int cogX; // Horizontal (X) center of gravity of the rectangle
        int cogY; // Vertical (Y) center of gravity of the rectangle
        int width; // Width of the rectangle
        int area; // Area of the rectangle
        boolean isTargetFound; // Was a target found?
        // --------------------------------------------------------------------------
    
        // These fields are used to track the validity of the data.
        int frameNumber; // Number of the camera frame
        boolean isFreshData; // Is the data fresh?

        public synchronized void fromJson(String message)
        {
            TargetData temp = new Gson().fromJson(message, TargetData.class);
            System.out.println(temp);
        }

        public String toString()
    {
        return String.format("[Received TargetData] Frame = %d, cogX = %d, cogY = %d, width = %d, area = %d  %s", frameNumber,
                cogX, cogY, width, area, isFreshData ? "FRESH" : "stale");
    }
    
    }

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
        System.out.println("[UDPReceive] packet listener thread started");
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
                //System.out.println("UDP received >" + lastDataReceived + "<");

                if (lastDataReceived.startsWith("Bumper "))
                {
                    String message = new String(lastDataReceived.substring("Bumper ".length()));
                    TargetData receivedTarget = new TargetData();
                    System.out.print("[UDPReceive] Bumper ");
                    receivedTarget.fromJson(message);
               }

                else if (lastDataReceived.startsWith("Elevator "))
                {
                    String message = new String(lastDataReceived.substring("Elevator ".length()));
                    TargetData receivedTargetB = new TargetData();
                    System.out.print("[UDPReceive] Elevator ");
                    receivedTargetB.fromJson(message);
                }

                else
                {
                    System.out.println("[UDPReceive] Unknown class received UDP " + lastDataReceived);
                }
            } catch (SocketTimeoutException e)
            {
                // do something when no messages for awhile
                System.out.println("[UDPReceive] hasn't heard from the vision pipeline for awhile");
            } catch (IOException e)
            {
                e.printStackTrace();
                // could terminate loop but there is no easy restarting
            }
        }
        // socket.close();
    }
}