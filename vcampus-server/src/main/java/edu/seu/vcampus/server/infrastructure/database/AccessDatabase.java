package edu.seu.vcampus.server.infrastructure.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/** Opens JDBC connections to the server-owned Microsoft Access database. */
public final class AccessDatabase {

    private static final String DRIVER_CLASS = "net.ucanaccess.jdbc.UcanaccessDriver";

    private final Path path;

    /** Prepares a database location. The file is created on first connection. */
    public AccessDatabase(Path path) {
        this.path = Objects.requireNonNull(path, "path must not be null")
                .toAbsolutePath()
                .normalize();
        prepareParentDirectory();
        loadDriver();
    }

    /** Opens a new connection. Callers must close it. */
    public Connection openConnection() throws SQLException {
        String url = "jdbc:ucanaccess://" + path + ";newDatabaseVersion=V2010";
        return DriverManager.getConnection(url);
    }

    /** Returns the normalized database file path. */
    public Path path() {
        return path;
    }

    private void prepareParentDirectory() {
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot create database directory: " + parent, exception);
        }
    }

    private void loadDriver() {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "UCanAccess JDBC driver is not available.", exception);
        }
    }
}
