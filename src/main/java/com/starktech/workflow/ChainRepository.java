package com.starktech.workflow;

import com.starktech.Storage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ChainRepository {
    private static final String FOLDER = "chains";

    private final Storage storage;

    public ChainRepository(Path dataFolder) {
        storage = new Storage(dataFolder);
    }

    public void save(ChainRecord chain) {
        Properties data = new Properties();

        data.setProperty("id", chain.getId());
        data.setProperty("status", chain.getStatus().name());
        data.setProperty("detail", chain.getDetail());

        storage.save(FOLDER, chain.getId(), data);
    }

    public ChainRecord find(String id) {
        Properties data = storage.load(FOLDER, id);

        if (data == null) {
            return null;
        }

        return new ChainRecord(
            data.getProperty("id"),
            ChainRecord.Status.valueOf(data.getProperty("status")),
            data.getProperty("detail")
        );
    }

    public List<ChainRecord> findAll() {
        List<ChainRecord> chains = new ArrayList<>();

        for (String id : storage.list(FOLDER)) {
            chains.add(find(id));
        }

        return chains;
    }
}
