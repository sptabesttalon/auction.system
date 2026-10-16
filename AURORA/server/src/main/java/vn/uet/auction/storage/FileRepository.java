package vn.uet.auction.storage;

import java.io.*;
import java.nio.file.*;

/** Durable server-only storage via Java Object Serialization and atomic replacement. */
public final class FileRepository implements Repository {
    private final Path path;
    public FileRepository(Path path) { this.path = path.toAbsolutePath(); }

    @Override public State load() throws Exception {
        if (Files.notExists(path)) return new State();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            Object data = in.readObject();
            if (!(data instanceof State state)) throw new IOException("Invalid storage format");
            return state;
        }
    }

    @Override public void save(State state) throws Exception {
        Path parent = path.getParent(); Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, ".auction-", ".tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) { out.writeObject(state); }
            try { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
