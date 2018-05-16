package application;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;

import javafx.scene.image.ImageView;
//import javafx.stage.FileChooser;
import utilities.Utilities;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
//import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;


public class Controller {
	
	@FXML
	private ImageView imageView; // the image display window in the GUI
	
	private Mat image;
	
	private int width;
	private int height;
	private int sampleRate; // sampling frequency
	private int sampleSizeInBits;
	private int numberOfChannels;
	private double[] freq; // frequencies for each particular row
	private int numberOfQuantizionLevels;
	private int numberOfSamplesPerColumn;
	
	
	//@FXML
	//private Slider slider;
	
	private int[][] H_pre ; 
	private int[][] H_cur ;
	
	private VideoCapture capture;
	private ScheduledExecutorService timer;
	
	private String fileName;
	
	@FXML
	private void initialize() {
		width=32;
		height=32;
		H_pre = new int[6][6]; 
		H_cur = new int[6][6];

	
	}
	@FXML
	protected void getImageFilename(ActionEvent event) throws InterruptedException{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open a video File");
		File f = fileChooser.showOpenDialog(null);		
		if (f != null)
		{			
			String ext = f.getName().substring(f.getName().indexOf(".") + 1);

			if (ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("avi")|| ext.equalsIgnoreCase("wmv")|| ext.equalsIgnoreCase("mov"))
			{
				fileName=f.getAbsolutePath();
				
			}
			else
			{
				System.out.println("file type not correct");
				
			}
		}
		else {
			System.out.println("file does not exist");
		}
		
		
		
		
	}
	
	protected void stiByCopyingPixel(int command) throws InterruptedException {
		if (capture != null && capture.isOpened()) { // the video must be open			
			//create a matrix for sti		
			double totalFrameNumber = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
			Mat stiCopying= Mat.eye(32,(int)totalFrameNumber,CvType.CV_8UC3);
			int count=0;//count the current frame
			while(true)
			{
				Mat frame = new Mat();
				
				if (capture.read(frame)) { // decode successfully
					Image im = Utilities.mat2Image(frame);
					
					image=frame;
							
					if (image != null) {
						// resize the image
						Mat resizedImage = new Mat();
						Imgproc.resize(image, resizedImage, new Size(32, 32));
										
						//for column
						if(command==1) {
							for (int row = 0; row < 32; row++) {
							double buff[] = resizedImage.get(row, 16);
							//System.out.println(buff);
							stiCopying.put(row,count , buff);
							}
						}
						
						
						//for row
						if(command==0) {
							for (int col = 0; col < 32; col++) {
							double buff[] = resizedImage.get(16,col );
							stiCopying.put(col,count , buff);		
							}		
						}
						
				
					} 
			}
			else
				break;
				
			count=count+1;
			}
			
		
			imageView.setImage(Utilities.mat2Image(stiCopying));
		
		}
	
	}
	@FXML
	protected void CopyingPixelForColumn(ActionEvent event) throws InterruptedException {
	

		capture = new VideoCapture(fileName); // open video file
		if (capture.isOpened()) 
		{ // open successfully		
			if (capture != null && capture.isOpened())
			{ // the video must be open
				//double framePerSecond = capture.get(Videoio.CAP_PROP_FPS);			
				stiByCopyingPixel(1);							
			}
				
		}
				
				
	}
	@FXML
	protected void CopyingPixelForRow(ActionEvent event) throws InterruptedException {


		capture = new VideoCapture(fileName); // open video file
		if (capture.isOpened()) 
		{ // open successfully		
			if (capture != null && capture.isOpened())
			{ // the video must be open
				//double framePerSecond = capture.get(Videoio.CAP_PROP_FPS);

				stiByCopyingPixel(0);

				
			}
				
		}
				
				
	}		
		
	@FXML
	protected void stiByHistogram(ActionEvent event) throws InterruptedException {
		capture = new VideoCapture(fileName); // open video file
		double [] arr0= {0,0,0};
		double [] arr1= {255,255,255};
		if (capture.isOpened()) 
		{ // open successfully		
		//createFrameGrabber();
			
			// total frame number
			int totalFrameCount = (int)capture.get(Videoio.CAP_PROP_FRAME_COUNT); 
			
			//create matrix: frame number * column number
			//create  previous hist, current hist, temp hist
			Mat sti_hist = Mat.eye(32,totalFrameCount , CvType.CV_8UC1);
			
			//int[][] H_pre = new int[6][6]; 
			//int[][] H_cur = new int[6][6];
			int count = 0;
			// go through each frame
			while (true) {
				Mat frame = new Mat();
				if (capture.read(frame)) {
				
					
				//resize
				Mat resizedImage = new Mat();
				image=frame;
				Imgproc.resize(image, resizedImage, new Size(32, 32));
				for (int col = 0; col < width; col++) {
			
					//System.out.println(H_cur[0][0]);
				    // for each pixel
					for (int row = 0; row < height; row++) {
						// new (r,g)
						
						double pixel[] = resizedImage.get(row, col);
						double r = pixel[0]; // to do
						double g = pixel[1]; // to do
						double b = pixel[2];
						if ((r+g+b) !=0) {
							r=r/(r+g+b);
						    g=g/(r+g+b);
						}else {
							r = 0;
							g = 0;
						}
						
						//System.out.println(r);
						//System.out.println(g);
						// new coordinate of (r,g) and increment histogram(s)
						int coorR = (int)(r*5);
						int coorG = (int)(g*5);
						//int entry;
						
					    H_cur[coorR][coorG]++;
						//System.out.println(H_cur[coorR][coorG]);

							
					} // finish filling current histogram
					
				    // previous and current's difference
					if (count == 0) { 
							sti_hist.put(col,count, arr1); 
							//System.out.println(I);
							
					}
					else { 
						double I = getDiff(H_pre,H_cur);// to do  
						//System.out.println(I);

						// fill in I-array 
						// calculate I for each column and fill in sti matrix
						if (I>0.7) {
							sti_hist.put(col,count,arr1);
							//System.out.println(I);
						}
						else sti_hist.put(col,count,arr0);
					}
					
					copyAndResetCurMat();
					
					
				}
				}
				else break;
				count++;			
			}
			//System.out.println(count);
			imageView.setImage(Utilities.mat2Image(sti_hist));		
		}		
				
	}		
	

	
	
	protected double getDiff(int[][] pre, int[][] cur) {
		double res = 0;
		int p,c;
		for (int col = 0; col < 6; col++) {
			for (int row = 0; row < 6; row++) {
				p = pre[row][col];
				c = cur[row][col];
			
				res += p<c?p:c;
			} 
		}
		return res/32;
	}
	

	
	protected void copyAndResetCurMat() {
		for (int col = 0; col < 6; col++) {
			for (int row = 0; row < 6; row++) {
				int entry = H_cur[row][col];
				H_pre[row][col]=entry;
			}
		}
		// reset current Histogram
		H_cur = new int[6][6];
		
	}
		



}
