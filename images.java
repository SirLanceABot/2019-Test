import org.opencv.core.Mat;

/**
 * images
 */
public class images {
    Mat anImage;
    boolean isFreshImage;

    images() {
        isFreshImage = false;
    }

    public synchronized void setImage(Mat aImage) {
       // System.out.println("setImage notifying");
        anImage = aImage;
        isFreshImage = true;
        notify();
    }

    public synchronized Mat getImage() throws InterruptedException {
       // System.out.println("getImage");
        if(!isFreshImage) {
           // System.out.println("getImage wait for fresh image");
            wait();
        }
        isFreshImage = false;
        //System.out.println("getImage done waiting - returning fresh Mat");
        return anImage;
    }

    public synchronized boolean isFreshImage() {
        return isFreshImage;
    }
}