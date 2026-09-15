package edu.seu.vcampus.server.module.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Access-backed holding-location dictionary. */
final class AccessBookLocationRepository implements BookLocationRepository {

    private final AccessLibraryStore store;

    AccessBookLocationRepository(AccessLibraryStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public List<String> findAll() {
        return store.read("Cannot list library locations.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT locationName FROM tblBookLocation ORDER BY sortOrder, locationName");
                 ResultSet result = statement.executeQuery()) {
                List<String> locations = new ArrayList<>();
                while (result.next()) {
                    locations.add(result.getString("locationName"));
                }
                return locations;
            }
        });
    }

    @Override
    public boolean exists(String locationName) {
        return store.read("Cannot inspect library location.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT locationName FROM tblBookLocation WHERE locationName = ?")) {
                statement.setString(1, locationName);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    @Override
    public void save(String locationName) {
        Objects.requireNonNull(locationName, "locationName must not be null");
        store.write("Cannot insert library location.", connection -> {
            String sql = "INSERT INTO tblBookLocation (locationName, sortOrder) "
                    + "VALUES (?, (SELECT COUNT(*) FROM tblBookLocation) + 1)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, locationName);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Library location was not inserted.");
                }
            }
        });
    }
}
