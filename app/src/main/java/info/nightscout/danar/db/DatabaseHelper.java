package info.nightscout.danar.db;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
	private static Logger log = LoggerFactory.getLogger(DatabaseHelper.class);

	public static final String DATABASE_NAME = "DanaAppDb";

	private static final int DATABASE_VERSION = 1;

	public DatabaseHelper(Context context) {

		super(context, DATABASE_NAME, null, DATABASE_VERSION);
//		try {
//            TableUtils.dropTable(connectionSource, HistoryRecord.class, true);
//            TableUtils.createTable(connectionSource, HistoryRecord.class);
//		} catch (SQLException e) {
//		}
	}


    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
		try {
			log.info("onCreate");
			TableUtils.createTable(connectionSource, PumpStatus.class);
            TableUtils.createTableIfNotExists(connectionSource, HistoryRecord.class);
		} catch (SQLException e) {
			log.error(DatabaseHelper.class.getName(), "Can't create database", e);
			throw new RuntimeException(e);
		}
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			log.info(DatabaseHelper.class.getName(), "onUpgrade");
			TableUtils.dropTable(connectionSource, PumpStatus.class, true);
			TableUtils.dropTable(connectionSource, HistoryRecord.class, true);
			onCreate(database, connectionSource);
		} catch (SQLException e) {
			log.error(DatabaseHelper.class.getName(), "Can't drop databases", e);
			throw new RuntimeException(e);
		}
    }

    /**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
	}

     public Dao<PumpStatus, Long> getDaoPumpStatus() throws SQLException {
//        getDao(PumpStatus.class).executeRaw("ALTER TABLE 'PumpStatus' ADD COLUMN 'tempBasalStart' VARCHAR;");
        return  getDao(PumpStatus.class);
    }

    public Dao<HistoryRecord, String> getDaoHistoryRecords() throws SQLException {
        return  getDao(HistoryRecord.class);
    }

}
