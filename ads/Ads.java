import java.util.*;
import java.io.*;

public class Ads
{
   private static HashMap INVENTORIES = new HashMap();
   
   /*-------------------------------------------------------------------------*/
   /*--- main ...                                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static void main( String args[] ) throws Exception
   {
      String      base        =  "";
      String      nProcess    =  "";
      try   {     nProcess    =  args[0]; } catch( Exception e ) {}
      try   {     base        =  args[1]; } catch( Exception e ) {}
      run( base, nProcess );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- run ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static void run( String base, String  nProcess ) throws Exception
   {
      ArrayList   alQ         =  new ArrayList();
      String      sql         =  "";
      String      apiKey      =  "";
      
      sql = "select * from multi_queue2 where type='ads'";
      if( base.equals("0") && nProcess.equals("0") )
      {
         alQ      =  DB.getData( sql );
         sql      =  "select apikey from open_ai_api limit 1";
         apiKey   =  DB.getValue( sql );
      }
      else
      {
         sql      =  sql + (nProcess.equals("")?"":(" and id%"+base+"="+nProcess));
         alQ      =  DB.getData( sql );
         sql      = "select apikey from open_ai_api" + (nProcess.equals("")?"":(" where id%"+base+"="+nProcess));
         apiKey   =  DB.getValue( sql );
      }
      
      for( int i=0; i<alQ.size(); i++ )
      {
         //--- get queue item info ---
         HashMap     hmQ            =  (HashMap) alQ.get(i);
         String      queueId        =  Format.getHashValue( hmQ, "id"         );
         String      rule           =  Format.getHashValue( hmQ, "rule"       );
         String      queueType      =  Format.getHashValue( hmQ, "type"       );
         String      queueSubType   =  Format.getHashValue( hmQ, "sub_type"   );
                     queueSubType   =  Format.hyphenize( queueSubType );
         String      nodeId         =  Format.getHashValue( hmQ, "node_id"    );
         String      domainId       =  Format.getHashValue( hmQ, "domain_id"  );
         String      levelType      =  Format.getHashValue( hmQ, "level_type" );
         
         //--- get shell, domain, vertical, sub-vertical info ---
         String      shellId        =  getShellId( levelType, nodeId );
         String      subVID         =                       DB.getValue(   "select id          from sub_verticals where id in (select sub_vertical_id from domains where id=" + domainId + ")", true  );
         String      vID            =                       DB.getValue(   "select vertical_id from sub_verticals where id = "   +  subVID,                                                     true  );
         String      subVLabel      =                       DB.getValue(   "select label       from sub_verticals where id = "   +  subVID,                                                     true  );
         String      vLabel         =                       DB.getValue(   "select label       from verticals2    where id = "   +  vID,                                                        true  );
         String      shellName      =                       DB.getValue(   "select label       from shells        where id="     +  shellId,                                                    true  );
         String      subVerticals   =                       DB.getValue(   "select tokens      from sub_verticals where id = "   +  subVID,                                                     true  );
                     subVerticals   =                       subVerticals.equals("")?"[]":subVerticals;
         ArrayList   subTokens      =  Format.json2Array(   subVerticals );
         HashMap     domainTokens   =  Format.hashJson(     DB.getValue(   "select tokens      from domains       where id = "   +  domainId,                                                   true  ) );
         ArrayList   alLevels       =  DB.getData(                         "select * from shell_levels where shell_id="          +  shellId + " order by level",                                true  );
         
         
         //--- load prompts ---
         String      prompts[]      =  Format.getHashValue( hmQ, "prompts" ).replaceAll("\\[|\\]|\\s", "").replace(" ", "").split(",");
         
         //--- load inventory ---
         ArrayList   alInv          =  inventory( domainId );
         ArrayList   subInv         =  extractFromInventory( alInv, nodeId, levelType );
         HashMap     hmCompleted    =  new HashMap();
         for( int j=0; j<subInv.size(); j++ )
         {
            ArrayList   alSub       =  (ArrayList) subInv.get(j);
            for( int k=0; k<prompts.length; k++ )
            {
               boolean success = true;
               String   cntHeadlinesResp     = "";
               String cntDescResp            = "";
               for( int xx=0; xx<10; xx++ )
               {
                  success = true;
                  try
                  {
                     HashMap  actualNode  =  (HashMap) alSub.get( k );
                     String   id          =  Format.getHashValue( actualNode, "id"   );
                     if( !prompts[k].equals("0") )
                     {
                                 sql               =  "select * from prompt_ads_association where id=" + prompts[k];
                        HashMap  hmPrompts         =  DB.getRecord( sql, true );
                        String   headline_id       =  Format.getHashValue( hmPrompts, "headline_id"      );
                        String   description_id    =  Format.getHashValue( hmPrompts, "description_id"   );
                        String   promptLabel       =  Format.getHashValue( hmPrompts, "label"            );
                        String   headlinePrompt    =  DB.getValue( "select prompt from prompts2 where id=" + headline_id,    true );
                                 headlinePrompt    =  merge( headlinePrompt,     domainId, shellName, vLabel, subVLabel, domainTokens, alSub );
                        String   descriptionPrompt =  DB.getValue( "select prompt from prompts2 where id=" + description_id, true );
                                 descriptionPrompt =  merge( descriptionPrompt,  domainId, shellName, vLabel, subVLabel, domainTokens, alSub );
                        DB.executeSQL( "delete from node_ads where node_id=" + id + " and association_id=" + prompts[k] );

                        if( !headlinePrompt.equals("") && !descriptionPrompt.equals("") )
                        {
                           System.out.print( "#" );
                           HashMap  hmHeadlinesResp      =  OpenAI.call( headlinePrompt,    apiKey );
                           HashMap  hmDescResp           =  OpenAI.call( descriptionPrompt, apiKey );
                                    cntHeadlinesResp     =  Format.getHashValue( hmHeadlinesResp,  "content"   ).trim().replace( "\r", "" ).replace( "\\n{2,}", "\n" );
                                    cntDescResp          =  Format.getHashValue( hmDescResp,       "content"   ).trim().replace( "\r", "" ).replace( "\\n{2,}", "\n" );
                           String   contentHeadlines[]   =  Format.packArray( cntHeadlinesResp.replace("\r", "").split("\n"), 13 ); //  split("[.!?\\r\\n]+"), 13 );
                           String   contentDesc[]        =  Format.packArray( cntDescResp     .replace("\r", "").split("\n"),  5 ); //  split("[.!?\\r\\n]+"),  5 );
                           if( contentHeadlines.length<13 ) System.out.println( "===> less than 13" );
                           if( contentDesc.length<5       ) System.out.println( "===> less than 5"  );


                           if( contentHeadlines.length<13 || contentDesc.length<5 )
                           {
                              success = false;
                           }
                           else
                           {
                              sql = "insert into node_ads (label, domain_id, node_id, node_type, prompt_headline, prompt_description, association_id";
                              for( int x=1; x<=13; x++ )  sql   += ",headline"    + Format.pad( ""+x, "0", 2 );
                              for( int x=1; x<= 5; x++ )  sql   += ",description" + Format.pad( ""+x, "0", 2 );
                                       sql   += ") values (";
                                       sql   += "\"" + promptLabel + "\"," + domainId + "," + id + "," + "'" + levelType + "',\"" + headlinePrompt.replace("\"", "\\\"") + "\",\"" + descriptionPrompt.replace("\"", "\\\"") + "\"," + prompts[k];
                        
                              for( int x=0; x<13; x++ )
                              {
                                 contentHeadlines[x] = contentHeadlines[x].trim();
                                 if( contentHeadlines[x].length() > 30 )
                                    contentHeadlines[x] = rephrase( contentHeadlines[x], 30, apiKey );
                                 if( contentHeadlines[x].length() > 30 )
                                 {
                                    System.out.println( "\n===> \"" + contentHeadlines[x] + "\"" );
                                    System.out.println( "===> OVER 30 Chars" );
                                    success = false;
                                 }
                                 sql   += ",\"" + contentHeadlines[x] + "\"";
                              }
                              for( int x=0; x< 5; x++ )
                              {
                                 contentDesc[x] = contentDesc[x].trim();
                                 if( contentDesc[x].length() > 90 )
                                    contentDesc[x] = rephrase( contentDesc[x], 90, apiKey );
                                 if( contentDesc[x].length() > 90 )
                                 {
                                    System.out.println( "\n===> \"" + contentDesc[x] + "\"" );
                                    System.out.println( "===> OVER 90 Chars" );
                                    success = false;
                                 }
                                 sql   += ",\"" + contentDesc[x]      + "\"";
                              }
                              sql += ")";
                              if( success )
                                 DB.executeSQL( sql );
                           }
                        }
                     }
                  }
                  catch( IndexOutOfBoundsException e )
                  {
                  }
                  if( success )
                  {
                     break;
                  }
                  else
                  {
                     System.out.print( " " );
                  }
               }
            }
         }
         DB.executeSQL( "delete from multi_queue2 where id=" + queueId );
      }
   }
   
   
   /*-------------------------------------------------------------------------*/
   /*--- merge ...                                                         ---*/
   /*-------------------------------------------------------------------------*/
   private static String merge( String inputPrompt, String domainId, String shellName, String vLabel, String subVLabel, HashMap domainTokens, ArrayList alSub )
   {
      if( inputPrompt.equals("") )  return "";
      
      String   outputPrompt   =  inputPrompt;
               outputPrompt   =  outputPrompt.replace( "{CurrentYear}",    Format.today().split("-")[0]  );
               outputPrompt   =  outputPrompt.replace( "{Domain Name}",    getDomainName(domainId)       );
               outputPrompt   =  outputPrompt.replace( "{Shell}",          shellName                     );
               outputPrompt   =  outputPrompt.replace( "{Vertical}",       vLabel                        );
               outputPrompt   =  outputPrompt.replace( "{Sub Vertical}",   subVLabel                     );
      
      //---- merging prompt with domain ---
      Iterator it = domainTokens.keySet().iterator();
      while( it.hasNext() )
      {
         String   ky             =  it.next().toString();
         String   vl             =  Format.getHashValue( domainTokens, ky );
                  outputPrompt   =  outputPrompt.replace( "{domain." + ky + "}", vl );
      }
      
      //---- merging prompt with node data ---
      for( int i=0; i<alSub.size(); i++ )
      {
         HashMap  hmSub          =  (HashMap) alSub.get(i);
         String   leveltype      =  Format.getHashValue( hmSub, "leveltype" );
         String   label          =  Format.getHashValue( hmSub, "label"     );
         if( outputPrompt.toLowerCase().contains("{" + leveltype.toLowerCase() + "}") )
         {
            outputPrompt   =  outputPrompt.replace( "{" + leveltype + "}",                   label );
            outputPrompt   =  outputPrompt.replace( "{" + Format.initCap(leveltype) + "}",   label );
         }
      }
      
      return outputPrompt;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- display ...                                                       ---*/
   /*-------------------------------------------------------------------------*/
   public static void display( String[] input )
   {
      for( int i=0; i<input.length; i++ )
         System.out.println( "   ===> \"" + input[i] + "\"" );
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- rephrase ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   private static String rephrase( String input, int length, String apiKey )
   {
      HashMap  hmResponse  =  OpenAI.call( "Rephrase \"" + input + "\" under " + length + " characters.\nPlease justt rephrase and do not say anything else",    apiKey );
      String   rephrased   =  Format.getHashValue( hmResponse,  "content" ).trim().replace( "\"", "" );
      System.out.println( "\n\"" + input + "\"\nrephrased to\n\"" + rephrased + "\"" );
      return   rephrased;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getShellId ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   public static String getShellId( String levelType, String nodeId )
   {
      String sql = "";
      if( levelType.equals("shell") )
         sql = "select shell_id from shell_nodes where id=" + nodeId;
      else
         sql = "select s.shell_id from sub_verticals s, domains d where d.sub_vertical_id=s.id and d.id in (select domain_id from domain_nodes where id=" + nodeId + ")";
      return DB.getValue( sql, true );
   }

   /*-------------------------------------------------------------------------*/
   /*--- inventory ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   public static ArrayList inventory( String domainId )
   {
      ArrayList   output   =  (ArrayList) INVENTORIES.get(domainId);
      if( output!=null )
         return output;
                  output   =  new ArrayList();
      String      shellId  =  DB.getValue(    "select s.shell_id from sub_verticals s, domains d where d.sub_vertical_id=s.id and d.id="             +  domainId                 );
      HashMap     hmShell  =  DB.getHashData( "select e.label, n.id  from shell_nodes  n, shell_entities  e where n.entity_id=e.id and n.shell_id="   +  shellId,  "id", "label"  );
      HashMap     hmDomain =  DB.getHashData( "select e.label, n.id  from domain_nodes n, domain_entities e where n.entity_id=e.id and n.domain_id="  +  domainId, "id", "label"  );
      HashMap     hmLevels =  DB.getHashData( "select   level, label from shell_levels where shell_id=" + shellId, "level", "label" );
      String      sql      =  "call get_process_inventory_nodes(" + domainId + ", false)";
      ArrayList   al       =  DB.getData( sql );
      for( int i=0; i<al.size(); i++ )
      {
         HashMap        hm       =  (HashMap) al.get(i);
         LinkedHashMap  hmOut    =  new LinkedHashMap();

         String        _shell    =  Format.getHashValue( hm, "shell" );
         String        _node     =  Format.getHashValue( hm, "node"  );

         String         shell[]  =  _shell.split(",");
         String         node[]   =  _node .split(",");
         ArrayList      alIds    =  new ArrayList();

         for( int j=0; j<shell.length; j++ )
         {
            HashMap hmTmp = new HashMap();
            if( !shell[j].equals("") )
            {
               hmTmp.put( "id",        shell[j]                                );
               hmTmp.put( "type",      "shell"                                 );
               hmTmp.put( "label",     Format.getHashValue(hmShell,  shell[j]) );
               hmTmp.put( "leveltype", Format.getHashValue(hmLevels, ""+(j+1)) );
               alIds.add( hmTmp );
            }
         }
         for( int j=0; j<node.length;  j++ )
         {
            HashMap hmTmp = new HashMap();
            if( !node[j].equals("") )
            {
               hmTmp.put( "id",        node[j]                                 );
               hmTmp.put( "type",      "domain"                                );
               hmTmp.put( "label",     Format.getHashValue(hmDomain, node[ j]) );
               //--- WTF??? THIS BUG REALLY KICKED MY ASS. I did not realize _shell could be empty
               hmTmp.put( "leveltype", Format.getHashValue(hmLevels, ""+(j+1+(_shell.equals("")?0:shell.length))) );
               alIds.add( hmTmp );
            }
         }
         output.add( alIds );
      }
      INVENTORIES.put( domainId, output );
      return output;
   }
   
   
   /*-------------------------------------------------------------------------*/
   /*--- extractFromInventory ...                                          ---*/
   /*-------------------------------------------------------------------------*/
   public static ArrayList extractFromInventory( ArrayList alInv, String nodeId, String levelType )
   {
      ArrayList   alSub =  new ArrayList();
      for( int i=0; i<alInv.size(); i++ )
      {
         ArrayList al = (ArrayList) alInv.get(i);
         for( int j=0; j<al.size(); j++ )
         {
            HashMap  hm    =  (HashMap) al.get(j);
            String   id    =  Format.getHashValue( hm, "id"    );
            String   type  =  Format.getHashValue( hm, "type"  );
            if( id.equals(nodeId) && type.equals(levelType) )
               alSub.add( al );
         }
      }
      return alSub;
   }
   
   /*-------------------------------------------------------------------------*/
   /*--- getDomainName ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String getDomainName( String domainId )
   {
      return DB.getValue( "select name from domains where id=" + domainId, true );
   }
}
