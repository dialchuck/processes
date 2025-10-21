import java.util.*;
import java.sql.*;
import java.io.*;

public class Multi
{
   private static HashMap INVENTORIES = new HashMap();

   /*-------------------------------------------------------------------------*/
   /*--- run ...                                                           ---*/
   /*-------------------------------------------------------------------------*/
   public static void run( String base, String  nProcess ) throws Exception
   {
      run( base, nProcess, "" );
   }
   public static void run( String base, String  nProcess, String queueName ) throws Exception
   {
      DB.executeSQL( "delete from zap" );
      
      
      ArrayList   alQ         =  new ArrayList();
      String      sql         =  "";
      String      where       =  queueName.equals("")?"":(" where type='" + queueName + "'");
      String      apiKey      =  "";
      sql = "select * from multi_queue2 where type!='image' and sub_type!='Headlines & Descriptions' and sub_type!='Image Generator'";
      if( base.equals("0") && nProcess.equals("0") )
      {
         alQ      =  DB.getData( sql );
         sql      =  "select apikey from open_ai_api limit 1";
         apiKey   =  DB.getValue( sql );
      }
      else
      {
         sql     +=  (nProcess.equals("")?"":(" and id%"+base+"="+nProcess));
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
            ArrayList alSub = (ArrayList) subInv.get(j);
            for( int k=0; k<prompts.length; k++ )
            {
               if( exitProcess(nProcess) )
               {
                  System.out.println( "--------------------------------"            );
                  System.out.println( "--- PROCESS HAS BEEN STOPPED ---"            );
                  System.out.println( "--------------------------------"            );
                  DB.executeSQL(       "delete from zap where record=" + nProcess   );
                  System.exit(0);
               }
               if( prompts[k].equals("0") )
               {
                  System.out.println( "      NO PROMPTS" );
               }
               else
               {
                  //---- merging prompt with shell and verticals tokens ---
		  
		           sql      =  "select prompt from prompts2 where id=" + prompts[k];
                  String   prompt   =  DB.getValue( sql, true ).trim();
                  if( prompt.equals("") )
                  {
                     System.out.println( "      EMPTY PROMPTS" );
                     continue;
                  }
                  prompt   =  prompt.replace( "{CurrentYear}",       Format.today().split("-")[0]  );
                  prompt   =  prompt.replace( "{Domain Name}",       getDomainName(    domainId)   );
                  prompt   =  prompt.replace( "{Domain Nickname}",   Format.getHashValue(domainTokens, "Domain Nickname")   );
                  prompt   =  prompt.replace( "{Shell}",             shellName                     );
                  prompt   =  prompt.replace( "{Vertical}",          vLabel                        );
                  prompt   =  prompt.replace( "{Sub Vertical}",      subVLabel                     );

                  //---- merging prompt with domain ---
                  Iterator it = domainTokens.keySet().iterator();
                  while( it.hasNext() )
                  {
                     String   ky       =  it.next().toString();
                     String   vl       =  Format.getHashValue( domainTokens, ky );
                  }
                  //---- main logic ---
                  if( k<alSub.size() )
                  {
                     HashMap  actualNode  =  new HashMap();
                     try {    actualNode  =  (HashMap) alSub.get( k ); } catch( Exception e ) { e.printStackTrace(); }
                     String   id          =  Format.getHashValue( actualNode, "id"   );
                     String   type        =  Format.getHashValue( actualNode, "type" );
                     boolean  isRun       =  isRun( queueType, rule, domainId, id, type );
                     if( hmCompleted.get(id) == null )
                     {
                        if( k < alSub.size() )
                        {
                           //---- get the node involved ---
                           if( isRun )
                           {
                              //---- merging prompt with data ---
                              for( int n=0; n<alSub.size(); n++ )
                              {
                                 HashMap  hm       =  (HashMap) alSub.get(n);
                                 String   lId      =  Format.getHashValue( hm, "id"          );
                                 String   lType    =  Format.getHashValue( hm, "leveltype"   );
                                 String   lbl      =  Format.getHashValue( hm, "label"       );
                                          prompt   =  prompt.replace( "{" + lType + "}", lbl );
                              }

                              //---- execute prompt on openAI  ---
                              HashMap  hmResp            =  new HashMap();
                              String   model             =  "";
                              String   prompt_tokens     =  "";
                              String   completion_tokens =  "";
                              String   timing            =  "";
                              String   content           =  "";
                              if( !queueType.equals("inventory") || (queueType.equals("inventory") && j==0) )
                              {
                                 prompt            =  prompt.replaceAll("\\{.*?\\}", "");
                                 hmResp            =  OpenAI.call( prompt, apiKey );
                                 model             =  Format.getHashValue( hmResp, "model"               ); // "model";
                                 prompt_tokens     =  Format.getHashValue( hmResp, "prompt_tokens"       ); // "10";
                                 completion_tokens =  Format.getHashValue( hmResp, "completion_tokens"   ); // "5";
                                 timing            =  Format.getHashValue( hmResp, "timing"              ); // "1000";
                                 content           =  Format.getHashValue( hmResp, "content"             ); // "content";
                              }

                              //-----------------
                              //---- Content ----
                              //-----------------
                              if( queueType.equals("content") )
                              {
                                 String   html     =  Format.convertMD( content );
                                 String   search   =  Html.text(      content.replace("\"", "") );
                                          sql      =  "insert into content2 ( node_id, domain_id, node_type, prompt, content, raw, search, model, prompt_tokens, completion_tokens, timing) ";
                                          sql     +=  "values (" + id + "," + domainId + ",'" + type + "',\"" + prompt.replace("\"", "\\\"") + "\",\"" + html.replace("\"", "\\\"") + "\", \"" + content.replace("\"", "\\\"") + "\", \"" + search + "\", '" + model + "'," + prompt_tokens + "," + completion_tokens + "," + timing + ")";
                                 if( isRun )
                                    DB.executeSQL( "delete from content2 where node_id=" + id + " and domain_id=" + domainId + " and node_type='" + type + "'" );
                                 DB.executeSQL( sql );
                              }

                              //----------------
                              //---- Tokens ----
                              //----------------
                              if( queueType.equals("tokens") )
                              {
                                 String   html     =  Format.convertMD( content );
                                          html     =  Html.text(      html.replace("\"", "") ).trim();
                                 try {    html     =  html.substring( 0, 4096 ); } catch( Exception e ) {}
                                          sql      =  "insert into node_tokens ( node_id, domain_id, node_type, prompt, token_name, value, model, prompt_tokens, completion_tokens, timing) ";
                                          sql     +=  "values (" + id + "," + domainId + ",'" + type + "',\"" + prompt.replace("\"", "\\\"") + "\",'" + queueSubType + "', \"" + html.replace("\"", "") + "\", '" + model + "'," + prompt_tokens + "," + completion_tokens + "," + timing + ")";
                                 if( isRun )
                                    DB.executeSQL( "delete from node_tokens where token_name='" + queueSubType + "' and node_id=" + id + " and domain_id=" + domainId + " and node_type='" + type + "'" );
                                 DB.executeSQL( sql );
                              }

                              //------------------
                              //---- Keywords ----
                              //------------------
                              if( queueSubType.equals("keywords") )
                              {
                                 String   rows[]   =  Format.packArray( content.split( "\n" ) );
                                 String   keywords =  "";
                                 for( int n=0; n<rows.length; n++ )
                                    keywords += ",\"" + rows[n] + "\"";
                                 try { keywords = keywords.substring(1); } catch( Exception e ) { e.printStackTrace(); }
                                 keywords = "[" + keywords + "]";
                                 sql      =  "insert into node_keywords (  node_id,    domain_id,        node_type,        prompt, keywords) ";
                                 sql     +=  "values (" +                  id + "," +  domainId + ",\"" + type + "\",\"" +   prompt.replace("\"", "\\\"") + "\",\"" + keywords.replace("\"", "\\\"") + "\")";
                                 if( isRun )
                                    DB.executeSQL( "delete from node_keywords where node_id=" + id + " and domain_id=" + domainId + " and node_type='" + type + "'" );
                                 DB.executeSQL( sql );
                              }

                              //----------------
                              //---- SEO    ----
                              //----------------
                              if( queueType.equals("seo") )
                              {
                                 String   html     =  Format.convertMD( content );
                                          html     =  Html.text(      html.replace("\"", "") ).trim();
                                 try {    html     =  html.substring( 0, 250 ); } catch( Exception e ) { e.printStackTrace(); }
                                          sql      =  "insert into node_seo ( node_id, domain_id, node_type, prompt, tag, value, model, prompt_tokens, completion_tokens, timing) ";
                                          sql     +=  "values (" + id + "," + domainId + ",'" + type + "',\"" + prompt.replace("\"", "\\\"") + "\",'" + queueSubType + "', \"" + html.replace("\"", "") + "\", '" + model + "'," + prompt_tokens + "," + completion_tokens + "," + timing + ")";
                                 if( isRun )
                                    DB.executeSQL( "delete from node_seo where tag='" + queueSubType + "' and node_id=" + id + " and domain_id=" + domainId + " and node_type='" + type + "'" );
                                 DB.executeSQL( sql );
                              }

                              //----------------
                              //---- AFS    ----
                              //----------------
                              if( queueType.equals("afs") )
                              {
                                 String   json     =  Format.lines2Json(content);
                                          sql      =  "insert into node_afs ( node_id, domain_id, node_type, prompt, options, value, model, prompt_tokens, completion_tokens, timing) ";
                                          sql     +=  "values (" + id + "," + domainId + ",'" + type + "',\"" + prompt.replace("\"", "\\\"") + "\",'" + queueSubType + "', \"" + json.replace("\"", "\\\"") + "\", '" + model + "'," + prompt_tokens + "," + completion_tokens + "," + timing + ")";
                                 if( isRun )
                                    DB.executeSQL( "delete from node_afs where options='" + queueSubType + "' and node_id=" + id + " and domain_id=" + domainId + " and node_type='" + type + "'" );
                                 try
                                 {
                                    DB.executeSQL( sql );
                                 }
                                 catch( Exception e )
                                 {
                                    e.printStackTrace();
                                 }
                              }

                              //-------------------
                              //---- INVENTORY ----
                              //-------------------
                              if( queueType.equals("inventory") && queueSubType.equals("inventory") )
                              {
                                 int level = DB.getIntValue( "select level from domain_nodes where id=" + nodeId + " union select level from shell_nodes where id=" + nodeId );
                                 if( level < alLevels.size() )
                                 {
                                    HashMap  hmLevelType    =  (HashMap) alLevels.get(level);
                                    String   nextLevelType  =  Format.getHashValue( hmLevelType, "type" );
                                    if( nextLevelType.equals("inventory") )
                                    {
                                       String   split[]     =  content.replace("\r", "").split("\n");
                                                sql         =  "delete from domain_nodes where parent_id=" + nodeId + " and domain_id=" + domainId;
                                       String   masterQuery =  "CALL insert_nodes(   '{  \"node_type\" :  \"domain\" }','[";
                                       
                                       DB.executeSQL( sql );
                                       for( int n=0; n<split.length; n++ )
                                       {
                                          String   label          =  split[n].trim().replace( "\"", "" );
                                          String   uri            =  Format.hyphenize( label );
                                          String   entId          =  DB.getValue( "select id from domain_entities where domain_id=" + domainId + " and uri='" + uri + "'" );
                                                   entId          =  entId.equals("")?(DB.executeSQL("insert into domain_entities (label, uri, domain_id) values ( \"" + label + "\", \"" + uri + "\", " + domainId + " )")):entId;
                                                   masterQuery   +=  n==0?"":",";
                                                   masterQuery   +=  "{\"parent_id\"      :  "    +  nodeId   +  ",";
                                                   masterQuery   +=     "\"entity_id\"    :  "    +  entId    +  ",";
                                                   masterQuery   +=     "\"domain_id\"    :  "    +  domainId +  ",";
                                                   masterQuery   +=     "\"shell_id\"     :  "    +  shellId  +  ",";
                                                   masterQuery   +=     "\"level\"        :  "    +  level    +  ",";
                                                   masterQuery   +=     "\"tokens\"       :  [],";
                                                   masterQuery   +=     "\"ebook_prompt\" :  \"\",";
                                                   masterQuery   +=     "\"taglines\"     :  \"\",";
                                                   masterQuery   +=     "\"active\"       :  1,";
                                                   masterQuery   +=     "\"freepik_id\"   :  0,";
                                                   masterQuery   +=     "\"source\"       :  \"\",";
                                                   masterQuery   +=     "\"sequence_id\"  :  1,";
                                                   masterQuery   +=     "\"parent_type\"  :  \""  + levelType + "\",";
                                                   masterQuery   +=     "\"deleted\"      :  0";
                                                   masterQuery   +=  "}";
                                       }
                                       masterQuery  += "]')";
                                       DB.executeSQL( masterQuery );
                                    }
                                 }
                              }

                              //-----------------------------------------
                              //---- EBOOK  OUTLINE PROMPT GENERATOR ----
                              //-----------------------------------------
                              if( queueType.equals("ebook") && queueSubType.equals("outline-prompt-generator") )
                              {
                                 DB.executeSQL( "delete from content2 where node_id in (select id        from domain_nodes where parent_id=" + nodeId + ") and node_type='domain' and domain_id=" + domainId );
                                 DB.executeSQL( "delete from domain_entities where id in      (select entity_id from domain_nodes where parent_id=" + nodeId + ")" );
                                 DB.executeSQL( "delete from domain_nodes    where parent_id=" + nodeId );
                                          content     =  content.replace( "\"",     ""    );
                                          content     =  content.replace( "#### ",  "- "  );
                                 int      level       =  DB.getIntValue( "select level from domain_nodes where id=" + nodeId + " union select level from shell_nodes where id=" + nodeId ) + 1;
                                 String   split[]     =  content.split( "\n" );
                                 for( int n=0; n<split.length; n++ )
                                 {
                                    if( split[n].trim().startsWith("## ") )
                                    {
                                       //--- Part 1 : Create Inventory ---
                                       String   outline     =  getOutline( split, split[n].trim() );
                                       String   title       =  split[n].substring( 3 );
                                       String   uri         =  Format.hyphenize( title );
                                       String   entId       =  DB.getValue( "select id from domain_entities where domain_id=" + domainId + " and uri='" + uri + "'" );
                                                entId       =  entId.equals("")?(DB.executeSQL("insert into domain_entities (label, uri, domain_id) values ( \"" + title + "\", \"" + uri + "\", " + domainId + " )")):entId;
                                                sql         =  "insert into domain_nodes    (level, entity_id, domain_id, parent_id, shell_id, parent_type ) values (";
                                                sql        +=  level + "," + entId + "," + domainId + "," + nodeId + "," + shellId + ",'" + levelType + "')";
                                       String   childNodeId =  DB.executeSQL( sql );

                                       //--- Part 2 : Create Content ---
                                       prompt       = "Here is the outline that was created which should produce a 10,000 total word e-Book. We are going to write each section individually with the goal , when assembled, reaching 10,000 total words.\n\n";
                                       prompt      += "--------------------------------------------------------------\n\n";
                                       prompt      += content + "\n\n";

                                       prompt      += "Follow these guidelines:\n";
                                       prompt      += "\n";

                                       prompt += "\n";
                                       prompt += "\nDo not use generic section labels such as introduction, conclusion, or final thoughts.";
                                       prompt += "\n";
                                       prompt += "\nDo not create fictional locations, businesses, or scenarios.";
                                       prompt += "\n";
                                       prompt += "\nEnsure a clear and well-structured outline with sufficient detail to guide the writing process.";
                                       prompt += "\n";
                                       prompt += "\nUse bold and underlined text for emphasis when necessary.";
                                       prompt += "\n";
                                       prompt += "\nIncorporate tables sparingly to organize information concisely and effectively.";
                                       prompt += "\n";
                                       prompt += "\nDo not include any additional prompt guidelines or instructions on the output-only provide the required output.";

                                       prompt += "\n\nPlease now write this section\n\n";

                                       prompt += "\"";
                                       prompt += title;
                                       prompt += "\"";

                                       //--- Part 2 ---
                                                hmResp   =  OpenAI.call( prompt, apiKey );
                                                content  =  Format.getHashValue( hmResp, "content" );
                                       String   html     =  Format.convertMD( content );
                                       String   search   =  Html.text(      content.replace("\"", "") );
                                                sql      =  "insert into content2 (node_id,        node_type,       outline,             prompt,                                  content,                                raw,                                       search,            model,         prompt_tokens,        completion_tokens,        timing,        domain_id) ";
                                                sql     +=  "values (" +           childNodeId + ", 'domain', \"" + outline + "\", \"" + prompt.replace("\"", "\\\"") + "\",\"" + html.replace("\"", "\\\"") + "\", \"" + content.replace("\"", "\\\"") + "\", \"" + search + "\", '" + model + "'," + prompt_tokens + "," + completion_tokens + "," + timing + "," + domainId + ")";
                                       DB.executeSQL( sql );
                                    }
                                 }
                              }
                              hmCompleted.put( id, "" );
                           }
                           else
                           {
                              //System.out.print( "-" );
                           }
                        }
                     }
                     else
                     {
                        //System.out.print( "!" );
                     }
                  }
               }
            }
         }
         sql = "delete from multi_queue2 where id=" + queueId;
//       System.out.println( sql );
         DB.executeSQL( sql );
      }
   }

   /*-------------------------------------------------------------------------*/
   /*--- isRun ...                                                         ---*/
   /*-------------------------------------------------------------------------*/
   public static boolean isRun( String queueType, String rule, String domainId, String nodeId, String nodeType )
   {
      if( rule.equals("runall") )
         return true;
      if( queueType.equals("Content") )
         return ( DB.getValue("select count(*) from content2 where domain_id=" + domainId + " and node_id=" + nodeId + " and node_type='" + nodeType + "'" ).equals("0") );
      return true;
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
   /*--- getDomainName ...                                                 ---*/
   /*-------------------------------------------------------------------------*/
   public static String getDomainName( String domainId )
   {
      return DB.getValue( "select name from domains where id=" + domainId, true );
   }

   /*-------------------------------------------------------------------------*/
   /*--- getDomainNickname ...                                             ---*/
   /*-------------------------------------------------------------------------*/
   public static String getDomainNickname( String domainId )
   {
      return DB.getValue( "select nickname from domains where id=" + domainId, true );
   }
   
   
   /*-------------------------------------------------------------------------*/
   /*--- getLevel ...                                                      ---*/
   /*-------------------------------------------------------------------------*/
   public static int getLevel( String levelType, String nodeId )
   {
      try
      {
         String sql = "";
         if( levelType.equals("shell") )
            sql = "select level from shell_nodes  where id=" + nodeId;
         else
            sql = "select level from domain_nodes where id=" + nodeId;
         return Integer.parseInt( DB.getValue( sql, true ) );
      }
      catch( Exception e )
      {
         e.printStackTrace();
         return 0;
      }
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
   /*--- chopPrompt ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   static ArrayList chopPrompt( String prompt )
   {
      String      split[]  =  (prompt+"\n###").split( "\n" );
      ArrayList   out      =  new ArrayList();
      String      html     =  "";
      for( int i=0; i<split.length; i++ )
      {
         split[i] = split[i].trim();
         if( split[i].startsWith("## ") )
         {
            if( !html.trim().equals("") )
               out.add( html );
            html = split[i] + "\n";
         }
         else
         {
            if( !split[i].trim().replace("#","").equals("") )
            html += split[i] + "\"\n";
         }
      }
      out.add( html );
      String out0 =  out.get(0).toString();
      String out1 =  out.get(1).toString();
             out1 =  out0 + out1;
      out.set( 1, out1 );
      out.remove(0);
      return   out;
   }

   /*-------------------------------------------------------------------------*/
   /*--- extractH2 ...                                                     ---*/
   /*-------------------------------------------------------------------------*/
   static String extractH2( String content )
   {
      String split[] =  content.split( "\n" );
      String h2      =  "";
      for( int i=0; i<split.length; i++ )
      {
         if( split[i].startsWith("## ") )
         {
            h2 = split[i].substring(3).replace("\"", "").trim();
            break;
         }
      }
      return h2;
   }

   /*-------------------------------------------------------------------------*/
   /*--- getOutline ...                                                    ---*/
   /*-------------------------------------------------------------------------*/
   static String getOutline( String content[], String title )
   {
      boolean  found2   =  false;
      boolean  found3   =  false;
      String   outline  =  "";
      for( int i=0; i<content.length; i++ )
      {
         if( content[i].trim().equals(title) )
            found2 = true;
         if( found2 && content[i].trim().startsWith("### ") )
         {
            outline +=  content[i].replace("\"", "") + "\n";
            found3   =  true;
         }
         if( found3 && !content[i].trim().startsWith("### ") )
            break;
      }
      return outline;
   }

   /*-------------------------------------------------------------------------*/
   /*--- exitProcess ...                                                   ---*/
   /*-------------------------------------------------------------------------*/
   static boolean exitProcess( String processNumber )
   {
      try
      {
         boolean isExit = !DB.getValue( "select record from zap where record=" + processNumber ).equals("");
         if( isExit )
            DB.executeSQL( "delete from zap where record=" + processNumber );
         return isExit;
      }
      catch( Exception e )
      {
         return false;
      }
   }
}

