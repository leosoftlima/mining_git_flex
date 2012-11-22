import java.io.*;
import java.util.*;

public class CompareTwoFiles {
  public static void main(String[] args) throws java.io.IOException {

/*    BufferedReader bfr2 = new BufferedReader(new InputStreamReader(
        System.in));
    System.out.println("Enter File name:");
    String str = bfr2.readLine();
    System.out.println("Enter another file name:");
    String str1 = bfr2.readLine();*/
	  
	File file1 = new File("Member.groovy");
	File fileId = new File ("Member1.groovy");
	File fileDif = new File ("Mod.groovy");

    String s1 = "";
    String s2 = "", s3 = "", s4 = "";
    String y = "", z = "";

    BufferedReader bfr = new BufferedReader(new FileReader(file1));
    BufferedReader bfr1 = new BufferedReader(new FileReader(fileDif));

    while ((z = bfr1.readLine()) != null)
      s3 += z;

    while ((y = bfr.readLine()) != null)
      s1 += y;

/*    System.out.println();

    System.out.println(s3);*/

    if (s3.equals(s1)) {
      System.out.println("Content of both files are same");
    } else {

      System.out.println("Content of both files are not same");
    }
  }
}