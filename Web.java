import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;

public class Web
{
   /*-------------------------------------------------------------------------*/
   /*--- webGet ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static byte[] webGet( String url ) throws Exception
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      URL                      u = new URL( url );
      HttpURLConnection connection = (HttpURLConnection) u.openConnection();
      
      connection.setRequestProperty( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36" );
      connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8" );
      connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
      InputStream             is = connection.getInputStream();
      byte[]                   b = new byte[1024];
      int                     nr = 0;
      
      while( (nr = is.read(b)) != -1 )
      {
         baos.write( b, 0, nr );
      }
      is.close();
      connection.disconnect();
      return baos.toByteArray();
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- webPost ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String webPost( String link, HashMap headers, String payLoad )
   {
      try
      {
         URL               url   =  new URL(link);
         HttpURLConnection conn  =  (HttpURLConnection) url.openConnection();
         conn.setRequestMethod( "POST" );
         if( headers!=null )
         {
            Iterator it = headers.keySet().iterator();
            while( it.hasNext() )
            {
               String k = it.next().toString();
               String v = Format.getHashValue( headers, k );
               conn.setRequestProperty( k, v );
            }
         }
         
         conn.setDoOutput(true);
         OutputStream   os    =  conn.getOutputStream();
         byte[]         input =  payLoad.getBytes("utf-8");
         os.write(input, 0, input.length);


         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         InputStream           is   = conn.getInputStream();
         byte[]                b    = new byte[1024];
         int                   nr   = 0;
         while( (nr=is.read(b)) != -1 )
            baos.write( b, 0, nr );
         is.close();
         return (new String(baos.toByteArray()));
      }
      catch( Exception e )
      {
         return "\"" + link + "\"   ===   " + e;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- noTrustCert ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static void noTrustCert()
   {
      TrustManager[] trustAllCerts = new TrustManager[]
      {
         new X509TrustManager()
         {
            public java.security.cert.X509Certificate[] getAcceptedIssuers()
            {
               return null;
            }
            public void checkClientTrusted( java.security.cert.X509Certificate[] certs, String authType )
            {
            }
            public void checkServerTrusted( java.security.cert.X509Certificate[] certs, String authType )
            {
            }
         }
      };
      
      try
      {
         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init( null, trustAllCerts, new java.security.SecureRandom() );
         HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory());
      }
      catch( GeneralSecurityException e )
      {
      } 
   }

}
