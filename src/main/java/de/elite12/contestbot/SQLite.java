/*******************************************************************************
 * Copyright (C) 2017 Sven Kirschbaum
 * Copyright (C) 2017 Markus Licht
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.elite12.contestbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import de.elite12.contestbot.Model.Leaderboard;

public class SQLite {
    
    private static final String CREATE_TABLE_STRING = "CREATE TABLE IF NOT EXISTS points(name VARCHAR(60) NOT NULL, points INTEGER NOT NULL, PRIMARY KEY(name));";
    private static final String CREATE_TABLE_STRING2 = "CREATE TABLE IF NOT EXISTS counter(name VARCHAR(60) NOT NULL, points INTEGER NOT NULL, PRIMARY KEY(name));";
    protected Connection con;
    private static Logger logger = Logger.getLogger(SQLite.class);
    private static SQLite instance = null;
    
    private SQLite() {
        this("jdbc:sqlite:points.db");
    }
    
    protected SQLite(String jdbc) {
        
        try {
            Class.forName("org.sqlite.JDBC");
            
            this.con = DriverManager.getConnection(jdbc);
            
            Statement stmnt = this.con.createStatement();
            stmnt.executeUpdate(CREATE_TABLE_STRING);
            stmnt.executeUpdate(CREATE_TABLE_STRING2);
            stmnt.close();
        } catch (ClassNotFoundException | SQLException e) {
            logger.fatal("Could not initialize Database", e);
            System.exit(1);
        }
        
    }
    
    public void changePoints(String user, int points) throws SQLException {
        PreparedStatement stmnt = this.con.prepareStatement("UPDATE points SET points = points + ? WHERE name = ?");
        stmnt.setInt(1, points);
        stmnt.setString(2, user.toLowerCase());
        int rows = stmnt.executeUpdate();
        stmnt.close();
        
        if (rows == 0) {
            PreparedStatement stmnt2 = this.con.prepareStatement("INSERT INTO points (name, points) VALUES (?, ?)");
            stmnt2.setString(1, user.toLowerCase());
            stmnt2.setInt(2, points);
            stmnt2.executeUpdate();
            stmnt2.close();
        }
    }
    
    public int getPoints(String name) throws SQLException {
        PreparedStatement stmnt = this.con.prepareStatement("SELECT points FROM points WHERE name = ?");
        stmnt.setString(1, name.toLowerCase());
        ResultSet rs = stmnt.executeQuery();
        boolean exists = rs.next();
        int ret = 0;
        if (exists) {
            ret = rs.getInt(1);
        }
        rs.close();
        stmnt.close();
        return ret;
    }
    
    public Leaderboard getLeaderboard(int count) throws SQLException {
        PreparedStatement stmnt = this.con
                .prepareStatement("SELECT name,points FROM points WHERE name != ? ORDER BY points DESC LIMIT ?");
        stmnt.setString(1, "!ripdevil");
        stmnt.setInt(2, count);
        ResultSet rs = stmnt.executeQuery();
        
        ArrayList<String> usernames = new ArrayList<>();
        ArrayList<Integer> points = new ArrayList<>();
        
        while (rs.next()) {
            usernames.add(rs.getString("name"));
            points.add(rs.getInt("points"));
        }
        
        rs.close();
        stmnt.close();
        
        Leaderboard l = new Leaderboard();
        l.setPoints(points.toArray(new Integer[0]));
        l.setUsernames(usernames.toArray(new String[0]));
        return l;
    }

    public void changeCounter(String user, int points) throws SQLException {
        PreparedStatement stmnt = this.con.prepareStatement("UPDATE counter SET points = points + ? WHERE name = ?");
        stmnt.setInt(1, points);
        stmnt.setString(2, user.toLowerCase());
        int rows = stmnt.executeUpdate();
        stmnt.close();
        
        if (rows == 0) {
            PreparedStatement stmnt2 = this.con.prepareStatement("INSERT INTO counter (name, points) VALUES (?, ?)");
            stmnt2.setString(1, user.toLowerCase());
            stmnt2.setInt(2, points);
            stmnt2.executeUpdate();
            stmnt2.close();
        }
    }
    
    public int getCounter(String name) throws SQLException {
        PreparedStatement stmnt = this.con.prepareStatement("SELECT points FROM counter WHERE name = ?");
        stmnt.setString(1, name.toLowerCase());
        ResultSet rs = stmnt.executeQuery();
        boolean exists = rs.next();
        int ret = 0;
        if (exists) {
            ret = rs.getInt(1);
        }
        rs.close();
        stmnt.close();
        return ret;
    }
    
    public static SQLite getInstance() {
        if (SQLite.instance == null) {
            SQLite.instance = new SQLite();
        }
        return SQLite.instance;
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.con.close();
        super.finalize();
    }
    
    public void resetLeaderboard() throws SQLException {
        Statement stmnt = this.con.createStatement();
        stmnt.executeUpdate("DELETE FROM points");
        stmnt.close();
    }
}
