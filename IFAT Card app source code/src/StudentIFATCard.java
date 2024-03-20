
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
import java.util.Hashtable;
import java.util.Map;
import org.opencv.core.Size;
/**
 *
 * @author Davian Todd
 * 
 */
public class StudentIFATCard {
    // student related attributes
    private int stdNo; // stores the student number
    private String stdName; // stores the student name
    private String course; // stores the course code for the assessment
    // IFAT card representation attributes
    private int numQuestions; // stores the number of questions of the IFAT assessment
    private int numBoxes; // stores the number of boxes of the IFAT assessment
    private Hashtable<String, Integer> weights; // stores the percentage weights for each scratched box
    private int[] scores; // stores the attempt score (grade) for each question
    private int[][] cardArray; // stores a digital representation of the IFAT card as a 2D matrix of numQuestions x numBoxes
    private double grade; // stores the final grade of the assessment
    // image detection attributes
    private String filepath; //stores the filepath of the scanned card
    private String filename; // stores the filename of the scanned card
    private BufferedImage imgCard; // stores the actual image of the scanned card
    private Mat matCard; // stores the Mat obj (digital representation) of the scanned IFAT card
    private Mat roi; // stores the Mat obj that defines the region of interest of the scanned image
    private Rect roiRect; // stores a rectangle obj that defines the bounds of the roi
    
