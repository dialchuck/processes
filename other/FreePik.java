import java.util.*;
import java.net.*;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;

public class FreePik
{
   static String FREEPIK_APIKEY    =  "FPSX90a1d3225b6c47498cc0287b8a7a2c40";
   static
   {
      Web.noTrustCert();
   }      
   
   public static void main( String args[] ) throws Exception
   {
      String      sql      =  "";
      ArrayList   alQ      =  DB.getData( "select * from multi_queue2 where type='image'" );
      HashMap     hmDone   =  new HashMap();
      for( int i=0; i<alQ.size(); i++ )
      {
         HashMap     hmQ         =  (HashMap) alQ.get(i);
         String      domainId    =  Format.getHashValue( hmQ, "domain_id"     );
         String      nodeId      =  Format.getHashValue( hmQ, "node_id"       );
         String      levelType   =  Format.getHashValue( hmQ, "level_type"    );
         String      queueId     =  Format.getHashValue( hmQ, "id"            );
         String      prompts[]   =  Format.getHashValue( hmQ, "prompts"       ).replaceAll("\\[|\\]|\\s", "").replace(" ", "").split(",");
         String      domainName  =  DB.getValue( "select name from domains where id=" + domainId );
         ArrayList   alInv       =  Multi.inventory( domainId );
         ArrayList   subInv      =  Multi.extractFromInventory( alInv, nodeId, levelType );

         for( int j=0; j<subInv.size(); j++ )
         {
            ArrayList alSub = (ArrayList) subInv.get(j);

            for( int k=0; k<prompts.length; k++ )
            {
               if( !prompts[k].equals("0") )
               {
                  String   level    =  "" + (k+1);
                  HashMap  hmPrompt =  DB.getRecord( "select shell_id, prompt from prompts2 where id=" + prompts[k], true );
                  String   prompt   =  Format.getHashValue( hmPrompt, "prompt"   ).trim();
                  String   shellId  =  Format.getHashValue( hmPrompt, "shell_id" );
                  HashMap  hmSub    =  null;
                  try {    hmSub    =  (HashMap) alSub.get(k); } catch( Exception e ) {}
                  if( hmSub!=null )
                  {
                     String   subNodeId   =  Format.getHashValue( hmSub, "id" );
                              sql         =  "select parent_id from shell_nodes where id=" + subNodeId + " union select parent_id from domain_nodes where id=" + subNodeId;
                     String   parentId    =  DB.getValue( sql );
                     if( hmDone.get(subNodeId)==null  )
                     {
                        hmDone.put( subNodeId, "" );
                        if( prompt.contains("{token") )
                        {
                                    sql         =  "select token_name, value from node_tokens where node_type='" + Format.getHashValue(hmSub, "type") + "' and node_id="   +  subNodeId;
                           HashMap  hmTokens    =  DB.getHashData( sql, "token_name", "value" );
                           Iterator it          =  hmTokens.keySet().iterator();
                           while( it.hasNext() )
                           {
                              String   ky  =  it.next().toString();
                              String   vl  =  Format.getHashValue( hmTokens, ky );
                              prompt       =  prompt.replace( "{token." + Format.initCap(ky) + "}", vl );
                           }
                        }
                        prompt = prompt.replace( "\"", "" );
                        if( !prompt.contains("{") )
                        {
                           ArrayList alSearch =  search( prompt );
                           if( alSearch!=null && alSearch.size() > 0 )
                           {
                              String prefix  =  Format.getHashValue( hmSub, "type" );
                                     prefix  =  prefix.equals("domain")?"domain_":"shell_";
                                     sql     =  "select uri from " + prefix + "nodes n, " + prefix + "entities e where e.id=n.entity_id and n.id=" + subNodeId;
                              String uri     =  DB.getValue( sql );
                              if( !uri.equals("") )
                              {
                                 String link =  "https://dialabpix.s3.amazonaws.com/" + domainName + "/" + uri + ".png";
                                 if( !uri.equals("") )
                                 {
                                    if( !exists(link) )
                                    {
                                       for( int n=0; n<alSearch.size(); n++ )
                                       {
                                          HashMap           hmSearch =  (HashMap) alSearch.get(i);
                                          String            imgId    =  Format.getHashValue( hmSearch, "id"  );
                                          if( !imgUsed( imgId, parentId ) )
                                          {
                                             String            imgLink  =  Format.getHashValue( hmSearch, "url" );
                                                               imgLink  =  imgLink.replace( "https://img.b2bpic.net/", "https://img.freepik.com/" );
                                             byte              b[]      =  webGet( imgLink );
                                             FileOutputStream  fop      =  new FileOutputStream( uri + ".png" );
                                             fop.write( b );
                                             fop.close();
                                             AWSS3.upload( "dialabpix", domainName, ".", uri + ".png" );
                                             DB.executeSQL( "delete from node_images where node_type='" + levelType + "' and node_id=" + subNodeId );
                                             DB.executeSQL( "insert into node_images (node_id, node_type, url, domain_id) values ("    + subNodeId + ", '" + levelType + "', '" + link + "', " + domainId + ")" );
                                             (new File(uri + ".png")).delete();
                                             DB.executeSQL( "insert ignore into images (img_id, parent_id) values ('" + imgId + "', " + parentId + ")" );
                                             break;
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
         DB.executeSQL( "delete from multi_queue2 where id=" + queueId );
      }
   }


   /*-------------------------------------------------------------------------*/
   /*--- search ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static ArrayList search( String keyword )
   {
      String      data  =  pull( keyword );
      HashMap     hm    =  Format.hashJson( data );
      ArrayList   al    =  new ArrayList();
      ArrayList   alTmp =  (ArrayList) hm.get("data");
      if( alTmp==null )
         return null;
      if( alTmp.size()==0 )
      {
         try { Thread.sleep( 1000*30 ); } catch( Exception e ) {}
         alTmp =  (ArrayList) hm.get("data");
      }
      for( int i=0; i<alTmp.size(); i++ )
      {
         HashMap hmTmp  =  (HashMap) alTmp.get(i);
         String  idTmp  =  (String)  hmTmp.get( "id"     );
                 hmTmp  =  (HashMap) hmTmp.get( "image"  );
                 hmTmp  =  (HashMap) hmTmp.get( "source" );
         String dims[]  =  Format.getHashValue( hmTmp, "size" ).split( "x" );
//       if( dims.length==2 )
         {
            int      w  =  Integer.parseInt( dims[0] );
            int      h  =  Integer.parseInt( dims[1] );
            double   r  =  ((double)w) / ((double)h);
            if( r<1.7 && r>1.3 )
            {
               HashMap hItem = new HashMap();
               hItem.put( "id",   idTmp                               );
               hItem.put( "url",  Format.getHashValue( hmTmp, "url" ) );
               al.add( hItem );
            }
         }
      }
      return al;
   }
   
   
   /*-------------------------------------------------------------------------*/
   /*--- pull ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static String pull( String keyword )
   {
      try
      {
         String urlString = "https://api.freepik.com/v1/resources?page=10&limit=20&order=relevance&term=" + URLEncoder.encode(keyword, "UTF-8") + "%20-cartoon%20-3d%20-render%20-illustration%20-funny&filters%5Borientation%5D%5Blandscape%5D=1&filters%5Bcontent_type%5D%5Bphoto%5D=1";
         return new String(get(urlString));
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return null;
      }
   }
   
   
   /*-------------------------------------------------------------------------*/
   /*--- get ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   private static byte[] get( String urlString )
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try
      {
         URL               url         =  new URL( urlString );
         HttpURLConnection connection  =  (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("GET");
         connection.setRequestProperty( "x-freepik-api-key", FREEPIK_APIKEY );
         
         int responseCode = connection.getResponseCode();
         if( responseCode == HttpURLConnection.HTTP_OK )
         {
            byte           b[]   =  new byte[1024];
            int            nr    =  0;
            InputStream    is    =  connection.getInputStream();
            while( (nr=is.read(b))!= -1 )
               baos.write( b, 0, nr );
         }
         connection.disconnect();
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
      return baos.toByteArray();
   }
   
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
   /*--- imgUsed ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   private static boolean imgUsed( String imgId, String parentId )
   {
      return !DB.getValue( "select count(*) from images where parent_id=" + parentId + " and img_id='" + imgId + "'" ).equals("0");
   }


   /*-------------------------------------------------------------------------*/
   /*--- exists ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean exists( String urlString )
   {
      HttpURLConnection connection = null;
      try
      {
         URL url = new URL(urlString);
         connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("HEAD");
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(5000);
         connection.connect();
      
         int responseCode = connection.getResponseCode();
         return (responseCode==200);
      }
      catch( IOException e )
      {
         return false;
      }
      finally
      {
         if (connection != null)
            connection.disconnect();
      }
   }
}
