import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class EBook
{
   /*-------------------------------------------------------------------------*/
   /*--- main ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static void main( String args[] ) throws Exception
   {
      String      endP     =  "https://nprm4vz4v4ht2czqvzom37vk5i0dygsr.lambda-url.us-east-1.on.aws/";
      String      apiKey   =  "";
      ArrayList   al       =  null;
      

      if( args.length==0 )
      {
         apiKey = DB.getValue( "select apikey   from open_ai_api"  );
         al     = DB.getData(  "select *        from ebook_queue2" );
      }
      else
      {
         apiKey = DB.getValue( "select apikey   from open_ai_api  where id%" + args[1] + "=" + args[0] );
         al     = DB.getData(  "select *        from ebook_queue2 where id%" + args[1] + "=" + args[0] );
      }
      

      String      sql      =  "select id, label_path        from domain_nodes where id in (select node_id from ebook_queue2 where node_type='domain') union ";
                  sql     +=  "select id, label_path        from shell_nodes  where id in (select node_id from ebook_queue2 where node_type='shell')";
      HashMap     hmTokens =  DB.getHashData( sql, "id", "label_path" );
      HashMap     headers  =  new HashMap();
      
      headers.put( "Content-Type", "application/json" );
      
      for( int i=0; i<al.size(); i++ )
      {
         HashMap     hm             =  (HashMap) al.get(i);
         String      id             =  Format.getHashValue( hm, "id"             );
         String      node_type      =  Format.getHashValue( hm, "node_type"      );
         String      node_id        =  Format.getHashValue( hm, "node_id"        );
         String      title          =  Format.getHashValue( hm, "title"          );
         String      description    =  Format.getHashValue( hm, "description"    );
         String      chapters       =  Format.getHashValue( hm, "chapters"       );
         String      include_images =  Format.getHashValue( hm, "image"          );
         String      style          =  Format.getHashValue( hm, "style"          );
         String      length         =  Format.getHashValue( hm, "length"         );
         String      audience       =  Format.getHashValue( hm, "audience"       );
         String      tone           =  Format.getHashValue( hm, "tone"           );
         String      stamp          =  Format.getHashValue( hm, "stamp"          );
         String      image          =  Format.getHashValue( hm, "image"          );
         String      domainId       =  DB.getValue( "select domain_id from domain_nodes where id=" + node_id, true );
         String      shellId        =  getShellId( domainId, node_id, node_type );
         ArrayList   alTokens       =  DB.getValues( "select label       from shell_levels             where shell_id=" + shellId + " order by level" );
         String      tokens[]       =  DB.getValue(  "select label_path  from " + node_type + "_nodes  where id=" + node_id ).split("\t");
         String      payLoad        =  "{";
                     payLoad       +=        "\"title\": \""         +  title                      +  "\", ";
                     payLoad       +=        "\"description\": \""   +  description                +  "\",";
                     payLoad       +=        "\"chapters\": "        +  chapters                   +  ",";
                     payLoad       +=        "\"include_images\": "  +  include_images.equals("1") +  ",";
                     payLoad       +=        "\"style\": \""         +  style                      +  "\",";
                     payLoad       +=        "\"length\": \""        +  length                     +  "\",";
                     payLoad       +=        "\"audience\": \""      +  audience                   +  "\",";
                     payLoad       +=        "\"tone\": \""          +  tone                       +  "\",";
                     payLoad       +=        "\"shared_secret\":\"clonelieutenant\",";
                     payLoad       +=        "\"openai\":\"" + apiKey + "\"";
                     payLoad       +=  "}";
         for( int j=0; j<tokens.length; j++ )
            payLoad = payLoad.replace( "{" + alTokens.get(j) + "}", tokens[j] );
         String      response    =  Web.webPost( endP, headers, payLoad );
         HashMap     hmResp      =  Format.hashJson( response );
         ArrayList   alChapters  =  (ArrayList) hmResp.get( "chapters" );
         DB.executeSQL( "delete from ebook_content where node_id=" + node_id );
         try
         {
            for( int j=0; j<alChapters.size(); j++ )
            {
               HashMap           chapter     =  (HashMap) alChapters.get(j);
               String            rTitle      =  Format.getHashValue( chapter, "title"     ).replace("\"", "\\\"");
               String            rImageUrl   =  Format.getHashValue( chapter, "imageUrl"  );
               String            rContent    =  Format.getHashValue( chapter, "content"   );
               String            hContent    =  Format.convertMD( rContent ).replace("\"", "\\\"");
                                 sql         =  "insert into ebook_content (node_id,          node_type,       prompt,     title,              chapter, content, domain_id) values (";
                                 sql        +=                              node_id + ", '" + node_type + "',\"" + payLoad.replace("\"", "\\\"") + "\",\"" + rTitle + "\", " + (j+1) + ",\"" + hContent + "\"," + domainId + ")";
               String            ebookID     =  DB.executeSQL( sql );
               String            fileName    =  node_id + "-" + (j+1) + ".png";
               String            resizeName  =  fileName.replace(".png", "_s.png");
               FileOutputStream  fop         =  new FileOutputStream( fileName );
               AWSS3.rm( "pit21", "ebook/" + shellId + "_" + domainId + "/" + fileName   );
               AWSS3.rm( "pit21", "ebook/" + shellId + "_" + domainId + "/" + resizeName );
               
               if( !(""+rImageUrl).equals("null") )
               {
                  fop.write( Web.webGet(rImageUrl) );
                  fop.close();
                  Images.resizePNG( fileName, resizeName, 300 );
                  AWSS3.upload( "pit21", "ebook/" + shellId + "_" + domainId, ".", fileName     );
                  AWSS3.upload( "pit21", "ebook/" + shellId + "_" + domainId, ".", resizeName   );
               }
            }
            DB.executeSQL( "delete from ebook_queue2 where id=" + id );
         }
         catch( Exception e )
         {
            e.printStackTrace();
         }
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getShellId ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   private static String getShellId( String domainId, String nodeId, String nodeType )
   {
      if( nodeType.equals("shell") )
         return DB.getValue( "select shell_id from shell_nodes where id=" + nodeId, true );
      return    DB.getValue( "select s.shell_id from sub_verticals s, domains d where d.sub_vertical_id=s.id and d.id=" + domainId );
   }
}