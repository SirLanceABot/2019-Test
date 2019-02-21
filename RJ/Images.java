import org.opencv.core.Mat;

/**
 * images
 */
public class Images
{
    Mat anImage;
    boolean isFreshImage;

    Images()
    {
        isFreshImage = false;
    }

    public synchronized void setImage(Mat aImage)
    {
        // System.out.println("setImage notifying");
        anImage = aImage;
        isFreshImage = true;
       notify();
    }

    public synchronized Mat getImage()
    {
        // System.out.println("getImage");
        if (!isFreshImage)
        {
            // System.out.println("getImage wait for fresh image");
            try
            {wait();}
            catch (Exception e){System.out.println("[Images] error " + e);}
        }        isFreshImage = false;
        // System.out.println("getImage done waiting - returning fresh Mat");
        return anImage;
    }

    public synchronized boolean isFreshImage()
    {
        return isFreshImage;
    }
}