//
// ReaderTest.java
//

/*
LOCI Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden, Chris Allan
and Eric Kjellman.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Library General Public License for more details.

You should have received a copy of the GNU Library General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.formats.test;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import junit.framework.*;
import loci.formats.*;

/**
 * JUnit tester for Bio-Formats file format readers.
 * Failed tests are written to a logfile, for easier processing.
 */
public class ReaderTest extends TestCase {

  // -- Fields --

  private static Vector badFiles;
  private static String currentFile;
  private static IFormatReader reader;
  private static FileWriter logFile;

  // -- Constructor --
 
  public ReaderTest(String s) {
    super(s);
  }

  // -- TestCase API methods --

  /** Release resources after tests have completed */
  protected void tearDown() {
    try {
      reader.close();
      badFiles = null;
      if (logFile != null) logFile.close();
    }
    catch (FormatException fe) { }
    catch (IOException io) { }
  }

  /** Make our results available to a TestRunner */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new ReaderTest("testBufferedImageDimensions"));
    suite.addTest(new ReaderTest("testByteArrayDimensions"));
    suite.addTest(new ReaderTest("testImageCount"));
    suite.addTest(new ReaderTest("testOMEXML"));
    return suite;
  }

  // -- ReaderTest API methods --

  /** Set the file to process. */
  public static void setFile(String file) {
    currentFile = file;
  }

  /** Determine if the given filename is a "bad" file. */
  public static boolean isBadFile(String file) {
    if (badFiles == null) {
      try {
        badFiles = new Vector();
        BufferedReader in = new BufferedReader(new FileReader("bad-files.txt"));
        while (true) {
          String line = in.readLine();
          if (line == null) break;
          badFiles.add(line);
        }
        in.close();
      }
      catch (IOException io) { }
    }
    if (badFiles.contains(file)) return true;
    for (int i=0; i<badFiles.size(); i++) {
      if (file.endsWith((String) badFiles.get(i))) return true;
    }
    return false; 
  }

  /** Reset the format reader. */
  public static void resetReader() {
    try {
      reader.close();
    }
    catch (Exception e) { }
    reader = new FileStitcher();
    OMEXMLMetadataStore store = new OMEXMLMetadataStore();
    store.createRoot();
    reader.setMetadataStore(store);
  }

  /** 
   * Returns true if any of the given file names are in the 
   * list of files to test. 
   */
  public static boolean isStitchedFile(Vector files, String[] related) {
    for (int i=0; i<related.length; i++) {
      if (files.contains(related[i])) return true;  
    }
    return false;
  }

  /** Recursively generate a list of files to test. */
  public static void getFiles(String root, Vector files) {
    if (reader == null) {
      reader = new FileStitcher();
      OMEXMLMetadataStore store = new OMEXMLMetadataStore();
      store.createRoot();
      reader.setMetadataStore(store);
    
      Date date = new Date();
      try {
        logFile = new FileWriter("bioformats-test-" + date.toString() + ".log");
        logFile.flush();
      }
      catch (IOException io) { }
    }
  
    if (!root.endsWith(File.separator)) root += File.separator;
    File f = new File(root);
    String[] subs = f.list();
    if (subs != null) {
      for (int i=0; i<subs.length; i++) {
        if (!isBadFile(subs[i])) {
          subs[i] = root + subs[i]; 
          File tmp = new File(subs[i]);
          if (!tmp.isDirectory() && reader.isThisType(subs[i])) {
            try {
              if (!isStitchedFile(files, reader.getUsedFiles(subs[i]))) {
                files.add(subs[i]);
                /* debug */ System.out.println("adding " + subs[i]);
              }
            }
            catch (IOException io) { files.add(subs[i]); }
            catch (FormatException fe) { files.add(subs[i]); }
          }
          else if (tmp.isDirectory()) getFiles(subs[i], files);
          else /* debug */ System.out.println(subs[i] + " has invalid type");
          tmp = null;
          try { reader.close(); }
          catch (Exception e) { }
        }
        else /* debug */ System.out.println(subs[i] + " is a bad file");
      }
    }
    else System.out.println("Invalid directory : " + root); 
    f = null;
  }

  // -- Testing methods --

  /** 
   * Check the SizeX and SizeY dimensions against the actual dimensions of
   * the BufferedImages.
   */
  public void testBufferedImageDimensions() {
   boolean success = true;
    try {
      for (int i=0; i<reader.getSeriesCount(currentFile); i++) {
        reader.setSeries(currentFile, i);
        int imageCount = reader.getImageCount(currentFile);
        int sizeX = reader.getSizeX(currentFile);
        int sizeY = reader.getSizeY(currentFile);
        for (int j=0; j<imageCount; j++) {
          BufferedImage b = reader.openImage(currentFile, j);
          if ((b.getWidth() != sizeX) || (b.getHeight() != sizeY)) {
            success = false;
            j = imageCount;
            i = reader.getSeriesCount(currentFile);
            break;
          }
        }
      }
    }
    catch (Exception e) {
      success = false; 
    }
    try {
      if (!success) {
        logFile.write(currentFile + " failed BufferedImage test\n");
        logFile.flush();
      }
    }
    catch (IOException io) { }
    assertTrue(success);
  }

  /**
   * Check the SizeX and SizeY dimensions against the actual dimensions of
   * the byte array returned by openBytes.
   */
  public void testByteArrayDimensions() {
    boolean success = true;
    try {
      for (int i=0; i<reader.getSeriesCount(currentFile); i++) {
        reader.setSeries(currentFile, i);
        int imageCount = reader.getImageCount(currentFile);
        int sizeX = reader.getSizeX(currentFile);
        int sizeY = reader.getSizeY(currentFile);
        int bytesPerPixel = 
          FormatReader.getBytesPerPixel(reader.getPixelType(currentFile));
        int sizeC = reader.getSizeC(currentFile);
        boolean rgb = reader.isRGB(currentFile);

        int expectedBytes = sizeX * sizeY * bytesPerPixel * (rgb ? sizeC : 1);

        for (int j=0; j<imageCount; j++) {
          byte[] b = reader.openBytes(currentFile, j);
          if (b.length != expectedBytes) {
            success = false; 
            j = imageCount;
            i = reader.getSeriesCount(currentFile);
            break;
          }
        }
      }
    }
    catch (Exception e) { 
      success = false; 
    }
    try {
      if (!success) {
        logFile.write(currentFile + " failed byte array test\n");
        logFile.flush();
      }
    }
    catch (IOException io) { }
    assertTrue(success);
  }

  /**
   * Check the SizeZ, SizeC, and SizeT dimensions against the 
   * total image count.
   */
  public void testImageCount() {
    boolean success = true;
    try {
      for (int i=0; i<reader.getSeriesCount(currentFile); i++) {
        reader.setSeries(currentFile, i);
        int imageCount = reader.getImageCount(currentFile);
        int sizeZ = reader.getSizeZ(currentFile);
        int sizeC = reader.getEffectiveSizeC(currentFile);
        int sizeT = reader.getSizeT(currentFile);
        success = imageCount == sizeZ * sizeC * sizeT;
        try {
          if (!success) {
            logFile.write(currentFile + " failed image count test\n");
            logFile.flush();
            assertTrue(false);
          }
        }
        catch (IOException io) { }
      }
      assertTrue(success);
    }
    catch (Exception e) { 
      try {
        logFile.write(currentFile + " failed image count test\n");
        logFile.flush();
      }
      catch (IOException io) { }
      assertTrue(false); 
    }
  }

  /**
   * Check that the OME-XML attribute values match the values of the core
   * metadata (Size*, DimensionOrder, etc.).
   */
  public void testOMEXML() {
    try {
      OMEXMLMetadataStore store = 
        (OMEXMLMetadataStore) reader.getMetadataStore(currentFile);
 
      boolean success = true;
      for (int i=0; i<reader.getSeries(currentFile); i++) {
        reader.setSeries(currentFile, i);
        int sizeX = reader.getSizeX(currentFile);
        int sizeY = reader.getSizeY(currentFile);
        int sizeZ = reader.getSizeZ(currentFile);
        int sizeC = reader.getSizeC(currentFile);
        int sizeT = reader.getSizeT(currentFile);
        boolean bigEndian = !reader.isLittleEndian(currentFile);
        String type = 
          FormatReader.getPixelTypeString(reader.getPixelType(currentFile));
        String dimensionOrder = reader.getDimensionOrder(currentFile);

        success =  
          (sizeX == store.getSizeX(null).intValue()) &&
          (sizeY == store.getSizeY(null).intValue()) &&
          (sizeZ == store.getSizeZ(null).intValue()) &&
          (sizeC == store.getSizeC(null).intValue()) &&
          (sizeT == store.getSizeT(null).intValue()) &&
          (bigEndian == store.getBigEndian(null).booleanValue()) &&
          type.toLowerCase().equals(store.getPixelType(null).toLowerCase()) &&
          dimensionOrder.equals(store.getDimensionOrder(null));
   
        if (!success) {
          try {
            logFile.write(currentFile + " failed OME-XML sanity test\n");
            logFile.flush();
            assertTrue(false);
          }
          catch (IOException io) { }
        }
      }
      assertTrue(success); 
    }
    catch (Exception e) { 
      try {
        logFile.write(currentFile + " failed OME-XML sanity test\n");
        logFile.flush();
      }
      catch (IOException io) { }
      assertTrue(false); 
    }
  }

  // -- Main method --

  public static void main(String[] args) {
    Vector files = new Vector();  
    ReaderTest.getFiles(args[0], files);
    System.out.println();
    for (int i=0; i<files.size(); i++) {
      System.out.println("Testing " + files.get(i));
      ReaderTest.setFile((String) files.get(i));
      TestResult result = new TestResult();
      ReaderTest.suite().run(result);
      int total = result.runCount();
      int failed = result.failureCount();
      float failPercent = (float) (100 * ((double) failed / (double) total));
      System.out.println(files.get(i) + " - " + failed + " failures in " +
        total + " tests (" + failPercent + "% failed)");
      ReaderTest.resetReader();
    }
  }

}
