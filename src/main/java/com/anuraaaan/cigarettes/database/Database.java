package com.anuraaaan.cigarettes.database;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.nicotine.WithdrawalData;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database {

    private final Cigarettes plugin;

    public Database(Cigarettes plugin) {
        this.plugin = plugin;
    }

    private Connection connection;

    public void connect() throws SQLException {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        connection = DriverManager.getConnection("jdbc:sqlite:"+ plugin.getDataFolder() +"/cigarettes.db");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_data (" +
                            "uuid TEXT PRIMARY KEY NOT NULL, " +
                            "nicotine REAL NOT NULL, " +
                            "tolerance REAL NOT NULL, " +
                            "addiction REAL NOT NULL, " +
                            "next_withdrawal_delay BIGINT NOT NULL, " +
                            "level INTEGER NOT NULL, " +
                            "remaining_time BIGINT NOT NULL)");
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void savePlayer(@NotNull UUID uuid, @NotNull NicotineData nicotineData, @NotNull WithdrawalData withdrawalData) throws SQLException {
        String sql = "INSERT INTO player_data(uuid, nicotine, tolerance, addiction, next_withdrawal_delay, level, remaining_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "nicotine = excluded.nicotine, " +
                "tolerance = excluded.tolerance, " +
                "addiction = excluded.addiction, " +
                "next_withdrawal_delay = excluded.next_withdrawal_delay, " +
                "level = excluded.level, " +
                "remaining_time = excluded.remaining_time";


        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setDouble(2, nicotineData.getNicotine());
            statement.setDouble(3, nicotineData.getTolerance());
            statement.setDouble(4, nicotineData.getAddiction());
            statement.setLong(5, withdrawalData.getNextWithdrawalDelay());
            statement.setInt(6, withdrawalData.getWithdrawalLevel());
            statement.setLong(7, withdrawalData.getRemainingTimeToWithdrawal());

            statement.executeUpdate();
        }
    }

    public List<PlayerData> loadPlayer() throws SQLException {

        List<PlayerData> players = new ArrayList<>();

        String sql = "SELECT uuid, nicotine, tolerance, addiction, next_withdrawal_delay, level, remaining_time FROM player_data";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                UUID uuid = UUID.fromString(result.getString("uuid"));

                NicotineData nicotineData = new NicotineData(result.getDouble("nicotine"),
                        result.getDouble("tolerance"), result.getDouble("addiction"));

                WithdrawalData withdrawalData = new WithdrawalData(0, result.getLong("next_withdrawal_delay"),
                        0, result.getInt("level"), result.getLong("remaining_time"));

                players.add(new PlayerData(uuid, nicotineData, withdrawalData));
            }
        }

        return players;
    }
}
