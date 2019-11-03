// test UDP receiver in place of a roboRIO
package app;

public class App {
    private static UdpReceive testUDPreceive;
    private static Thread UDPreceiveThread;

    public static void main(String[] args) throws Exception {
        testUDPreceive = new UdpReceive(5800);
        UDPreceiveThread = new Thread(testUDPreceive, "Test4237UDPreceive");
        UDPreceiveThread.start();
    }
}