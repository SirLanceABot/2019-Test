//import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraFrame
{
    private Mat mat;
    private boolean isFreshImage;

    CameraFrame(int rows, int cols)
    {
       // mat = new Mat(/*rows, cols, CvType.CV_8UC3*/);
        isFreshImage = false;
    }

    public synchronized void setImage(Mat amat)
    {
        // System.out.println("setImage notifying");
        //amat.copyTo(mat);
        mat = amat;
        isFreshImage = true;
        notify();
    }

    public synchronized Mat getImage()
    {
        //Mat amat = new Mat();
        // System.out.println("getImage");
        if (!isFreshImage)
        {
            // System.out.println("getImage wait for fresh image");
            try
            {wait();}
            catch (Exception e){System.out.println("[CameraFrame] error " + e);}
        }
        //mat.copyTo(amat);
        isFreshImage = false;
        // System.out.println("getImage done waiting - returning fresh Mat");
       // return amat;
       return mat;
    }
}