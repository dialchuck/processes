import java.util.*;
import java.io.*;

public class MainProcess
{
   public static void main( String args[] ) throws Exception
   {
      String      base        =  "";
      String      nProcess    =  "";
      try   {     nProcess    =  args[0]; } catch( Exception e ) {}
      try   {     base        =  args[1]; } catch( Exception e ) {}
      Multi.run( base, nProcess );
   }
}
