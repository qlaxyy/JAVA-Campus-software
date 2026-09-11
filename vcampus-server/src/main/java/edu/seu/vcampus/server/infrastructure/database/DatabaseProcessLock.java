package edu.seu.vcampus.server.infrastructure.database;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Prevents a running server and the destructive rebuild command from overlapping. */
public final class DatabaseProcessLock implements AutoCloseable {
    private final Path lockPath;
    private final FileChannel channel;
    private final FileLock lock;

    private DatabaseProcessLock(Path lockPath, FileChannel channel, FileLock lock) {
        this.lockPath = lockPath;
        this.channel = channel;
        this.lock = lock;
    }

    public static DatabaseProcessLock acquire(Path databasePath) throws IOException {
        Path normalized = databasePath.toAbsolutePath().normalize();
        Path lockPath = normalized.resolveSibling(normalized.getFileName() + ".server.lock");
        Files.createDirectories(lockPath.getParent());
        FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock;
        try {
            lock = channel.tryLock();
        } catch (RuntimeException | IOException exception) {
            channel.close();
            throw exception;
        }
        if (lock == null) {
            channel.close();
            throw new IllegalStateException("服务器仍在使用数据库，请先停止服务器再执行该操作。");
        }
        return new DatabaseProcessLock(lockPath, channel, lock);
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            channel.close();
            Files.deleteIfExists(lockPath);
        }
    }
}
