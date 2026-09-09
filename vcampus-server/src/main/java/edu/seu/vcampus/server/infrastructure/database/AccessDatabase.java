package edu.seu.vcampus.server.infrastructure.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Connection connection = DriverManager.getConnection(url);
        suppressMisleadingCursorWarnings();
        return connection;
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

    private void suppressMisleadingCursorWarnings() {
        // Opening the first connection can reset this UCanAccess logger. Configure it
        // afterwards: rejected WHERE candidates are normal, while real SQL failures
        // still surface as exceptions and SEVERE records.
        Logger logger = Logger.getLogger("net.ucanaccess.commands.AbstractCursorCommand");
        logger.setLevel(Level.SEVERE);
        logger.setFilter(record -> record.getLevel().intValue() >= Level.SEVERE.intValue());
    }
}
