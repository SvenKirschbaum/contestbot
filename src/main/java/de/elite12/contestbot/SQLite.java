package de.elite12.contestbot;

import java.sql.Statement;

import org.apache.log4j.Logger;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

public class SQLite {

	private Connection con;
	private static Logger logger = Logger.getLogger(SQLite.class);

	public SQLite() {

		try {
			Class.forName("org.sqlite.JDBC");

			this.con = DriverManager.getConnection("jdbc:sqlite:points.db");

			Statement stmnt = this.con.createStatement();
			stmnt.executeUpdate(
					"CREATE TABLE IF NOT EXISTS points(name VARCHAR(60) NOT NULL, points INTEGER NOT NULL, PRIMARY KEY(name));");
			stmnt.close();
		} catch (ClassNotFoundException | SQLException e) {
			logger.fatal("Could not initialize Database", e);
			System.exit(1);
		}

	}

	public void addPoints(String user, int points) throws SQLException {
		PreparedStatement stmnt = this.con
				.prepareStatement("UPDATE points SET points = points + ? WHERE name = ?");
		stmnt.setInt(1, points);
		stmnt.setString(2, user);
		int rows = stmnt.executeUpdate();
		stmnt.close();

		if (rows == 0) {
			PreparedStatement stmnt2 = this.con.prepareStatement("INSERT INTO points (name, points) VALUES (?, ?)");
			stmnt2.setString(1, user);
			stmnt2.setInt(2, points);
			stmnt2.executeUpdate();
			stmnt2.close();
		}
	}

	public int getPoints(String name) throws SQLException {
		PreparedStatement stmnt = this.con.prepareStatement("SELECT points.points FROM points WHERE points.name = ?");
		stmnt.setString(1, name);
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

}
