//Tzang, Tsung-Ting
//Student ID 4870930866

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.lang.*;


public class imageReader {
   
    public static void main(String[] args) {
   	
        
        Scanner scan = new Scanner( System.in );
        int Qlevel = 0;
        int Mode = 0;
        int Latency = 0;
        String fileName = "image1.rgb";//Default
        
        printInfo(1);
        while( true ){
            System.out.print( "Please Enter Quantization Level(0~7)$ " );
            Qlevel = Integer.parseInt(scan.nextLine());
            if( Qlevel >= 0 && Qlevel <= 7 ) break;
        }
        
        printInfo(2);
        while( true ){
            System.out.print( "Please Enter which Mode(1~3)$ " );
            Mode = Integer.parseInt(scan.nextLine());
            if( Mode >= 0 && Mode <= 3 ) break;
        }
        
        printInfo(3);
        while( true ){
            System.out.print( "Please Enter Latency$ " );
            Latency = Integer.parseInt(scan.nextLine());
            if( Latency >= 0 ) break;
        }

        int width = 352;//Integer.parseInt(args[1]);
        int height = 288;//Integer.parseInt(args[2]);

        BufferedImage imgTest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

          // Use a label to display the image
        JFrame frame = new JFrame("Assignment II");
        JLabel label = new JLabel(new ImageIcon(img), JLabel.CENTER);
        Dimension screenSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());

