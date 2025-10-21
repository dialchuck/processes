import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;

public class Images
{
   /*-------------------------------------------------------------------------*/
   /*--- resizePNG ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static void resizePNG( String sourceFile, String targetFile, int newWidth )
   {
      try
      {
         BufferedImage  sourceImage    =  ImageIO.read( new File(sourceFile) );
         int            originalWidth  =  sourceImage.getWidth();
         int            originalHeight =  sourceImage.getHeight();
         int            newHeight      =  (newWidth * originalHeight) / originalWidth;
         Image          scaledImage    =  sourceImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
         BufferedImage  resizedImage   =  new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
         Graphics2D     g2d            =  resizedImage.createGraphics();
         g2d.drawImage( scaledImage, 0, 0, null );
         g2d.dispose();
         ImageIO.write( resizedImage, "png", new File(targetFile) );
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
   }
}

