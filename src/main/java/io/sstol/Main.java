package io.sstol;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Main {
   public static void main(String[] args) {
      String zipFile = "src/main/resources/1.zip";
      Predicate<Path> pathPredicate = path -> path.toString().endsWith(".json");
      Map<Path, Object> filesFromZipArchive = getFilesFromZipArchive(zipFile, pathPredicate);
      filesFromZipArchive.entrySet().forEach(System.out::println);
   }

   static Map<Path, Object> getFilesFromZipArchive(String zipFile, Predicate<Path> filter) {
      Map<Path, Object> filesFromZipArchive = new HashMap<>();
      ObjectMapper objectMapper = new ObjectMapper();
      getFilesReaderAndPath(zipFile, filter, (reader, path) -> {
         try {
            filesFromZipArchive.put(path, objectMapper.readValue(reader, Object.class));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      });
      return filesFromZipArchive;
   }

   static void getFilesReaderAndPath(String zipFile, Predicate<Path> filter, BiConsumer<Reader, Path> consumer) {
      consumeFilesIntoZipArchive(zipFile, filter,
              path -> {
                 try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    consumer.accept(reader, path);
                 } catch (IOException e) {
                    throw new RuntimeException(e);
                 }
              });
   }

   static void consumeFilesIntoZipArchive(String zipFile, Predicate<Path> filter, Consumer<Path> consumer) {
      consumeZip(Path.of(zipFile), true, fs -> {
         try (Stream<Path> paths = Files.walk(fs.getPath("/"))) {
            paths.filter(path -> !path.equals(fs.getPath("/")))
                    .filter(filter)
                    .forEach(consumer);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      });
   }

   static void consumeZip(Path zipFile, boolean createNewIfNotExists, Consumer<FileSystem> fileSystemConsumer) {
      Map<String, Object> env = new HashMap<>();
      env.put("create", String.valueOf(createNewIfNotExists));
      env.put("useTempFile", Boolean.TRUE);
      try (FileSystem fs = FileSystems.newFileSystem(zipFile, env)) {
         fileSystemConsumer.accept(fs);
      } catch (IOException ie) {
         throw new RuntimeException(String.format("Cannot consume zipfs: %s", zipFile), ie);
      }
   }
}