        int window_X = screenSize.width / 4 ;
        int window_Y = screenSize.height / 4;
        frame.getContentPane().add(label, BorderLayout.CENTER);
   
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                System.exit(0);
            }
        });

        frame.pack();
        frame.setLocation(window_X, window_Y);
        frame.setVisible(true);

        try {
	        File file = new File(fileName);
	        InputStream is = new FileInputStream(file);
        
	        long len = file.length();
	        byte[] bytes_R = new byte[(int)len];
            Byte[] bytes = new Byte[(int)len];

            int[][] tmpPixel_Oringin = new int[height][width];
            int[][] Pixel_Compression = new int[height][width];
        
            int[][][] RGB_Origin = new int[height][width][3];
            double[][][][][] RGB_Blocks = new double[height/8][width/8][8][8][3];
        
            int[][][][][] RGB_FeqBlocks = new int[height/8][width/8][8][8][3];
            int[][][][][] RGB_QuantizeB = new int[height/8][width/8][8][8][3];
            int[][][][][] RGB_DQuantizeB = new int[height/8][width/8][8][8][3];
            int[][][][][] RGB_ItnBlocks = new int[height/8][width/8][8][8][3];
            int[][][][][] RGB_ZigBlocks = new int[height/8][width/8][8][8][3];
            int[][][][] Pixel_AfterCompression = new int[height/8][width/8][8][8];
        
            byte[][][][][] RGB_DeItnBlocks = new byte[height/8][width/8][8][8][3];
            double[][][][][] RGB_tmpBlocks = new double[height/8][width/8][8][8][3];
        
	        int offset = 0;
            int numRead = 0;
            while (offset < bytes_R.length && (numRead=is.read(bytes_R, offset, bytes_R.length-offset)) >= 0) {
               offset += numRead;
            }
        
            for(int i = 0; i < (int)len; i++)  bytes[i] = bytes_R[i];
        
            //Load Oringinal
            LoadImage( RGB_Origin, bytes, tmpPixel_Oringin, height, width );
            //Break into 8x8 blocks
            Partition( RGB_Origin, RGB_Blocks, height, width );
            //DCT
            DCT( RGB_Blocks, RGB_tmpBlocks, RGB_FeqBlocks, height, width );
            //Quantize
            Quantize( RGB_QuantizeB, RGB_FeqBlocks, Qlevel, height, width );
            //DeQuantize
            DeQuantize( RGB_DQuantizeB, RGB_QuantizeB, Qlevel, height, width );
            //
            switch( Mode ){
                case 1:
                    //IDCT
                    IDCT( RGB_DeItnBlocks, RGB_ItnBlocks, RGB_DQuantizeB, 
                         RGB_tmpBlocks, Pixel_AfterCompression, img, height, width, Mode);
                    //Mode I display
                    for(int i = 0; i < height/8; i++){
                        for(int j = 0; j < width/8; j++){
                            for(int y = 0; y < 8; y++){
                                for(int x = 0; x < 8; x++){

                                    byte RR = (RGB_DeItnBlocks[i][j][y][x][0] );
                                    byte GG = (RGB_DeItnBlocks[i][j][y][x][1] );
                                    byte BB = (RGB_DeItnBlocks[i][j][y][x][2] );

                                    Pixel_AfterCompression[i][j][y][x] = 0xff000000 
                                                | ((RR & 0xff) << 16) | ((GG & 0xff) << 8) | (BB & 0xff);

                                    img.setRGB(j*8+x, i*8+y, Pixel_AfterCompression[i][j][y][x]);
                                }
                            }
                            label.repaint();
                            Thread.sleep(Latency);   
                        }
                    }
                    break;
                case 2:
                    int col = 1, row = 1;
                    for( int zigzag = 0; zigzag < 8*8; zigzag ++ ){

                        for(int i = 0; i < height/8; i++){
                            for(int j = 0; j < width/8; j++){
                                //Get DC AC1 AC2...
                                RGB_ZigBlocks[i][j][row-1][col-1][0] = RGB_DQuantizeB[i][j][row-1][col-1][0];
                                RGB_ZigBlocks[i][j][row-1][col-1][1] = RGB_DQuantizeB[i][j][row-1][col-1][1];
                                RGB_ZigBlocks[i][j][row-1][col-1][2] = RGB_DQuantizeB[i][j][row-1][col-1][2];
                            }
                        }

                        //IDCT
                        IDCT( RGB_DeItnBlocks, RGB_ItnBlocks, RGB_ZigBlocks, 
                             RGB_tmpBlocks, Pixel_AfterCompression, img, height, width, Mode );

                        label.repaint();
                        Thread.sleep(Latency);

                        //Get Next Coeffecient
                        if( (row+col)%2 == 0 ){ //even
                            if( col < 8 ) col++;
                            else row+=2;
                            if( row > 1 ) row--;
                        }
                        else{
                            if( row < 8 ) row++;
                            else col+=2;
                            if( col > 1 ) col--;
                        }
                    }
                    break;
                case 3:
                    //IDCT
                    IDCT( RGB_DeItnBlocks, RGB_ItnBlocks, RGB_DQuantizeB, 
                         RGB_tmpBlocks, Pixel_AfterCompression, img, height, width, Mode);
                    //Mode III Display
                    int mask = 0x80;
                    for( int countBits = 0; countBits < 8; countBits++ ){
                        for(int i = 0; i < height/8; i++){
                            for(int j = 0; j < width/8; j++){
                                for(int y = 0; y < 8; y++){
                                    for(int x = 0; x < 8; x++){

                                        byte RR = (RGB_DeItnBlocks[i][j][y][x][0] );
                                        byte GG = (RGB_DeItnBlocks[i][j][y][x][1] );
                                        byte BB = (RGB_DeItnBlocks[i][j][y][x][2] );

                                        Pixel_AfterCompression[i][j][y][x] = 0xff000000 | (((RR & 0xff & mask)) << 16)
                                        | (((GG & 0xff & mask)) <<  8)
                                        | (((BB & 0xff & mask)));

                                        img.setRGB(j*8+x, i*8+y, Pixel_AfterCompression[i][j][y][x]);
                                    }
                                }
                            }
                        }
                        label.repaint();
                        Thread.sleep(Latency);
                        mask = (mask >> 1) | 0x80;
                    }
                    //End bits countin
                    break;
                default:
                    break;
            }
        
        
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch( Exception e ){
            e.printStackTrace();
        }
    
   }
    
    
    private static void printInfo( int option ){
        if( option == 1 ){
            System.out.println("-------------------------------------------------------");
            System.out.println("  A factor that will increase or decrease compression");
            System.out.println("-------------------------------------------------------");
            return;
        }
        if( option == 2 ){
            System.out.println("---------------------------------------------------------");
            System.out.println("  Mode 1: Base Line Delivery - Sequential");
            System.out.println("  Mode 2: Progressive Delivery - Spectral Selection");
            System.out.println("  Mode 3: Progressive Delivery - Succesive Bit Approx.");
            System.out.println("---------------------------------------------------------");
            return;
        }
        if( option == 3 ){
            System.out.println("---------------------------------------------------------");
            System.out.println("  Simulate low and high bandwidth in milliseconds");
            System.out.println("---------------------------------------------------------");
            return;
        }
        
    }
    
    private static void LoadImage( int[][][] RGB_Origin, Byte[] bytes, int[][] tmpPixel_Oringin, int height, int width ){
        int index = 0;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
                
				byte a = 0;
				byte r = bytes[index];
                byte g = bytes[index+height*width];
				byte b = bytes[index+height*width*2];
                
                RGB_Origin[y][x][0] = 0x00000000 | (r & 0xff);
                RGB_Origin[y][x][1] = 0x00000000 | (g & 0xff);
                RGB_Origin[y][x][2] = 0x00000000 | (b & 0xff);
                
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                tmpPixel_Oringin[y][x] = pix;
                //img.setRGB(x,y,pix);
				index++;
			}
		}
    }
    
    private static void Partition( int[][][] RGB_Origin, double[][][][][] RGB_Blocks, int height, int width ){
        for(int i = 0; i < height; i+=8){
            for(int j = 0; j < width; j+=8){
                int x=0, y=0;
                for(y=0; y<8; y++){
                    for(x=0; x<8; x++){
                        RGB_Blocks[i/8][j/8][y][x][0] = RGB_Origin[i+y][j+x][0];
                        RGB_Blocks[i/8][j/8][y][x][1] = RGB_Origin[i+y][j+x][1];
                        RGB_Blocks[i/8][j/8][y][x][2] = RGB_Origin[i+y][j+x][2];
                    }
                }
            }
        }
    }
    
    private static void DCT( double[][][][][] RGB_Blocks, 
                             double[][][][][] RGB_tmpBlocks, 
                             int[][][][][] RGB_FeqBlocks, int height, int width ){
        for(int i = 0; i < height/8; i++){
            for(int j = 0; j < width/8; j++){
                double Cu=0, Cv=0;
                for(int v = 0; v < 8; v++){
                    if( v == 0 )
                        Cv = 1/Math.sqrt(2.0);
                    else
                        Cv = 1;
                    
                    for(int u = 0; u < 8; u++){
                        RGB_tmpBlocks[i][j][v][u][0] = 0;
                        RGB_tmpBlocks[i][j][v][u][1] = 0;
                        RGB_tmpBlocks[i][j][v][u][2] = 0;
                        //Formula
                        for(int y = 0; y < 8; y++){
                            double COSYV = Math.cos(((2.0*y+1.0)*v*Math.PI)/16.0);
                            for(int x = 0; x < 8; x++){
                                double COSXU = Math.cos(((2.0*x+1.0)*u*Math.PI)/16.0);
                                if( u == 0 )
                                    Cu = 1/Math.sqrt(2.0);
                                else
                                    Cu = 1;
                
                                RGB_tmpBlocks[i][j][v][u][0] += (1.0/4.0) * Cu * Cv * 
                                    (RGB_Blocks[i][j][y][x][0]) * COSXU * COSYV;
                                RGB_tmpBlocks[i][j][v][u][1] += (1.0/4.0) * Cu * Cv * 
                                    (RGB_Blocks[i][j][y][x][1]) * COSXU * COSYV;
                                RGB_tmpBlocks[i][j][v][u][2] += (1.0/4.0) * Cu * Cv * 
                                    (RGB_Blocks[i][j][y][x][2]) * COSXU * COSYV;
                            }
                        }
                        RGB_FeqBlocks[i][j][v][u][0] = (int)Math.round( RGB_tmpBlocks[i][j][v][u][0] );
                        RGB_FeqBlocks[i][j][v][u][1] = (int)Math.round( RGB_tmpBlocks[i][j][v][u][1] );
                        RGB_FeqBlocks[i][j][v][u][2] = (int)Math.round( RGB_tmpBlocks[i][j][v][u][2] );
                    }
                }
            }
        }
    }
    
    
    private static void Quantize( int[][][][][] RGB_QuantizeB, 
                                 int[][][][][] RGB_FeqBlocks, int Qlevel, int height, int width ){
        for(int i = 0; i < height/8; i++){
            for(int j = 0; j < width/8; j++){
                for(int v = 0; v < 8; v++){
                    for(int u = 0; u < 8; u++){
                        RGB_QuantizeB[i][j][v][u][0] = (int)Math.round( RGB_FeqBlocks[i][j][v][u][0] / Math.pow(2, Qlevel) );
                        RGB_QuantizeB[i][j][v][u][1] = (int)Math.round( RGB_FeqBlocks[i][j][v][u][1] / Math.pow(2, Qlevel) );
                        RGB_QuantizeB[i][j][v][u][2] = (int)Math.round( RGB_FeqBlocks[i][j][v][u][2] / Math.pow(2, Qlevel) );
                    }
                }
            }
        }
    }
    
    private static void DeQuantize( int[][][][][] RGB_DQuantizeB, 
                                   int[][][][][] RGB_QuantizeB, int Qlevel, int height, int width ){
        for(int i = 0; i < height/8; i++ ){
            for(int j = 0; j < width/8; j++){
                for(int v = 0; v < 8; v++){
                    for(int u = 0; u < 8; u++){
                        RGB_DQuantizeB[i][j][v][u][0] = (int)(RGB_QuantizeB[i][j][v][u][0] * Math.pow(2, Qlevel));
                        RGB_DQuantizeB[i][j][v][u][1] = (int)(RGB_QuantizeB[i][j][v][u][1] * Math.pow(2, Qlevel));
                        RGB_DQuantizeB[i][j][v][u][2] = (int)(RGB_QuantizeB[i][j][v][u][2] * Math.pow(2, Qlevel));
                    }
                }
            }
        }
    }
    
    private static void IDCT( byte[][][][][] RGB_DeItnBlocks, 
                              int[][][][][] RGB_ItnBlocks, 
                              int[][][][][] RGB_DQuantizeB, 
                              double[][][][][] RGB_tmpBlocks,
                              int[][][][] Pixel_AfterCompression,
                              BufferedImage img, int height, int width, int mode ){
        
        for(int i = 0; i < height/8; i++){
            for(int j = 0; j < width/8; j++){
                
                for(int y = 0; y < 8; y++){
                    for(int x = 0; x < 8; x++){
                        RGB_tmpBlocks[i][j][y][x][0] = 0;
                        RGB_tmpBlocks[i][j][y][x][1] = 0;
                        RGB_tmpBlocks[i][j][y][x][2] = 0;
                        //Formula
                        double Cv = 0; double Cu = 0;
                        for(int v = 0; v < 8; v++){
                            double COSYV = Math.cos(((2.0*y+1.0)*v*Math.PI)/16.0);
                            if( v == 0 )
                                Cv = 1/Math.sqrt(2.0);
                            else
                                Cv = 1;
                            for(int u = 0; u < 8; u++){
                                double COSXU = Math.cos(((2.0*x+1.0)*u*Math.PI)/16.0);
                                if( u == 0 )
                                    Cu = 1/Math.sqrt(2.0);
                                else
                                    Cu = 1;
            
                                RGB_tmpBlocks[i][j][y][x][0] += (1.0/4.0) * Cu * Cv * 
                                            RGB_DQuantizeB[i][j][v][u][0] * COSXU * COSYV;
                                RGB_tmpBlocks[i][j][y][x][1] += (1.0/4.0) * Cu * Cv * 
                                            RGB_DQuantizeB[i][j][v][u][1] * COSXU * COSYV;
                                RGB_tmpBlocks[i][j][y][x][2] += (1.0/4.0) * Cu * Cv * 
                                            RGB_DQuantizeB[i][j][v][u][2] * COSXU * COSYV;
                            }
                        }
                        RGB_ItnBlocks[i][j][y][x][0] = (int)( RGB_tmpBlocks[i][j][y][x][0] );
                        RGB_ItnBlocks[i][j][y][x][1] = (int)( RGB_tmpBlocks[i][j][y][x][1] );
                        RGB_ItnBlocks[i][j][y][x][2] = (int)( RGB_tmpBlocks[i][j][y][x][2] );
                        
                        if( RGB_ItnBlocks[i][j][y][x][0] < 0  ) RGB_ItnBlocks[i][j][y][x][0] = 0;
                        if( RGB_ItnBlocks[i][j][y][x][1] < 0  ) RGB_ItnBlocks[i][j][y][x][1] = 0;
                        if( RGB_ItnBlocks[i][j][y][x][2] < 0  ) RGB_ItnBlocks[i][j][y][x][2] = 0;
                        if( RGB_ItnBlocks[i][j][y][x][0] >  255  ) RGB_ItnBlocks[i][j][y][x][0] =  255;
                        if( RGB_ItnBlocks[i][j][y][x][1] >  255  ) RGB_ItnBlocks[i][j][y][x][1] =  255;
                        if( RGB_ItnBlocks[i][j][y][x][2] >  255  ) RGB_ItnBlocks[i][j][y][x][2] =  255;
                        
                        RGB_DeItnBlocks[i][j][y][x][0] = (byte)( RGB_ItnBlocks[i][j][y][x][0] );
                        RGB_DeItnBlocks[i][j][y][x][1] = (byte)( RGB_ItnBlocks[i][j][y][x][1] );
                        RGB_DeItnBlocks[i][j][y][x][2] = (byte)( RGB_ItnBlocks[i][j][y][x][2] );
                        
                        if( mode == 2 ){
                            byte RR = (RGB_DeItnBlocks[i][j][y][x][0] );
                            byte GG = (RGB_DeItnBlocks[i][j][y][x][1] );
                            byte BB = (RGB_DeItnBlocks[i][j][y][x][2] );
                        
                            Pixel_AfterCompression[i][j][y][x] = 0xff000000 
                                                | ((RR & 0xff) << 16) | ((GG & 0xff) << 8) | (BB & 0xff);
                        
                            img.setRGB(j*8+x, i*8+y, Pixel_AfterCompression[i][j][y][x]);
                        }
                    }
                }
                
            }
        }
        
    }
    
}