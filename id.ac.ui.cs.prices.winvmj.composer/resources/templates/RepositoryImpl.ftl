package ${packageName};

import id.ac.ui.cs.prices.winvmj.hibernate.RepositoryUtil;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.persistence.PersistenceException;
import org.hibernate.Session;

public class ${entityName}RepositoryImpl<Y> extends RepositoryUtil<Y> {

    private static final String FEATURE_NAME = "${featureName}";
    private static final String TABLE_NAME = "${tableName}";
    private final LongCounter dbQueryCounter;
    private final LongCounter dbQueryErrorCounter;
    private final LongHistogram dbQueryDurationHistogram;

    public ${entityName}RepositoryImpl(Class<? extends Y> componentClass) {
        super(componentClass);
        Meter meter = GlobalOpenTelemetry.get().getMeter("monitoring");
        this.dbQueryCounter = meter.counterBuilder("db_queries_total")
            .setDescription("Total number of database queries").build();
        this.dbQueryErrorCounter = meter.counterBuilder("db_query_errors_total")
            .setDescription("Total number of failed database queries").build();
        this.dbQueryDurationHistogram = meter.histogramBuilder("db_query_duration_ms")
            .setDescription("Database query duration in milliseconds").ofLongs().build();
    }

    private Attributes attrs(String operation) {
        return Attributes.of(
            AttributeKey.stringKey("feature"), FEATURE_NAME,
            AttributeKey.stringKey("db.operation"), operation,
            AttributeKey.stringKey("db.table"), TABLE_NAME);
    }

    @Override
    public void saveObject(Y object) throws PersistenceException {
        long start = System.currentTimeMillis();
        try {
            super.saveObject(object);
            dbQueryCounter.add(1, attrs("INSERT"));
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("INSERT"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("INSERT"));
        }
    }

    @Override
    public void updateObject(Y object) {
        long start = System.currentTimeMillis();
        try {
            super.updateObject(object);
            dbQueryCounter.add(1, attrs("UPDATE"));
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("UPDATE"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("UPDATE"));
        }
    }

    @Override
    public Y getObject(int id) {
        long start = System.currentTimeMillis();
        try {
            Y result = super.getObject(id);
            dbQueryCounter.add(1, attrs("SELECT"));
            return result;
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("SELECT"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("SELECT"));
        }
    }

    @Override
    public Y getObject(UUID id) {
        long start = System.currentTimeMillis();
        try {
            Y result = super.getObject(id);
            dbQueryCounter.add(1, attrs("SELECT"));
            return result;
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("SELECT"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("SELECT"));
        }
    }

    @Override
    public List<Y> getAllObject(String tableName) {
        long start = System.currentTimeMillis();
        try {
            List<Y> result = super.getAllObject(tableName);
            dbQueryCounter.add(1, attrs("SELECT"));
            return result;
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("SELECT"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("SELECT"));
        }
    }

    @Override
    public List<Y> getAllObject(String tableName, String objectName) {
        long start = System.currentTimeMillis();
        try {
            List<Y> result = super.getAllObject(tableName, objectName);
            dbQueryCounter.add(1, attrs("SELECT"));
            return result;
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("SELECT"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("SELECT"));
        }
    }

    @Override
    public void deleteObject(int id) {
        long start = System.currentTimeMillis();
        try {
            super.deleteObject(id);
            dbQueryCounter.add(1, attrs("DELETE"));
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("DELETE"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("DELETE"));
        }
    }

    @Override
    public void deleteObject(UUID id) {
        long start = System.currentTimeMillis();
        try {
            super.deleteObject(id);
            dbQueryCounter.add(1, attrs("DELETE"));
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("DELETE"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("DELETE"));
        }
    }

    @Override
    public void executeQuery(Consumer<Session> action) throws PersistenceException {
        long start = System.currentTimeMillis();
        try {
            super.executeQuery(action);
            dbQueryCounter.add(1, attrs("EXECUTE"));
        } catch (Throwable t) {
            dbQueryErrorCounter.add(1, attrs("EXECUTE"));
            throw t;
        } finally {
            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs("EXECUTE"));
        }
    }
}
