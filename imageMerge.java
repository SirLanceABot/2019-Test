import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.wpilibj.Timer;

class imageMerge implements Runnable {
   public static images currentFrame = new images();

   @Override
   public void run() {
      Mat Image = new Mat(); // main driver image from elevator
      Mat ImageOverlay = new Mat(); // main driver image from elevator
      Mat ImageOutput = new Mat(); // main driver image from elevator
      Mat insert = new Mat(); // bumper image
      Mat insertSmall = new Mat(); // bumper image shrunk
      Mat subMat = new Mat(); // place for shrunken bumper image inserted into main driver image

      CvSource outputStream = new CvSource("DriverView", VideoMode.PixelFormat.kMJPEG, 160, 120, 30);
      // CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG,
      // width, height, frames_per_sec);

      MjpegServer mjpegServer = new MjpegServer("serve_DriverView", 1185);

      mjpegServer.setSource(outputStream);

      while (true) {
         try {
            Main.elevatorPipeline.getImage().copyTo(Image);
            Image.copyTo(ImageOverlay);
            Image.copyTo(ImageOutput);

            Main.bumperPipeline.getImage().copyTo(insert);

            // Scaling the insert smaller
            // Imgproc.resize(insert, insertSmall, new Size(insert.rows() / 3, insert.rows()
            // / 3), 0, 0, Imgproc.INTER_AREA);
            Imgproc.resize(insert, insertSmall, new Size(), 0.333, 0.333, Imgproc.INTER_AREA);

            subMat = ImageOverlay.submat(80, 80 + subMat.rows(), 56, 56 + subMat.cols()); // rowStart, rowEnd, colStart,
                                                                                          // colEnd to place the insert
                                                                                          // on the overlay

            insertSmall.copyTo(subMat); // copy the insert to the overlay
            double alpha = 0.9f;

            // alpha+beta = 1 usually; gamma is added to image; = 0 for no gamma adjustment
            Core.addWeighted(ImageOverlay, alpha, ImageOutput, alpha, 0, ImageOutput);

            outputStream.putFrame(ImageOutput);

         } catch (Exception e) {
            System.out.println("ImageMerge error " + e);
         }

      }
   }
}
// Mat zeros = Mat.zeros(60, 160, CvType.CV_8UC3);

// public void showResult(Mat img) {
// Imgproc.resize(img, img, new Size(640, 480));
// MatOfByte matOfByte = new MatOfByte();
// Imgcodecs.imencode(".jpg", img, matOfByte);
// byte[] byteArray = matOfByte.toArray();
// BufferedImage bufImage = null;
// try {
// InputStream in = new ByteArrayInputStream(byteArray);
// bufImage = ImageIO.read(in);
// JFrame frame = new JFrame();
// frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
// frame.pack();
// frame.setVisible(true);
// } catch (IOException | HeadlessException e) {
// e.printStackTrace();
// }
// }
// https://www.programcreek.com/java-api-examples/?class=org.opencv.imgproc.Imgproc&method=resize