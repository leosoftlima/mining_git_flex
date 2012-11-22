import java.io.*;
import java.net.*;
public class Downloader {

//it serves for files up to 8mb approximately
	
public static void main(String args[]) throws IOException
{
	
String end = "https://github.com/spgroup/rgms/archive/13341bf1c715cdf7781174c3ebde868c80f0f193.zip";
java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.net.URL(end).openStream());
java.io.FileOutputStream fos = new java.io.FileOutputStream("test.zip");
java.io.BufferedOutputStream bout = new BufferedOutputStream(fos,8192);
byte[] data = new byte[8192];
int x=0;
while((x=in.read(data,0,8192))>=0)
{
bout.write(data,0,x);
}
bout.close();
in.close();
}
}