import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;

class imageMerge implements Runnable {
 
   @Override
   public void run() {
      Mat ImageOverlay = new Mat(); // main image from elevator
      Mat ImageOutput = new Mat(); // main image from elevator + small bumper image inserted then weighted merge and saved
      Mat subMat = new Mat(); // place for small bumper image inserted into main elevator image
      Mat insert = new Mat(); // bumper image
      Mat insertSmall = new Mat(); // bumper image shrunk

      CvSource outputStream = new CvSource("DriverView", VideoMode.PixelFormat.kMJPEG, 320, 240, 30);
      // CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG,
      // width, height, frames_per_sec);

      MjpegServer mjpegServer = new MjpegServer("serve_DriverView", 1185);

      mjpegServer.setSource(outputStream);

      while (true) {
         try {
            // start with both images the elevator
            Main.elevatorPipeline.getImage().copyTo(ImageOverlay);
            ImageOverlay.copyTo(ImageOutput);

            Main.bumperPipeline.getImage().copyTo(insert); // get the insert bumper image

            // Scaling the insert smaller
            // Imgproc.resize(insert, insertSmall, new Size(insert.rows() / 3, insert.rows()
            // / 3), 0, 0, Imgproc.INTER_AREA);
            Imgproc.resize(insert, insertSmall, new Size(), 0.333, 0.333, Imgproc.INTER_AREA); // 1/3 size

            //locate the small insert on the overlay
            int rowStart = insert.rows() - subMat.rows(); // for top/down put at bottom
            int rowEnd = rowStart + subMat.rows();
            int colStart = (insert.cols() - subMat.cols()) / 2; // for left/right align centers of the two images
            int colEnd = colStart + subMat.cols();

            subMat = ImageOverlay.submat(rowStart, rowEnd, colStart, colEnd); // define the insert area on the main image

            insertSmall.copyTo(subMat); // copy the insert to the overlay's insert area
            double alpha = 0.9f;

            // merge the original elevator image with the bumper insert overlaid elevator image (sorry--input+output=new output)
            // alpha+beta = 1 usually; gamma is added to image; = 0 for no gamma adjustment
            Core.addWeighted(ImageOverlay, alpha, ImageOutput, alpha, 0, ImageOutput);

            outputStream.putFrame(ImageOutput);

         } catch (Exception e) {
            System.out.println("ImageMerge error " + e);
         }
      }
   }
}