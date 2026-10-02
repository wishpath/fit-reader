package org.sa;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class FileUtil {

  public static byte[] loadSingleFitFile() {
    try {
      File fitFile = getFitFile();
      System.out.println("Reading: " + fitFile.getName());
      return Files.readAllBytes(fitFile.toPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static File getFitFile() throws IOException, URISyntaxException {
    List<File> fitFiles = new ArrayList<>();

    Enumeration<URL> roots = Thread.currentThread()
        .getContextClassLoader()
        .getResources("");

    while (roots.hasMoreElements()) {
      URI rootUri = roots.nextElement().toURI();
      File rootDir = new File(rootUri);
      File[] matches = rootDir.listFiles((dir, name) -> name.endsWith(".fit"));
      if (matches != null) {
        fitFiles.addAll(java.util.Arrays.asList(matches));
      }
    }

    if (fitFiles.size() != 1) {
      throw new IllegalStateException("Expected exactly one .fit file in resources, found: " + fitFiles.size());
    }

    File fitFile = fitFiles.get(0);
    return fitFile;
  }

  public static File createEmptyOutputFile(String s) {
    File outputDir = new File("output");
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    File outputFile = new File(outputDir, s);
    return outputFile;
  }

  public static String getSingleFitFileName() {
    try {
      return getFitFile().getName();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}