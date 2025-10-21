import java.io.*;
import org.json.*;
import java.text.*;
import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.security.*;
import java.math.*;
import java.net.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.awt.image.*;
import java.awt.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlRenderer;

public class Format
{
   public static String[] days   = { "Saturday","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday"};
   public static String[] months = { "Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec." };
   
   /*-------------------------------------------------------------------------*/
   /*--- max ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static int max( ArrayList al )
   {
      try
      {
         int max = ((Integer)al.get(0)).intValue();
         for( int i=0; i<al.size(); i++ )
            max = Math.max( max, ((Integer)al.get(i)).intValue() );
         return max;
      }
      catch( Exception e )
      {
         return 0;
      }
   }
   public static int max( int n[] )
   {
      try
      {
         int max = n[0];
         for( int i=0; i<n.length; i++ )
            max = Math.max( max, n[i] );
         return max;
      }
      catch( Exception e )
      {
         return 0;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- maxDate ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String maxDate( String d1, String d2 )
   {
      if( d1.compareTo(d2) > 0 )
         return d1;
      else
         return d2;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- minDate ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String minDate( String d1, String d2 )
   {
      if( d1.compareTo(d2) < 0 )
         return d1;
      else
         return d2;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- dateISO ...                                                       ---*/
   /*--- convert mm/dd/yyyy to yyyy-MM-dd                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static String dateISO( String dt )
   {
      try
      {
         String tmp[] = dt.split( "/" );
         StringBuffer sb = new StringBuffer();
         sb.append(tmp[2]).append("-");
         if( tmp[0].length()==1 ) sb.append( "0" );
         sb.append(tmp[0]).append("-");
         if( tmp[1].length()==1 ) sb.append( "0" );
         sb.append(tmp[1]);
         return sb.toString();
      // return (new StringBuffer("")).append(tmp[2]).append("-").append(tmp[0]).append("-").append(tmp[1]).toString();
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- monthName ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String monthName( String mmyyyy )
   {
      try
      {
         return months[Integer.parseInt( mmyyyy.split("/")[0] ) - 1];
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- weekRange ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String[] weekRange( int nWeeks, String _cycleDay )
   {
      return weekRange( today(), nWeeks, _cycleDay );
   }
   public static String[] weekRange( String _start, int nWeeks, String _cycleDay )
   {
      LinkedHashMap lHm = new LinkedHashMap();
      try
      {
         String start = _start.contains("-")?_start:dateISO(_start);
         int    n     = 0;
         while( n<100 && !dayOfWeek(start).equals(_cycleDay) )
         {
            start = addDays( start, 1 );
            n++;
         }
         lHm.put( start, "" );
         for( int i=0; i<Math.min(nWeeks,100); i++ )
         {
            start = addDays( start, -7 );
            lHm.put( start, "" );
         }
         Set    keys = lHm.keySet();
         return (String []) keys.toArray(new String[keys.size()]);
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return null;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- mmyyyyRange ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String[] mmyyyyRange( String start, String end )
   {
      try
      {
         LinkedHashMap  lHm = new LinkedHashMap();
         String      _start = start;
         String        _end = end;
         int              n = 0;
         while( n<200 )
         {
            n++;
            String s = mmyy( _start );
            String f = mmyy( _end   );
            _start   = addDays( _start, 1 );
            lHm.put( mmyy(_start), "" );
            if( _start.equals(_end) )
               break;
         }
         Set    keys = lHm.keySet();
         return (String []) keys.toArray(new String[keys.size()]);
      }
      catch( Exception e )
      {
         return (new String[1]);
      }
   }
   public static ArrayList mmyyyyRange( int n )
   {
      ArrayList   al       =  new ArrayList();
      String      start[]  =  mmyy().split("/");
      int         mm       =  Integer.parseInt(start[0]);
      int         yy       =  Integer.parseInt(start[1]);
      for( int i=0; i<n; i++ )
      {
         String  val = pad( "" + mm, "0", 2 ) + "/" + pad( "" + yy, "0", 4 );
         al.add( val );
         mm = mm - 1;
         if( mm==0 )
         {
            mm = 12;
            yy = yy - 1;
         }
      }
      return al;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- reverseDateISO ...                                                ---*/
   /*--- convert yyyy-MM-dd to mm/dd/yyyy                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static String reverseDateISO( String dt )
   {
      try
      {
         String tmp[] = dt.split( "-" );
         return (new StringBuffer("")).append(tmp[1]).append("/").append(tmp[2]).append("/").append(tmp[0]).toString();
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- dayOfWeek ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String dayOfWeek( String dt )
   {
      try
      {
         SimpleDateFormat  sdf         =  new SimpleDateFormat( dt.contains("-")?"yyyy-MM-dd":"MM/dd/yyyy" );
         Date              date        = (Date) sdf.parse(      dt );
         Calendar          c1          = Calendar.getInstance();
         c1.setTime( date );
         return days[c1.get(Calendar.DAY_OF_WEEK)];
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- lastChar ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String lastChar( String c ) throws Exception
   {
      return c.substring(c.length() - 1 );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- removeLastChar ...                                                ---*/
   /*-------------------------------------------------------------------------*/
   public static String removeLastChar( String c ) throws Exception
   {
      return c.substring(0, c.length() - 1 );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- unMerge ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String unMerge( String s ) throws Exception
   {
      String r = "";
      for( int i=0; i<s.length(); i+=2 )
         r += s.charAt(i);
      return r;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- merge ...                                                         ---*/
   /*-------------------------------------------------------------------------*/
   public static String merge( String s ) throws Exception
   {
      String r = "";
      for( int i=0; i<s.length(); i++ )
         r += "" + s.charAt(i) + randomChar();
      return r;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- randomChar ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static char randomChar()
   {
      return (char) ((new Random()).nextInt(26) + 'a' );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- addDays ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String addDays( String date_string, int days ) throws Exception
   {
      String            DATE_FORMAT =  "yyyy-MM-dd";
      SimpleDateFormat  sdf         =  new SimpleDateFormat( DATE_FORMAT );
      Date              date        = (Date) sdf.parse(      date_string );
      Calendar          c1          = Calendar.getInstance();
      c1.setTime( date );
      c1.add( Calendar.DATE,days );
      return sdf.format( c1.getTime() );
   }
   
   public static String addMonths( String date_string, int months ) throws Exception
   {
      String            DATE_FORMAT =  "yyyy-MM-dd";
      SimpleDateFormat  sdf         =  new SimpleDateFormat( DATE_FORMAT );
      Date              date        = (Date) sdf.parse(      date_string );
      Calendar          c1          = Calendar.getInstance();
      c1.setTime( date );
      c1.add( Calendar.MONTH,months );
      return sdf.format( c1.getTime() );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- yyyy ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static String yyyy()
   {
      try
      {
         return today( "yyyy" );
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- mmyy ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static String mmyy( String dt )
   {
      try
      {
         String tmp[] = dt.split( "-" );
         StringBuffer sb = new StringBuffer();
         sb.append(tmp[1]).append("/");
         sb.append(tmp[0]);
         return sb.toString();
      }
      catch( Exception e )
      {
         return "";
      }
   }
   public static String mmyy()
   {
      try
      {
         return today( "MM/yyyy" );
      }
      catch( Exception e )
      {
         return "";
      }
   }
   public static String mmyy( String units, int n )
   {
      Calendar          cal   =  Calendar.getInstance();
      cal.setTime( new Date() );
      if(      units.equals("DAY")     )  cal.add( Calendar.DATE,    n );
      else if( units.equals("HOUR")    )  cal.add( Calendar.HOUR,    n );
      else if( units.equals("MINUTE")  )  cal.add( Calendar.MINUTE,  n );
      else if( units.equals("SECOND")  )  cal.add( Calendar.SECOND,  n );
      Date              dt    =  cal.getTime();
      SimpleDateFormat  fmt   =  new SimpleDateFormat( "MMyy" );
      return fmt.format( dt );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- currentHour ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static int currentHour()
   {
      Calendar          cal   =  Calendar.getInstance();
      cal.setTime( new Date() );
      SimpleDateFormat  fmt   =  new SimpleDateFormat( "H" );
      Date              dt    =  cal.getTime();
      return Integer.parseInt( fmt.format( dt ) );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- today ...                                                         ---*/
   /*-------------------------------------------------------------------------*/
   public static String today( String theFormat )
   {
      SimpleDateFormat fmt = new SimpleDateFormat(theFormat);
      return fmt.format( new Date() );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- age ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static int age( String mm, String dd, String yyyy )
   {
      try
      {
         String _mm   = mm.trim();
                _mm   = (_mm.length()==2?"":"0") + _mm;
         String _dd   = dd.trim();
                _dd   = (_dd.length()==2?"":"0") + _dd;
         String _yyyy = yyyy.trim();
         return age( yyyy + "-" + _mm + "-"   + _dd );
      }
      catch( Exception e )
      {
         return -1;
      }
   }
   
   public static int age( String dob )
   {
      String _dob = dob;
      if( _dob.contains("/") )
      {
         String splt[]  =  _dob.split( "/" );
                _dob    =  splt[2] + "-" + (splt[0].length()==1?"0":"") + splt[0] + "-" + (splt[1].length()==1?"0":"") + splt[1];
      }
      else
      {
         String splt[]  =  _dob.split( "-" );
                _dob    =  splt[0] + "-" + (splt[1].length()==1?"0":"") + splt[1] + "-" + (splt[2].length()==1?"0":"") + splt[2];
      }
      try
      {
         LocalDate   birthDate   =  LocalDate.parse( _dob         );
         LocalDate   today       =  LocalDate.parse( todayISO()   );
         return Period.between( birthDate, today ).getYears();
      }
      catch( Exception e )
      {
         System.out.println( "Error Claculating age for " + dob );
         return -1;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- daysBetween ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static long daysBetween( String start, String end )
   {
      LocalDate dateBefore = LocalDate.parse( start );
	   LocalDate dateAfter  = LocalDate.parse( end  );
	   return ChronoUnit.DAYS.between( dateBefore, dateAfter );
   }    
   
   /*-------------------------------------------------------------------------*/
   /*--- todayISO ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String today()
   {
      return today( "MM/dd/yyyy" );
   }
   public static String todayISO()
   {
      return today( "yyyy-MM-dd" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- now ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static String now()
   {
      return today( "hh:mm:ss" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- deCypherStamp ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String deCypherStamp( String input )
   {
      String clean = "";
      for( int i=1; i<input.length(); i+=2 )
         clean += input.charAt(i);
       clean = clean.replace( "A", "0" );
       clean = clean.replace( "Z", "1" );
       clean = clean.replace( "E", "2" );
       clean = clean.replace( "R", "3" );
       clean = clean.replace( "T", "4" );
       clean = clean.replace( "Y", "5" );
       clean = clean.replace( "C", "6" );
       clean = clean.replace( "H", "7" );
       clean = clean.replace( "U", "8" );
       clean = clean.replace( "K", "9" );
       clean = clean.replace( "a", "0" );
       clean = clean.replace( "z", "1" );
       clean = clean.replace( "e", "2" );
       clean = clean.replace( "r", "3" );
       clean = clean.replace( "t", "4" );
       clean = clean.replace( "y", "5" );
       clean = clean.replace( "c", "6" );
       clean = clean.replace( "h", "7" );
       clean = clean.replace( "u", "8" );
       clean = clean.replace( "k", "9" );
      return clean;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- cypherStamp ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String cypherStamp()
   {
      String cyphr   =  "";
      String stamp   =  today( "yyyyMMddHH" );
             stamp   =  stamp.replace( "0", "A" );
             stamp   =  stamp.replace( "1", "Z" );
             stamp   =  stamp.replace( "2", "E" );
             stamp   =  stamp.replace( "3", "R" );
             stamp   =  stamp.replace( "4", "T" );
             stamp   =  stamp.replace( "5", "Y" );
             stamp   =  stamp.replace( "6", "C" );
             stamp   =  stamp.replace( "7", "H" );
             stamp   =  stamp.replace( "8", "U" );
             stamp   =  stamp.replace( "9", "K" );
      for( int i=0; i<stamp.length(); i++ )
         cyphr += (""+randomChar()).toUpperCase() + stamp.charAt(i);
      return cyphr;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- dbSafe ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static String dbSafe( String v )
   {
      return v.replace( "\\", "" ).replace( "\"", "\\\"" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- clean ...                                                         ---*/
   /*-------------------------------------------------------------------------*/
   public static String clean( String v )
   {
      if( v==null )
         return null;
      return v.replace( "\"", "" ).replace( "\\", "" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- setToken ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String setToken( String txt, String token, String newVal, int pos )
   {
      String split[] =  txt.split( token );
      String newTxt  =  "";
      for( int i=0; i<split.length; i++ )
      {
         newTxt += (i==pos)?newVal:split[i];
         newTxt += (i==split.length-1)?"":token;
      }
      return newTxt;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- lastToken ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String lastToken( String txt, String token )
   {
      String nToken = token;
      if( nToken.equals("\\") )
         nToken = "\\\\";
      try
      {
         String tmp[] = txt.split( nToken );
         return tmp[tmp.length-1];
      }
      catch( Exception e )
      {
         return txt;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- removeToken ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static String removeToken( String input, String token, int index, boolean removeToken )
   {
      String output  =  "";
      String split[] =  input.split( token );
      for( int i=0; i<split.length; i++ )
      {
         if( !(i==index && removeToken) ) output += (i==0?"":token);
         if( i!=index )                   output += (i==index?"":split[i]);
      }
      return output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- extractLastToken ...                                              ---*/
   /*-------------------------------------------------------------------------*/
   public static String extractLastToken( String txt, String token )
   {
      try
      {
         return (new StringBuffer( txt.substring( 0, txt.lastIndexOf(token) ) )).toString();
      }
      catch( Exception e )
      {
         return txt;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- uuid ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static String uuid()
   {
      String uuid = "+";
      try
      {
         while( uuid.contains("+") )
            uuid = UUID.randomUUID().toString();
         return uuid;
      }
      catch( Exception e )
      {
         return "" + System.currentTimeMillis();
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- bigUUID ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String bigUUID()
   {
      String uuid    = "+";
      String newUUID = "";
      try
      {
         while( uuid.contains("+") )
            uuid = UUID.randomUUID().toString();
         for( int i=0; i<uuid.length(); i++ )
            newUUID += "" + uuid.charAt(i) + randomChar() + randomChar();
         return newUUID;
      }
      catch( Exception e )
      {
         return "" + System.currentTimeMillis();
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- genPassword ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String genPassword()
   {
      return genPassword(12);
   }
   public static String genPassword( int len )
   {
      String charsCaps     =  "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      String chars         =  "abcdefghijklmnopqrstuvwxyz";
      String nums          =  "0123456789";
      String symbols       =  "!@#$%^&*_=+-/.?<>)";
      String passSymbols   =  charsCaps + chars + nums + symbols;
      Random rnd           =  new Random();
      String password      =  "";
      int    index         =  0;
      for( int i=0; i<len; i++ )
         password += passSymbols.charAt( rnd.nextInt(passSymbols.length()) );
      return password;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- validatePassword ...                                              ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean validatePassword( String passwd )
   {
      if( passwd.trim().length() < 8   )                       return false;
      if( passwd.contains("\"")        )                       return false;
      if( !Pattern.compile("[0-9]").matcher(passwd).find() )   return false;
      if( !Pattern.compile("[A-Z]").matcher(passwd).find() )   return false;
      return true;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- validateEmail ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean validateEmail( String email )
   {
      return email.matches("^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$");
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- repairEmail ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String repairEmail( String email )
   {
      if( email==null )
         return null;
      return email.toLowerCase()
            .replace("gamil.com","gmail.com")
            .replace("gmial.com","gmail.com");
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- formatUSPhone ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String formatUSPhone( String input )
   {
      String   tmp   =  input.replaceAll("[^\\d]", "");
      String   out   =  "(" + tmp.substring( 0, 3 ) + ") ";
               out  +=        tmp.substring( 3, 6 ) + "-";
               out  +=        tmp.substring(    6 );
      return   out;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- repairPhone ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String repairPhone( String phone )
   {
      if( phone==null )
         return null;
      String nPhone = phone.replaceAll("[^\\d]", "");
      if( nPhone.startsWith("1") )
         nPhone = nPhone.substring(1);
      return nPhone;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getJson ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String getJson( String jSon, String key )
   {
      try
      {
         return (new JSONObject(jSon)).get(key).toString();
      }
      catch( Exception e )
      {
         return null;
      }
   }
   
   public static Object getJson( JSONObject jSon, String key )
   {
      try
      {
         return jSon.get(key);
      }
      catch( Exception e )
      {
         return null;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getJsonObject ...                                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static JSONObject getJsonObject( JSONObject jSon, String key )
   {
      try
      {
         return (JSONObject) getJson(jSon, key);
      }
      catch( Exception e )
      {
         return null;
      }
   }
   public static JSONObject getJsonObject( JSONArray arr, int ndx )
   {
      try
      {
         return (JSONObject) arr.get(ndx);
      }
      catch( Exception e )
      {
         return null;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getJsonString ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String getJsonString( JSONObject jSon, String key )
   {
      try
      {
         return (String) jSon.get(key);
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getJsonArray ...                                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static JSONArray getJsonArray( JSONObject jSon, String key )
   {
      try
      {
         return (JSONArray) getJson(jSon, key);
      }
      catch( Exception e )
      {
         return new JSONArray();
      }
   }
   
   
   /*-------------------------------------------------------------------------*/
   /*--- json2Array ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static ArrayList json2Array( String str, String col )
   {
      ArrayList data = new ArrayList();
      try
      {
         JSONArray jArr = new JSONArray(str);
         for( int i=0; i<jArr.length(); i++ )
         {
            JSONObject jObj =  (JSONObject) jArr.get(i);
            data.add( jObj.get(col) );
         }
      }
      catch( Exception e )
      {
      }
      return data;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- json2Array ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static ArrayList json2Array( JSONArray jArr )
   {
      ArrayList data = new ArrayList();
      try
      {
         for( int i=0; i<jArr.length(); i++ )
         {
            Object o = jArr.get(i);
            if( o instanceof JSONObject )
               data.add( hashJson( (JSONObject) o ) );
            else
            if( o instanceof JSONArray )
               data.add( json2Array( (JSONArray) o ) );
            else
            if( o instanceof String )
               data.add( o );
         }
      }
      catch( Exception e )
      {
      }
      return data;
   }
   public static ArrayList json2Array( String str )
   {
      try
      {
         JSONArray jArr = new JSONArray(str);
         return json2Array( jArr );
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return new ArrayList();
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- setJson ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static void setJson( JSONObject jSon, String key, Object val )
   {
      try
      {
         jSon.put(key, val);
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- mergeJson ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static JSONObject mergeJson( JSONObject source, JSONObject target )
   {
      String names[] = JSONObject.getNames(source);
      for( int i=0; i<names.length; i++ )
      {
         String   key   = names[i];
         Object   value = null;
         try {    value = source.get(key); } catch( Exception e ) {}
         if (!target.has(key))
         {
            try { target.put(key, value); } catch( Exception e ) {}
         }
         else
         {
            if (value instanceof JSONObject)
            {
               JSONObject valueJson = (JSONObject)value;
               try { mergeJson( valueJson, target.getJSONObject(key) ); } catch( Exception e ) {}
            }
            else
            {
               try { target.put(key, value); } catch( Exception e ) {}
            }
         }
      }
      return target;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- inRange ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean inRange( String value, String range )
   {
      String split[] = range.split( "[,~]" );
      if( split.length!=2 )
         return false;
      
      //--- try integer comparison ---
      int _val   = -1;
      int _start = -1;
      int _end   = -1;
      
      try
      {
         _val   = Integer.parseInt( value    );
         _start = Integer.parseInt( split[0].trim() );
         _end   = Integer.parseInt( split[1].trim() );
         return (_val>=_start && _val<=_end);
      }
      catch( Exception e )
      {
      }
      
      return (value.toUpperCase().compareTo(split[0].trim().toUpperCase())>0 && value.toUpperCase().compareTo(split[1].trim().toUpperCase())<0);
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- hashJson ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static HashMap hashJson( String source )
   {
      try
      {
         return hashJson( new JSONObject(source) );
      }
      catch( Exception e )
      {
         return (new HashMap());
      }
   }
   public static HashMap hashJson( JSONObject source )
   {
      HashMap  hm = new HashMap();
      Iterator it = source.keys();
      while( it.hasNext() )
      {
         try
         {
            String k = it.next().toString();
            Object o = source.get(k);
            if( o instanceof JSONObject )
               hm.put( k, hashJson( (JSONObject) o ) );
            else
            if( o instanceof JSONArray  )
               hm.put( k, json2Array( (JSONArray) o ) );
            else
               hm.put( k, o.toString() );
         }
         catch( Exception e )
         {
            e.printStackTrace();
         }
      }
      return hm;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- printStackTrace ...                                               ---*/
   /*-------------------------------------------------------------------------*/
   public static String printStackTrace( Exception e )
   {
      StringWriter sw = new StringWriter();
      e.printStackTrace( new PrintWriter(sw) );
      return sw.toString();
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- dayName ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String dayName()
   {
      return dayName( todayISO() );
   }
   public static String dayName( String inputDate )
   {
      try
      {
         Date date = new SimpleDateFormat("yyyy-MM-dd").parse(inputDate);
         return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getHashInt ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static int getHashInt( HashMap hm, String keyName )
   {
      String v = getHashValue( hm, keyName, "0" ).split( "\\." )[0];
      return Integer.parseInt( v );
   }
   /*-------------------------------------------------------------------------*/
   /*--- getHashDouble ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static double getHashDouble( HashMap hm, String keyName )
   {
      return Double.parseDouble( getHashValue( hm, keyName, "0" ) );
   }
   /*-------------------------------------------------------------------------*/
   /*--- getHashValue ...                                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static String getHashValue( HashMap hm, String keyName )
   {
      return getHashValue( hm, keyName, "" );
   }
   public static String getHashValue( HashMap hm, String keyName, String dflt )
   {
      try
      {
         return hm.get(keyName).toString();
      }
      catch( Exception e )
      {
         return dflt;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- pad ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static String pad( String input, String pad, int length )
   {
      String str = input.trim();
      while( str.length() < length )
         str = pad + str;
      return str;
   }
   public static String pad( int input, String pad, int length )
   {
      return pad( ""+input, pad, length );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- like ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean like( String str, String expr )
   {
      String newExpr = expr;
      newExpr  = newExpr.toLowerCase();
      newExpr  = newExpr.replace( ".", "\\." );
      newExpr  = newExpr.replace( "?", "."   );
      newExpr  = newExpr.replace( "%", ".*"  );
      str = str.toLowerCase();
      return str.matches( newExpr );
   }

   /*-------------------------------------------------------------------------*/
   /*--- indexAt ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String indexAt( String str, String ndx )
   {
      try
      {
         String splt[] = str.split(",");
         return splt[ Integer.parseInt(ndx) ];
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- initCap ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String initCap( String word )
   {
      return initCap( word, false );
   }
   public static String initCap( String word, boolean hyphenSpace )
   {
      try
      {
         String     tmp[]   = word.split( "[\\ \\_-]" );
         StringBuffer txt   = new StringBuffer();
         for( int i=0; i<tmp.length; i++ )
         {
            StringBuffer sb = new StringBuffer( tmp[i] );
            sb.setCharAt( 0, Character.toUpperCase(sb.charAt(0)) );
            txt.append( sb );
            if( i<tmp.length-1 )
               txt.append( hyphenSpace?"-":" " );
         }
         return txt.toString();
      }
      catch( Exception e )
      {
         return word;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- initCap ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   /*
   public static String initCap( String word )
   {
      try
      {
         String     tmp[]   = word.split( "[\\ \\_-]" );
         StringBuffer txt   = new StringBuffer();
         for( int i=0; i<tmp.length; i++ )
         {
            tmp[i] = tmp[i].length()==0?tmp[i].toUpperCase():tmp[i].toLowerCase();
            StringBuffer sb = new StringBuffer( tmp[i] );
            sb.setCharAt( 0, Character.toUpperCase(sb.charAt(0)) );
            txt.append( sb );
            if( i<tmp.length-1 )
               txt.append( " " );
         }
         return txt.toString();
      }
      catch( Exception e )
      {
         return word;
      }
   }
   */
   
   
   /*-------------------------------------------------------------------------*/
   /*--- hyphenize ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String hyphenize( String word )
   {
      return hyphenize( word, false ).replace("'", "");
   }
   public static String hyphenize( String word, boolean encode )
   {
      String r = word.replaceAll("[^a-zA-Z0-9 -]", "");
             r = r.trim().toLowerCase().replace("-"," ").replace("&","and").replaceAll("\\s+", "-").replace("/","-").replace("?","");
      try
      {
         if( encode )
            r = URLEncoder.encode( r, "UTF-8" );
      }
      catch( Exception e )
      {
      }
//    return word.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", "-").replace("-"," ").replaceAll("\\s+", "-");
      return r;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- sec2Time ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String sec2Time( String secs )
   {
      try
      {
         return sec2Time( Double.parseDouble(secs) );
      }
      catch( Exception e )
      {
         return "00:00:00";
      }
   }
   public static String sec2Time( double secs )
   {
      String       splt[]  =  ("" + secs + ".00").split("\\.");
      int              ss  =  Integer.parseInt( splt[0] );
      int             dec  =  Integer.parseInt( splt[1] );
      Date              d  =  new Date(ss * 1000L);
      SimpleDateFormat df  =  new SimpleDateFormat("HH:mm:ss");
      df.setTimeZone( TimeZone.getTimeZone("GMT") );
      return df.format(d) + "." + dec;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- format ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static String format( double d, int nd )
   {
      if( (new Double(d)).isNaN() )
         return "";
      StringBuffer fm = new StringBuffer( "###,###,###,##0" );
      if( nd > 0 )
         fm.append( "." );
      for( int i=0; i<nd; i++ )
         fm.append( "0" );
      NumberFormat formatter = new DecimalFormat( fm.toString() );
      return formatter.format(d);
   }
   public static String format( double d )
   {
      return format( d, 2 );
   }
   public static String format( String d )
   {
      return format( d, 0 );
   }
   public static String format( String d, int nd )
   {
      try
      {
         return format( Double.parseDouble(d), nd );
      }
      catch( Exception e )
      {
         return d;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- safePath ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String safePath( String path )
   {
      String sep     =  File.separator;
      String newPath =  path.replace( "/", sep );
      while( newPath.contains(sep+sep) )
      {
         newPath = newPath.replace( sep+sep, sep);
      }
      return newPath;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- decode64 ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String decode64( String in )
   {
      try
      {
         return new String( Base64.getDecoder().decode( in.getBytes() ) );
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return null;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- encode64 ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String encode64( String in ) throws Exception
   {
      try
      {
         return new String( Base64.getEncoder().encode( in.getBytes() ) );
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return null;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- nvl ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static String nvl( String txt )
   {
      return nvl( txt, "" );
   }
   public static String nvl( String txt, String def )
   {
      if( txt==null )
         return def;
      return txt;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- pureText ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String pureText( String input )
   {
      String output = "";
      for( int i=0; i<input.length(); i++ )
      {
         int n = (int) input.charAt(i);
         if( n>=32 && n<126 )
            output += ((char) n);
      }
      return output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- doubleArray ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String[][] doubleArray( String input[] )
   {
      String output[][] = new String[input.length][2];
      for( int i=0; i<input.length; i++ )
      {
         output[i][0] = input[i];
         output[i][1] = input[i];
      }
      return output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- array2Json ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static String array2Json( ArrayList al )
   {
      String data = "[";
      for( int i=0; i<al.size(); i++ )
      {
         Object o = al.get(i);
         if( o instanceof String )
            data += (i==0?"":",") + "\"" + o + "\"";
         else
            data += (i==0?"":",") + hash2Json( (HashMap) o );
      }
      data += "]";
      return data;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- hash2Json ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String hash2Json( HashMap hm )
   {
      Iterator    it       =  hm.keySet().iterator();
      JSONObject  jObj     =  new JSONObject();
      while( it.hasNext() )
      {
         String k = it.next().toString();
         Object o = hm.get(k);
         try
         {
            if( o instanceof String )
               jObj.put( k, o.toString() );
            else
            if( o instanceof HashMap )
               jObj.put( k, hash2Json( (HashMap) o ) );
         }
         catch( Exception e )
         {
         }
      }
      return jObj.toString();
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- entity ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static String entity( String input )
   {
      String output = "";
      for( int i=0; i<input.length(); i++ )
      {
         char c = input.charAt(i);
         int  n = (int) c;
         if(  n > 127 )
            output += "&#" + n + ";";
         else
            output += c;
      }
      return output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- hashGet ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String hashGet( HashMap hm, String ky, String dflt )
   {
      String val = (String) hm.get( ky );
      return (val==null?dflt:val);
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- millis2Time ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String millis2Time( long millis )
   {
      String  dy    =  "" + ( TimeUnit.MILLISECONDS.toDays(    millis   )                                                                             );
      String  hr    =  "" + ( TimeUnit.MILLISECONDS.toHours(   millis   )  -  TimeUnit.DAYS.toHours(        TimeUnit.MILLISECONDS.toDays(    millis)) );
      String  min   =  "" + ( TimeUnit.MILLISECONDS.toMinutes( millis   )  -  TimeUnit.HOURS.toMinutes(     TimeUnit.MILLISECONDS.toHours(   millis)) );
      String  sec   =  "" + ( TimeUnit.MILLISECONDS.toSeconds( millis   )  -  TimeUnit.MINUTES.toSeconds(   TimeUnit.MILLISECONDS.toMinutes( millis)) );
      return  dy + " " + pad( hr, "0", 2 ) + ":" + pad( min, "0", 2 ) + ":" + pad( sec, "0", 2 );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- multiSpaces ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String multiSpaces( String input )
   {
      return input.replaceAll("\\s+", " ");
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- parseInt ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static int parseInt( String input, int deflt )
   {
      try
      {
         return Integer.parseInt(input);
      }
      catch( Exception e )
      {
         return deflt;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- ranking ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String ranking( String input )
   {
      return input.replace( "st", "<sup>st</sup>" ).replace( "nd", "<sup>nd</sup>" ).replace( "rd", "<sup>rd</sup>" ).replace( "th", "<sup>th</sup>" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- randomInt ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static int randomInt()
   {
      return (int) (new Random()).nextInt(512);
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- sortStringIds ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String sortStringIds( String input )
   {
      try
      {
         String splt[]  =  input.split(",");
         long   longs[] =  new long[ splt.length ];
         String r       =  "";
         for( int i=0; i<splt.length; i++ )
            longs[i] = Long.parseLong( splt[i] );
         Arrays.sort( longs );
         for( int i=0; i<longs.length; i++ )
            r += "," + longs[i];
         return r.substring(1);
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- pin ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static String pin( int size )
   {
      String m = "" + System.currentTimeMillis();
      return m.substring( m.length() - size );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- ratio ...                                                         ---*/
   /*-------------------------------------------------------------------------*/
   public static double ratio( int     n1, int     n2 )  {  return (double) (((double) n1) / ((double) n2));   }
   public static double ratio( int     n1, long    n2 )  {  return (double) (((double) n1) / ((double) n2));   }
   public static double ratio( double  n1, int     n2 )  {  return (double) (((double) n1) / ((double) n2));   }
   public static double ratio( int     n1, double  n2 )  {  return (double) (((double) n1) / ((double) n2));   }
   public static double ratio( double  n1, double  n2 )  {  return (double) (((double) n1) / ((double) n2));   }
   
   /*-------------------------------------------------------------------------*/
   /*--- splitString ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String splitString( String input )
   {
      return "\"" + input.replace( ",", "\",\"" ) + "\"";
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- array2String ...                                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static String array2String( String input[] )
   {
      String output = "";
      for( int i=0; i<input.length; i++ )
         output += (i==0?"":",") + input[i];
      return output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- replaceAt ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String replaceAt( String cnt, String find, String replace, int n )
   {
      int start = 0;
      for( int i=0; i<n; i++ )
      {
         start = cnt.indexOf( find, start );
         if( start==-1 ) return cnt;
         start++;
      }
      return cnt.substring( 0, start-1 ) + replace + cnt.substring( start-1 + find.length());
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- cleanFileName ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String cleanFileName( String name )
   {
      String newName    =  "";
      String invalids   =  "}#%{}\\<>*?/$!'\":@+`|=},";
      for( int i=0; i<name.length(); i++ )
      {
         String ndx = "" + name.charAt(i);
         if( !invalids.contains(ndx) )
            newName += ndx;
      }
      return newName.replace( "&", "-and-" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getFirstWords ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String getFirstWords( String sentence, int nWords )
   {
      String split[] =  sentence.split( "[\\s,;]+" );
      String out     =  "";
      for( int i=0; i<nWords && i<split.length; i++ )
         out += (i==0?"":" ") + split[i];
      return out;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- hash2URI ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String hash2URI( HashMap hmInput )
   {
      String   output   =  "";
      Iterator it       =  hmInput.keySet().iterator();
      while( it.hasNext() )
      {
         String k = it.next().toString();
         String v = (String) hmInput.get( k );
         try { output += "&" + k + "=" + URLEncoder.encode( v, "UTF-8" ); } catch( Exception e ) {}
      }
      try {    output = output.substring(1); } catch( Exception e ) {}
      return   output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- translateUA ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String translateUA( String ua )
   {
      String uUa = ua.toUpperCase();
      
      if( uUa.contains( "ADSBOT"                               ) ) return "bot";
      if( uUa.contains( "CRAWLER"                              ) ) return "bot";
      if( uUa.contains( "ROBOT"                                ) ) return "bot";
      if( uUa.contains( "YAHOO AD MONITORING"                  ) ) return "bot";
      if( uUa.contains( "GOOGLEBOT"                            ) ) return "bot";
      if( uUa.contains( "SEMRUSH"                              ) ) return "bot";
      
      if( uUa.contains( "IPAD"                                 ) ) return "ipad";
      if( uUa.contains( "IPHONE"                               ) ) return "iphone";
      if( uUa.contains( "ANDROID"                              ) ) return "android";
      
      if( uUa.contains( "FIREFOX"                              ) ) return "web";
      if( uUa.contains( "(X11; CROS"                           ) ) return "web";
      if( uUa.contains( "INTEL MAC OS"                         ) ) return "web";
      if( uUa.contains( "WINDOWS"                              ) ) return "web";
      if( uUa.contains( "SAMSUNGBROWSER"                       ) ) return "web";
      if( uUa.contains( "(X11; LINUX"                          ) ) return "web";
      if( uUa.contains( "LINUX") && !uUa.contains( "ANDROID" )   ) return "web";
      
      return "unknown";
   }
   
   /*-------------------------------------------------------------------------*/
   /*   getColor ...                                                          */
   /*-------------------------------------------------------------------------*/
   public static Color getColor( String str ) throws NumberFormatException
   {
      String _str = str;
      _str = _str.startsWith("#")?_str.substring(1):_str;
      int z = Integer.parseInt(_str, 16);
      return new Color(z >>> 16 & 0xff, z >>> 8 & 0xff, z & 0xff);
   }
   public static Color getColor( String str, int opacity ) throws NumberFormatException
   {
      String _str = str;
      _str = _str.startsWith("#")?_str.substring(1):_str;
      int z = Integer.parseInt(_str, 16);
      return new Color(z >>> 16 & 0xff, z >>> 8 & 0xff, z & 0xff, opacity);
   }
   
   /*-------------------------------------------------------------------------*/
   /*   increaseColor ...                                                     */
   /*-------------------------------------------------------------------------*/
   public static Color increaseColor( Color c, int step ) throws NumberFormatException
   {
      int r = c.getRed();
      int g = c.getGreen();
      int b = c.getBlue();
      r += step;
      g += step;
      b += step;
      if( r>255 || g>255 || b>255 || r<0 || g<0 || b<0 )
         return c;
      return new Color( r, g, b );
   }
   public static String increaseColor( String c, int step )
   {
      Color    cl1   =  getColor( c );
      Color    cl2   =  increaseColor( cl1, step );
      int      r     =  cl2.getRed();
      int      g     =  cl2.getGreen();
      int      b     =  cl2.getBlue();
      
      String   hR    =  Integer.toHexString(r);  hR =  pad( hR, "0", 2 );
      String   hG    =  Integer.toHexString(g);  hG =  pad( hG, "0", 2 );
      String   hB    =  Integer.toHexString(b);  hB =  pad( hB, "0", 2 );
      
      return   hR+hG+hB;
      
      /*
      Color cl1 = getColor( c );
      Color cl2 =  increaseColor( cl1, step );
      int r = cl2.getRed();
      int g = cl2.getGreen();
      int b = cl2.getBlue();
      return Integer.toHexString(r) + Integer.toHexString(g) + Integer.toHexString(b);
      */
   }

   /*-------------------------------------------------------------------------*/
   /*   al2String ...                                                         */
   /*-------------------------------------------------------------------------*/
   public static String al2String( ArrayList al )
   {
      return al.toString().replace("[", "").replace("]", "");
   }
   
   /*-------------------------------------------------------------------------*/
   /*   escapeLink ...                                                         /
   /*-------------------------------------------------------------------------*/
   public static String escapeLink( String link )
   {
      return link;
//    return link.replaceAll("\\s+","+").replace("&", "%26");
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- cleanInput ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static String cleanInput( String input )
   {
      String clean  = input.replaceAll("\\s+", " ");
      String output = "";
      for( int i=0; i<clean.length(); i++ )
      {
         char c = clean.charAt(i);
         if( c==' ' || (c<='9' && c>='0') || (c<='z' && c>='a') || (c<='Z' && c>='A') )
            output += c;
      }
      return output;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- cropBottomBlanks ...                                              ---*/
   /*-------------------------------------------------------------------------*/
   public static String[] cropBottomBlanks( String[] input )
   {
      if( input == null || input.length == 0 )
         return new String[0];
      
      int lastNonEmptyIndex = -1;
      for( int i=input.length - 1; i >= 0; i-- )
      {
         if( !input[i].trim().isEmpty() )
         {
            lastNonEmptyIndex = i;
            break;
         }
      }
      
      if( lastNonEmptyIndex == -1 )
         return new String[0];
   
      String[] result = new String[lastNonEmptyIndex + 1];
      System.arraycopy( input, 0, result, 0, lastNonEmptyIndex + 1 );
   
      return result;
   }
   
   
   
   /*-------------------------------------------------------------------------*/
   /*--- lines2Json ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static String lines2Json( String input )
   {
      String   split[]  =  input.replace("\r", "").split( "\n" );
      String   output   =  "";
      for( int i=0; i<split.length; i++ )
      {
         split[i] = split[i].trim();
         if( split[i].length()==0 )
            continue;
         output += ",\"" + split[i].replace("\"", "") + "\"";
      }
      try { output = output.substring(1); } catch( Exception e ) {}
      return "[" + output + "]";
   }

   /*-------------------------------------------------------------------------*/
   /*--- packArray ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String[] packArray( String[] input )
   {
      return packArray( input, input.length );
   }
   public static String[] packArray( String[] input, int size )
   {
      ArrayList list = new ArrayList();
      for( int i=0; i<input.length; i++ )
         if( input[i].trim().length() != 0 )
            list.add( input[i].trim() );
      int min = Math.min( list.size(), size );
      return (String[]) list.toArray(new String[min]);
   }

   
   /*-------------------------------------------------------------------------*/
   /*--- convertMD ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static String convertMD( String markdown )
   {
      MutableDataSet options = new MutableDataSet();
      options.set( Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()) );
      
      Parser         parser   =  Parser.builder(options).build();
      HtmlRenderer   renderer =  HtmlRenderer.builder(options).build();
      Node           document =  parser.parse(markdown);
      String         html     =  renderer.render(document);
      return         html;
   }
}
