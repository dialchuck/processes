import java.util.*;
import java.io.*;
import java.net.*;

public class OpenAI
{
   private static String ENDPOINT   =  "https://api.openai.com/v1/chat/completions";
   private static String MODEL      =  "gpt-4o";
   
   /*-------------------------------------------------------------------------*/
   /*--- call ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static HashMap call( String prompt, String apiKey )
   {
      HashMap  hmResp   =  new HashMap();
      long     now      =  System.currentTimeMillis();
      String output  =  "";
      try
      {
         String payLoad =  "{\"stream\":false, \"model\": \"" + MODEL + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + URLEncoder.encode(prompt, "UTF-8") + "\"}]}";
         URL               url   =  new URL( ENDPOINT );
         URLConnection     conn  =  url.openConnection();
         conn.setRequestProperty( "Authorization", "Bearer " + apiKey   );
         conn.setRequestProperty( "content-type",  "application/json"   );
         conn.setDoOutput( true );
         int                  nr       =  0;
         byte                 b[]      = new byte[512];
         String               line     =  "";
         OutputStreamWriter   wr       =  new OutputStreamWriter (conn.getOutputStream ());
         wr.write (payLoad);
         wr.flush ();
         
         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
         while( (line = reader.readLine()) != null )
            output += line;
         reader.close();
         wr.close();
      }
      catch( Exception e )
      {
         e.printStackTrace();
         output = "";
      }
      long        timing            =  System.currentTimeMillis() - now;
      HashMap     hmResponse        =  Format.hashJson( output );
      String      model             =  Format.getHashValue( hmResponse, "model" );
      ArrayList   choices           =  (ArrayList) hmResponse.get("choices");
      HashMap     usage             =  (HashMap)   hmResponse.get("usage"  );
      String      promptTokens      =  Format.getHashValue( usage, "prompt_tokens"     );
      String      completionTokens  =  Format.getHashValue( usage, "completion_tokens" );
      HashMap     choice            =  (HashMap)   choices.get(0           );
      HashMap     message           =  (HashMap)   choice.get( "message"   );
      
      hmResp.put( "model",             model             );
      hmResp.put( "prompt_tokens",     promptTokens      );
      hmResp.put( "completion_tokens", completionTokens  );
      hmResp.put( "timing",            "" + timing       );
      hmResp.put( "content",           Format.getHashValue( message, "content"   ).replace("\r", "") );
      
      return hmResp;
   }
}