    // default constructor
    public StudentIFATCard(){
        // set default values
        numQuestions = 10;
        numBoxes = 5;
        // initialize default cardArray
        cardArray = new int[numQuestions][numBoxes];
        // set each value in the card array to 1 (scratched) by default
        for (int i = 0; i < cardArray.length; i++) {
            for (int j = 0; j < cardArray[j].length; j++) {
                cardArray[i][j] = 1;
            }
        }
        // initialize the scores array
        scores = new int[numQuestions];
        // initialize the default weights (in percentages) hash table
        weights = new Hashtable<>();
        weights.put("Attempt 1", 100); // attempt 1 gets 100%
        weights.put("Attempt 2", 66); // attempt 2 gets 66%
        weights.put("Attempt 3", 33); // attempt 3 gets 33%
        weights.put("Attempt 4", 25); // attempt 4 gets 25%
        weights.put("Attempt 5", 0); // attempt 5 gets 0%
    }
    // 3 argument constructor
    public StudentIFATCard(String name, String path, BufferedImage img, Mat cardMtx){
        // set the filename
        filename = name;
        // set the filepath
        filepath = path;
        // set the buffered image
        imgCard = img;
        // set the Mat obj
        matCard = cardMtx;
        
        // set default values
        numQuestions = 10;
        numBoxes = 5;
        // initialize default cardArray
        cardArray = new int[numQuestions][numBoxes];
        // set each value in the card array to 1 (scratched) by default
        for (int i = 0; i < cardArray.length; i++) {
            for (int j = 0; j < cardArray[j].length; j++) {
                cardArray[i][j] = 1;
            }
        }
        // initialize the scores array
        scores = new int[numQuestions];
        // initialize the default weights (in percentages) hash table
        weights = new Hashtable<>();
        weights.put("Attempt 1", 100); // attempt 1 gets 100%
        weights.put("Attempt 2", 66); // attempt 2 gets 66%
        weights.put("Attempt 3", 33); // attempt 3 gets 33%
        weights.put("Attempt 4", 25); // attempt 4 gets 25%
        weights.put("Attempt 5", 0); // attempt 5 gets 0%
    }
    // grading algorithm
    public void calcScore(int[] answerKey){
        int numScratched; // number of unscratched boxes
        int answerIndex; // stores the index of the correct answer in the asnwer key
        boolean correctAnswer; // stores whether or not the correct answer is among the scratched boxes
        // outer for loop for each row/question
        for (int i = 0; i < cardArray.length; i++) {
            numScratched = 0; // set numscratched to 0 for each row iteration
            answerIndex = answerKey[i]; // get the answer key index for each row iteration
            correctAnswer = false; // set the correct answer to false for each row iteration
            // if the correct answer was scratched then set the correct answer to true
            if(cardArray[i][answerIndex] == 1)
                correctAnswer = true;
            // if the correct answer was scratched then calculate the score(percentage) for each question
            if(correctAnswer){
                // loop through the inner array
                for (int j = 0; j < cardArray[j].length; j++) {
                    // calculate the number of scratched boxes
                    numScratched += cardArray[i][j];
                }
                // switch statement that maps the number of scratched boxes to a score in the weights table
                switch(numScratched){
                    case 1:
                        scores[i] = weights.get("Attempt 1");
                        break;
                    case 2:
                        scores[i] = weights.get("Attempt 2");
                        break;
                    case 3:
                        scores[i] = weights.get("Attempt 3");
                        break;
                    case 4:
                        scores[i] = weights.get("Attempt 4");
                        break;
                    case 5:
                        scores[i] = weights.get("Attempt 5");
                        break;
                    default:
                        scores[i] = weights.get("Attempt 5");
                        break;
                }
                // if the correct answer was not scratched then the score is 0
            } else{
                scores[i] = weights.get("Attempt 5");
            }
        }
        // calculate the sum of the scores array
        int sum = 0;
        for (int i = 0; i < scores.length; i++) {
            sum += scores[i];
        }
        // calculate the final grade
        grade = sum / numQuestions;
    }
    // helper method to print the cardArray to console
    public void printArray(int[][] mtx) {
        for (int i = 0; i < mtx.length; i++) {
            for (int j = 0; j < mtx[j].length; j++) {
                System.out.print(mtx[i][j] + "\t");
            }
            System.out.print("\n");
        }
    }
    // getter methods
    public int getStudentNo(){
        return stdNo;
    }
    public String getStudentName(){
        return stdName;
    }
    public String getCourse(){
        return course;
    }
    public int getNumQuestions(){
        return numQuestions;
    }
    public int getNumBoxes(){
        return numBoxes;
    }
    public Hashtable getWeights(){
        return weights;
    }
    public int[] getScores(){
        return scores;
    }
    public int[][] getCardArray(){
        return cardArray;
    }
    public double getGrade(){
        return grade;
    }
    public String getFilePath(){
        return filepath;
    }
    public String getFileName(){
        return filename;
    }
    public BufferedImage getBuffImage(){
        return imgCard;
    }
    public Mat getCardMatObj(){
        return matCard;
    }
    public Mat getCardROI(){
        return roi;
    }
    public Rect getROIBounds(){
        return roiRect;
    }
    public Mat getMatObj(){
        return matCard;
    }
    // setter methods
    public void setStudentNO(int stdNo){
        this.stdNo = stdNo;
    }
    public void setStudentName(String name){
        stdName = name;
    }
    public void setCourse(String course){
        this.course = course;
    }
    public void setNumQuestions(int questions){
        numQuestions = questions;
    }
    public void setNumBoxes(int boxes){
        numQuestions = boxes;
    }
    public void setGrade(double grade){
        this.grade = grade;
    }
    public void setScores(int[] qscores){
        for (int i = 0; i < scores.length; i++) {
            scores[i] = qscores[i];
        }
    }
    public void setCardArray(int[][] card){
        for (int i = 0; i < cardArray.length; i++) {
            for (int j = 0; j < cardArray[j].length; j++) {
                cardArray[i][j] = card[i][j];
            }
        }
    }
    public void setWeights(Hashtable<String, Integer> newWeights){
        weights.clear();
        for(Map.Entry<String, Integer> e : newWeights.entrySet()){
            weights.put(e.getKey(), e.getValue());
        }
    }
    public void setBuffImage(BufferedImage img){
        imgCard = img;
    }
    public void setROI(Mat roi){
        this.roi = roi;
    }
    public void setROIRect(Rect roiRect){
        this.roiRect = roiRect;
    }
    public void setFilePath(String path){
        filepath = path;
    }
    public void setFileName(String name){
        filename = name;
    }
}
