import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class DB
{
   private  static   String         URL      =  "jdbc:mysql://pdf.c6dns8h9ygqc.us-east-1.rds.amazonaws.com/dialog";
   private  static   String         USER     =  "chucky";
   private  static   String         PASSWORD =  "password";
   private  static   HashMap        CACHE    =  new HashMap();
   
   
   /*-------------------------------------------------------------------------*/
   /*--- connect ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   static Connection connect()
   {
      Connection conn = null;
      try
      {
//       Class.forName( "com.mysql.jdbc.Driver"    );
         Class.forName( "com.mysql.cj.jdbc.Driver" );
         conn = DriverManager.getConnection( URL, USER, PASSWORD );
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
      return conn;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- disconnect ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   static void disconnect( Connection conn )
   {
      try { conn.close(); } catch( Exception e ) {}
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- exists ...                                                        ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean exists( String query )
   {
      return (getRecord( query )!=null);
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getRecord ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static LinkedHashMap getRecord( String query, boolean cache )
   {
      try
      {
         return (LinkedHashMap) getData(query, cache).get(0);
      }
      catch( Exception e )
      {
         return ( new LinkedHashMap() );
      }
      
   }
   public static LinkedHashMap getRecord( String query )
   {
      return getRecord( query, false );
   }
   public static LinkedHashMap getRecord( Statement stmt, String query )
   {
      try
      {
         return (LinkedHashMap) getData(stmt, query).get(0);
      }
      catch( Exception e )
      {
         return ( new LinkedHashMap() );
      }
   }
   
   public static ArrayList getData( Statement stmt, String query )
   {
      ResultSet            rs   = null;
      ResultSetMetaData    md   = null;
      int                  cols = 0;
      ArrayList            al   = new ArrayList();
      try
      {
//       Log.log( query );
         rs   = stmt.executeQuery( query );
         md   = rs.getMetaData();
         cols = md.getColumnCount();
         while( rs.next() )
         {
            LinkedHashMap hm = new LinkedHashMap();
            for( int j=0; j<cols; j++ )
            {
               String colType = md.getColumnTypeName(j+1);
               String colLbl  = md.getColumnLabel(j+1);
               String colVal  = rs.getString( colLbl );
                      colVal  = (colVal==null)?"":colVal.trim();
               hm.put( colLbl.replaceAll("\\(.+?\\)", "").trim().toLowerCase(), colVal );
            }
            al.add( hm );
         }
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
      finally
      {
         try { rs.close();             } catch( Exception e ) {}
      }
      return al;
   }
   
   public static ArrayList getData( String query )
   {
      return getData( query, false );
   }
   public static ArrayList getData( String query, boolean cache )
   {
      long        start =  System.currentTimeMillis();
      Connection  conn  =  null;
      Statement   stmt  =  null;
      ArrayList   al    =  (ArrayList) CACHE.get( query );
      if( al!=null && cache )
         return al;
      al = new ArrayList();
      try
      {
         conn = connect();
         stmt = conn.createStatement();
         al   = getData( stmt, query );
      }
      catch( Exception e )
      {
         al = null;
         e.printStackTrace();
      }
      finally
      {
         try { stmt.close();           } catch( Exception e ) {}
         disconnect( conn );
      }
      if( !al.isEmpty() && cache )
         CACHE.put( query, al );
      return al;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- executeSQL ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static String executeSQL( String query ) throws Exception
   {
//    System.out.println( query );
      Connection  conn     =  null;
      Statement   stmt     =  null;
      String      r        =  "-1";
      Exception   thr      =  null;
      try
      {
         conn = connect();
         stmt = conn.createStatement();
         r    = executeSQL( stmt, query );
      }
      catch( Exception e )
      {
         thr = e;
         e.printStackTrace();
      }
      finally
      {
         try { stmt.close();          } catch( Exception e ) {}
         try { disconnect(conn);      } catch( Exception e ) {}
      }
      if( thr!=null )
      {
         throw thr;
      }
      return r;
   }
   public static String executeSQL( Statement stmt, String query ) throws Exception
   {
//    System.out.println( query );
      stmt.executeUpdate( query );
      String lastID = ((LinkedHashMap) getData( stmt, "select last_insert_id() id" ).get(0)).get("id").toString();
      return lastID;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getHashData ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static LinkedHashMap getHashData( String sql, String key, String val )
   {
      try
      {
         LinkedHashMap h = new LinkedHashMap();
         ArrayList     v = getData(sql);
                       h = new LinkedHashMap();
         if( v.size()==0 )
            return h;
         for( int i=0; i<v.size(); i++ )
         {
            LinkedHashMap hm = (LinkedHashMap) v.get(i);
            h.put( hm.get(key), hm.get(val) );
         }
         return h;
      }
      catch( Exception e )
      {
         return (new LinkedHashMap());
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getHashValue ...                                                  ---*/
   /*-------------------------------------------------------------------------*/
   public static String getHashValue( String sql, String key, String val, String match )
   {
      LinkedHashMap hm = getHashData( sql, key, val );
      String    vMatch = (String) hm.get(match);
      if( vMatch!=null )
         return vMatch;
      hm       = getHashData( sql, key, val );
      vMatch   = (String) hm.get(match);
      return vMatch;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getJsonData ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static String getJsonArray( String sql, String key )
   {
      String    jSon =  "";
      ArrayList al   =  DB.getData( sql );
      for( int i=0; i<al.size(); i++ )
      {
         String v = (String) ((HashMap) al.get(i)).get(key);
         if( v!=null )
            jSon += ",\"" + v + "\"";
      }
      try { jSon = jSon.substring(1); } catch( Exception e ) {}
      return "[" + jSon + "]";
   }
   
   public static String getJsonData( String sql, String key, String val )
   {
      String         ky =  sql+key+val;
      String       jSon =  null;
      LinkedHashMap  hm =  getHashData( sql, key, val );
      Iterator       it =  hm.keySet().iterator();
                   jSon =  "";
      while( it.hasNext() )
      {
         String k = it.next().toString();
         String v = hm.get(k).toString();
         jSon += ",\"" + k + "\":\"" + v + "\"";
      }
      try { jSon = jSon.substring(1); } catch( Exception e ) {}
      return "{" + jSon + "}";
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getIntValue ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   public static int getIntValue( String sql )
   {
      try { return Integer.parseInt( getValue(sql) ); } catch( Exception e ) { return -1; }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getValue ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static String getValue( String sql, String dft )
   {
      String v = getValue( sql );
             v = v.equals("")?dft:v;
      return v;
   }
   public static String getValue( String sql )
   {
      return getValue( sql, false );
   }
   public static String getValue( String sql, boolean cache )
   {
      if( cache )
      {
         String v = Format.getHashValue( CACHE, sql );
         if( !v.equals("") )
            return v;
      }
      
      
      try
      {
         HashMap    hm    =   getRecord( sql );
         String     match =   hm.keySet().toArray()[0].toString();
         String    vMatch =   (String) hm.get(match);
         if( cache )
            CACHE.put( sql, vMatch );
         return    vMatch;
      }
      catch( Exception e )
      {
         return "";
      }
   }
   public static String getValue( Statement stmt, String sql )
   {
      try
      {
         HashMap    hm    =   getRecord( stmt, sql );
         String     match =   hm.keySet().toArray()[0].toString();
         String    vMatch =   (String) hm.get(match);
         return    vMatch;
      }
      catch( Exception e )
      {
         return "";
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getValues ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static ArrayList getValues( String sql )
   {
      return getValues( sql, false );
   }
   public static ArrayList getValues( String sql, boolean cache )
   {
      ArrayList alOut = (ArrayList) CACHE.get( sql );
      if( cache && alOut!=null )
         return alOut;
      
      alOut = new ArrayList();
      try
      {
//       System.out.println( "   GET FROM DB" );
         ArrayList al = getData( sql );
         for( int i=0; i<al.size(); i++ )
         {
            HashMap hm = (HashMap) al.get(i);
            alOut.add( hm.get(hm.keySet().iterator().next()) );
         }
         if( cache )
            CACHE.put( sql, alOut );
      }
      catch( Exception e )
      {
      }
      return alOut;
   }
   
   
   public static String getValue( String sql, String key, String dflt )
   {
      try
      {
         return DB.getRecord(sql).get(key).toString();
      }
      catch( Exception e )
      {
         return dflt;
      }
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- perPage ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static String perPage( int pgNum, int perPage )
   {
      return " limit " + (pgNum-1)*perPage + "," + perPage;
   }
   
}
