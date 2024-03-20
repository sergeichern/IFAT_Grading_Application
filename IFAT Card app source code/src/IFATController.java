
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Core;
import static org.opencv.core.Core.inRange;
import org.opencv.core.CvType;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import java.awt.Point;
import org.opencv.core.Size;

/**
 *
 * @author Davian Todd
 */
public class IFATController {
    // method to load all scanned cards from a folder
    public Hashtable<String, StudentIFATCard> loadCards(String folder) throws Exception {
        // create a hashtable to store a list of IFAT cards
        // the key will be the filename (for now) and the value is the IFAT card obj
        Hashtable<String, StudentIFATCard> studentCardTable = new Hashtable<>();
        try {
            // image codecs obj required for generating the Mat obj
            Imgcodecs imgcdx = new Imgcodecs();
            // create a file obj with the folder path
            File path = new File(folder);
            // array of files that stores all of the files in the folder
            File[] allFiles = path.listFiles();
            // array that stores the actual image for display
            BufferedImage[] allImages = new BufferedImage[allFiles.length];
            // loop through all the files in the folder
            for (int i = 0; i < allFiles.length; i++) {
                // store the filepath of the image
                String filepath = allFiles[i].getPath();
                // store the filename of the image
                String filename = allFiles[i].getName();
                // read each image file
                allImages[i] = ImageIO.read(allFiles[i]);
                // read each image file and create a Mat obj
                Mat cardMtx = imgcdx.imread(filepath);
                // create the IFAT card object
                StudentIFATCard card = new StudentIFATCard(filename, filepath, allImages[i], cardMtx);
                // add the card to the hash table
                studentCardTable.put(filename, card);
            }
        } catch (Exception e) {
            // handle exceptions with a popup window here
        }
        return studentCardTable;
    }

