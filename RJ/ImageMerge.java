import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;

public class ImageMerge implements Runnable
{
    // This object is used to send the image to the Dashboard
    private CvSource outputStream;
    
    @Override
    public void run()
    {
        System.out.println("[ImageMerge] Thread Started");

        Mat ImageOverlay = new Mat(); // main image from elevator
        Mat ImageOutput = new Mat(); // main image from elevator + small bumper image inserted then weighted merge
                                     // and saved
        Mat subMat = new Mat(); // place for small bumper image inserted into main elevator image
        Mat insert = new Mat(); // bumper image
        Mat insertSmall = new Mat(); // bumper image shrunk

        outputStream = CameraServer.getInstance().putVideo("MergedImages", 320, 240);

        // CvSource outputStream = new CvSource("DriverView", VideoMode.PixelFormat.kMJPEG, 320, 240, 30);
        // // CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG, width, height, frames_per_sec);

        // MjpegServer mjpegServer = new MjpegServer("serve_DriverView", 1185);

        // mjpegServer.setSource(outputStream);

        while (true)
        {
            try
            {
                if (Main.elevatorPipeline.getImage().dims() <= 1)
                {
                    System.out.println("ImageMerge] elevator too few dimensions");
                    Main.bumperPipeline.getImage().copyTo(ImageOutput);
                    Imgproc.putText(ImageOutput, "Bumper Contours Only", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(100, 100, 255), 1);
                }
                else if (Main.bumperPipeline.getImage().dims() <= 1)
                {
                    System.out.println("ImageMerge] bumper too few dimensions");
                    Main.elevatorPipeline.getImage().copyTo(ImageOutput);
                    Imgproc.putText(ImageOutput, "Elevator Contours Only", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(100, 100, 255), 1);
                }
                else
                {
                    // start with both images the elevator
                    Main.elevatorPipeline.getImage().copyTo(ImageOverlay);
                    ImageOverlay.copyTo(ImageOutput);

                    Main.bumperPipeline.getImage().copyTo(insert); // get the insert bumper image

                    // Scaling the insert smaller
                    // Imgproc.resize(insert, insertSmall, new Size(insert.rows() / 3, insert.rows()
                    // / 3), 0, 0, Imgproc.INTER_AREA);
                    Imgproc.resize(insert, insertSmall, new Size(), 0.5, 0.5, Imgproc.INTER_AREA);

                    // locate the small insert on the overlay
                    int rowStart = ImageOutput.rows() - insertSmall.rows(); // for top/down put at bottom
                    int rowEnd = rowStart + insertSmall.rows();
                    int colStart = (ImageOutput.cols() - insertSmall.cols()) / 2; // for left/right align centers of the two images
                    int colEnd = colStart + insertSmall.cols();

                    subMat = ImageOverlay.submat(rowStart, rowEnd, colStart, colEnd); // define the insert area on the main image

                    insertSmall.copyTo(subMat); // copy the insert to the overlay's insert area
                    double alpha = 0.85f;
                    double beta = 0.25f;
                    // merge the original elevator image with the bumper insert overlaid elevator
                    // image (input+output=new output)
                    // alpha+beta = 1 usually; gamma is added to image; = 0 for no gamma adjustment
                    Core.addWeighted(ImageOverlay, alpha, ImageOutput, beta, 0, ImageOutput);

                    Imgproc.putText(ImageOutput, "Merged Images", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX, 0.5,
                            new Scalar(100, 100, 255), 1);
                }

                outputStream.putFrame(ImageOutput);

            } catch (Exception e)
            {
                System.out.println("[ImageMerge] error " + e);
            }
        }
    }
}