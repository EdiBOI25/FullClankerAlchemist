package fullclanker.alchemist.config;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import java.sql.SQLException;

/** SQLite dialect that writes multi-column unique constraints into CREATE TABLE. */
public class SqliteDialect extends SQLiteDialect {

	private final UniqueDelegate uniqueDelegate = new AlterTableUniqueDelegate(this) {

		@Override
		public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
			return " unique";
		}

		@Override
		public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
			StringBuilder fragment = new StringBuilder();
			for (UniqueKey key : table.getUniqueKeys().values()) {
				if (key.getColumnSpan() > 1) {
					fragment.append(", ").append(uniqueConstraintSql(key));
				}
			}
			return fragment.toString();
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(UniqueKey key, Metadata metadata, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(UniqueKey key, Metadata metadata, SqlStringGenerationContext context) {
			return "";
		}
	};

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> isConstraintViolation(sqlException)
				? new ConstraintViolationException(message, sqlException, sql)
				: null;
	}

	private boolean isConstraintViolation(SQLException exception) {
		for (SQLException current = exception; current != null; current = current.getNextException()) {
			if (current.getErrorCode() == 19) {
				return true;
			}
		}
		return false;
	}

}
