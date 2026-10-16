package com.starktech;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Storage {
    private static final String EXTENSION = ".properties";

    private final Path dataFolder;

    public Storage(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void save(String folder, String id, Properties data) {
        try {
            Path directory = dataFolder.resolve(folder);
            Files.createDirectories(directory);

            Path file = directory.resolve(id + EXTENSION);

            try (OutputStream out = Files.newOutputStream(file)) {
                data.store(out, "Stark Tech data");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save data", e);
        }
    }

    public Properties load(String folder, String id) {
        Path file = dataFolder.resolve(folder).resolve(id + EXTENSION);

        if (!Files.exists(file)) {
            return null;
        }

        Properties data = new Properties();

        try (InputStream in = Files.newInputStream(file)) {
            data.load(in);
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Could not load data", e);
        }
    }

    public List<String> list(String folder) {
        Path directory = dataFolder.resolve(folder);

        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files
                .map(file -> file.getFileName().toString())
                .filter(name -> name.endsWith(EXTENSION))
                .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Could not list data", e);
        }
    }

    public void delete(String folder, String id) {
        Path file = dataFolder.resolve(folder).resolve(id + EXTENSION);

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete data", e);
        }
    }
}
