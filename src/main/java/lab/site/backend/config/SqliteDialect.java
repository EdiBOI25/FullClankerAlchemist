package lab.site.backend.config;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/** SQLite dialect that writes multi-column unique constraints into CREATE TABLE; the stock one silently drops them. */
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
	};

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

}