    // image detection method to be invoked
    public int[][] detect(StudentIFATCard card) {
        // set the bounds of the rectangle to define the roi
        Rect rect = new Rect(35, 110, 250, 350);
        // get the roi based on the rectangle
        Mat roi = new Mat(card.getMatObj(), rect);
        // set the roi for the student IFAT card
        card.setROI(roi);
        // create a grayscale Mat obj
        Mat gray = new Mat();
        // convert the roi to grayscale
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_RGB2GRAY);
        // create upper/lower bound Scalars for the grayscale image
        Scalar low = new Scalar(115);
        Scalar high = new Scalar(180);
        // Mat obj that stores the grayscale mask
        Mat mask = new Mat();
        // mask the pixels within the scalar range
        inRange(gray, low, high, mask);
        // set the inrange pixels to black
        gray.setTo(new Scalar(0), mask);
        // store the Mat obj as a 2D int matrix
        int[][] cardmtx = matTo2DArray(gray);
        // invoke image detection algorithm
        int[][] cardArray = mapToCard(cardmtx, card.getNumBoxes(), card.getNumQuestions());
        return cardArray;
    }

    // method to potentially detect student number, name and course code
    public void detectStudent(StudentIFATCard card) {

    }

    // method to print/view the details of the IFAT card
    public void printCard(StudentIFATCard card) {

    }

    // method to export the data
    public void exportData(Hashtable<String, StudentIFATCard> cardTable) {

    }

    // Image detection and card matrix mapping algorithm
    private static int[][] mapToCard(int[][] mtx, int boxes, int questions) {
        // initialize card array to the size of the IFAT card questions and boxes
        int[][] cardArray = new int[questions][boxes];

        // get the number of rows/cols from the 2D pixel array
        int numRows = mtx.length;
        int numCols = mtx[boxes].length;

        // set the threshold values
        int horThresh = 35; // width of the pixel density rectangle
        int verThresh = 21; // height of the pixel density rectangle
        int pdrArea = horThresh * verThresh; // area of the pixel density rectangle
        int partialThresh = 715; // indicates the min number of pixels before a box is considered partially scratched

        // row/col index for top left corner of the pixel density rectangle
        int rIndx;
        int cIndx;

        // stores the pixel value used for comparisons
        int pix;

        // point array that stores the coordinates for the point at the top left corner of the pixel density rectangle
        Point[] boxPnts = new Point[boxes * questions];
        // counter for the boxpoints array
        int k = 0;

        // traverse the 2D pixel array
        for (int i = 0; i < mtx.length; i++) {
            for (int j = 0; j < mtx[j].length; j++) {
                // assign the pixel value to pix
                pix = mtx[i][j];
                // if pix is 0, store the starting coordinates
                if (pix == 0) {
                    rIndx = i; // top left x coord
                    cIndx = j; // top left y coord

                    int csum = 0; // stores the sum of pixels going down a col based on the vertical threshold
                    int rsum = 0; // stores the sum of pixels going across a row based on the horizontal threshold
                    int area = 0; // stores the area of the masked region of the pixel density rectangle

                    // look ahead (down) by vertical threshold
                    if ((rIndx + verThresh) < mtx.length) { // ensures no index out of bounds while going down
                        for (int cx = rIndx; cx < (rIndx + verThresh); cx++) {
                            // sum each pixel value while keeping the column index static
                            csum += mtx[cx][cIndx];
                        }
                    }
                    // look ahead (across) by horizontal threshold
                    if ((cIndx + horThresh) < (mtx[j].length)) { // ensures no index out of bounds while going across
                        for (int cy = cIndx; cy < (cIndx + horThresh); cy++) {
                            // sum each pixel value while keeping the row index static
                            rsum += mtx[rIndx][cy];
                        }
                    }
                    // Potential box is found based on the pixel density rectangle
                    if ((csum == 0) && (rsum == 0)) {
                        // stores the number of masked pixels within the pixel denstity rectangle
                        int numPix = 0;
                        // traverse the pixel density rectangle
                        if ((rIndx + verThresh) < mtx.length) {
                            for (int cx = rIndx; cx < (rIndx + verThresh); cx++) {
                                if ((cIndx + horThresh) < mtx[j].length) {
                                    for (int cy = cIndx; cy < (cIndx + horThresh); cy++) {
                                        // count the number of masked pixels within the pixel density rectangle
                                        if (mtx[cx][cy] == 0) {
                                            numPix++;
                                        }
                                    }
                                }
                            }
                        }
                        // if the number of masked pixels exceeds the partial threshold (unscratched box detected)
                        if (numPix > partialThresh) {
                            // traverse the pixel density rectangle
                            if ((rIndx + verThresh) < mtx.length) {
                                for (int cx = rIndx; cx < (rIndx + verThresh); cx++) {
                                    if ((cIndx + horThresh) < mtx[j].length) {
                                        for (int cy = cIndx; cy < (cIndx + horThresh); cy++) {
                                            // set each masked pixel to 1
                                            mtx[cx][cy] = 1;
                                            // calculate the area of the pdr by summing the 1's in the pdr
                                            area += mtx[cx][cy];
                                        }
                                        // if the area of the masked region matches the precalculated area of the pdr
                                        if (area == pdrArea) {
                                            // create a new point with the row index as the x coord and col index as the y coord
                                            // store the point in the boxpoints array
                                            boxPnts[k] = new Point(rIndx, cIndx);
                                            k++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // end of 2D pixel array traversal

        // set each value in the card array to 1 (scratched)
        for (int i = 0; i < cardArray.length; i++) {
            for (int j = 0; j < cardArray[j].length; j++) {
                cardArray[i][j] = 1;
            }
        }
        // number of unscratched boxes
        int numUnscratched = 0;
        // pointers that store the indicies for the card array
        int xPtr = 0;
        int yPtr = 0;
        // cardArray mapping algorithm
        for (int i = 0; i < boxPnts.length; i++) {
            // set inital lower x/y bounds
            int lowerX = 0;
            int lowerY = 0;
            // set initial upper x/y bounds
            int upperX = (numRows / questions) * 1;
            int upperY = (numCols / boxes) * 1;

            if (boxPnts[i] != null) {
                // stores whether the x/y indicies have been found
                boolean xfound = false;
                boolean yfound = false;
                int j = 1;
                // while loop that traverses the rows of the cardArray
                while (!xfound && j < (cardArray.length) + 1) {
                    // if statement that checks if the x coord is within the bounds
                    if (boxPnts[i].x > lowerX && boxPnts[i].x < upperX) {
                        // set the xPtr to the required index
                        xPtr = j - 1;
                        // set xfound to true to end the while loop
                        xfound = true;
                        // if the x coord is not in bounds then raise the upper and lower bounds
                    } else {
                        lowerX = upperX;
                        upperX = (numRows / questions) * (j + 1);
                    }
                    j++;
                }
                j = 1;
                // while loop that traverses the cols of the cardArray
                while (!yfound && j < (cardArray[j].length) + 1) {
                    // if statement that checks if the y coord is within the bounds
                    if ((boxPnts[i].y > lowerY) && boxPnts[i].y < upperY) {
                        // set the yPtr to the required index
                        yPtr = j - 1;
                        // set xfound to true to end the while loop
                        yfound = true;
                        // if the y coord is not in bounds then raise the upper and lower bounds
                    } else {
                        lowerY = upperY;
                        upperY = (numCols / boxes) * (j + 1);
                    }
                    j++;
                }
                // set the value at the x/yPtr index to 0 (unscratched) in the cardArray
                cardArray[xPtr][yPtr] = 0;
                // increment the number of unscratched
                numUnscratched++;
            }
        }
        return cardArray;
    }

    // static helper method to convert a Mat obj to a buffered image for display
    private static BufferedImage convertToBuff(Mat mtx) throws IOException {
        //Encoding the image
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mtx, matOfByte);

        //Storing the encoded Mat in a byte array
        byte[] byteArray = matOfByte.toArray();

        //Preparing the Buffered Image
        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage bufImage = ImageIO.read(in);
        return bufImage;
    }

    // static helper method to convert a Mat obj to a 2D array of integers
    private static int[][] matTo2DArray(Mat mtx) {
        // get rows/cols from mat obj
        int numRows = mtx.rows();
        int numCols = mtx.cols();
        int channels = mtx.channels();
        int[][] intMtx = new int[numRows][numCols];

        // copy mat obj to 2d int array
        intMtx = new int[numRows][numCols];
        byte[] data = new byte[(int) mtx.total() * mtx.channels()];
        mtx.get(0, 0, data);
        int width = mtx.width();
        int height = mtx.height();
        int numpix = (int) mtx.total();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                for (int i = 0; i < channels; i++) {
                    intMtx[r][c] = data[r * (width * channels) + c * channels + i] & 0xff;
                }
            }
        }
        return intMtx;
    }
}