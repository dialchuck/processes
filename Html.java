import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Html
{
   /*-------------------------------------------------------------------------*/
   /*--- text ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static String text( String inputCode )
   {
      Document doc = Jsoup.parse( inputCode );
      return doc.body().text();
   }
}
