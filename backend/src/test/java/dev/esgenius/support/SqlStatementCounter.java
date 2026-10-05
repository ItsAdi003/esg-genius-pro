package dev.esgenius.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * Counts SQL statements prepared on the current thread.
 * Registered only from tests via hibernate.session_factory.statement_inspector.
 */
public class SqlStatementCounter implements StatementInspector {

    private static final ThreadLocal<List<String>> STATEMENTS =
            ThreadLocal.withInitial(ArrayList::new);

    public SqlStatementCounter() {
    }

    public static void reset() {
        STATEMENTS.get().clear();
    }

    public static int count() {
        return STATEMENTS.get().size();
    }

    public static List<String> statements() {
        return List.copyOf(STATEMENTS.get());
    }

    @Override
    public String inspect(String sql) {
        STATEMENTS.get().add(sql);
        return sql;
    }
}
