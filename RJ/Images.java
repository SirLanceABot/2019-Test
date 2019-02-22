import org.opencv.core.Mat;

/**
 * images for merge process
 */
public class Images
{
    Mat mat = new Mat();
    boolean isFreshImage = false;

    public synchronized void setImage(Mat mat)
    {
        // System.out.println("setImage notifying");
        mat.copyTo(this.mat);
        this.isFreshImage = true;
        notify();
    }

    public synchronized Mat getImage()
    {
        // System.out.println("getImage");
        if (!this.isFreshImage)
        {
            // System.out.println("getImage wait for fresh image");
            try
            {
                wait();
            } catch (Exception e)
            {
                System.out.println("[Images] error " + e);
            }
        }
        this.isFreshImage = false;
        // System.out.println("getImage done waiting - returning fresh Mat");
        Mat returnMat = new Mat();
        this.mat.copyTo(returnMat);
        return returnMat;
    }

    public synchronized boolean isFreshImage()
    {
        return this.isFreshImage;
    }
}