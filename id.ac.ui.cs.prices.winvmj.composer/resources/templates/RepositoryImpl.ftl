package ${packageName};

import id.ac.ui.cs.prices.winvmj.hibernate.RepositoryUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.persistence.PersistenceException;
import org.hibernate.Session;

/**
 * Bare Repository proxy — extends RepositoryUtil, overrides all public methods
 * with pass-through to super. Monitoring injectors (DbMetrics, Tracing, etc.)
 * will modify this file via AST to wrap method bodies with their concerns.
 */
public class ${entityName}RepositoryImpl<Y> extends RepositoryUtil<Y> {

    private static final String FEATURE_NAME = "${featureName}";
    private static final String TABLE_NAME = "${tableName}";

    public ${entityName}RepositoryImpl(Class<? extends Y> componentClass) {
        super(componentClass);
    }

    @Override
    public void saveObject(Y object) throws PersistenceException {
        super.saveObject(object);
    }

    @Override
    public void updateObject(Y object) {
        super.updateObject(object);
    }

    @Override
    public Y getObject(int id) {
        return super.getObject(id);
    }

    @Override
    public Y getObject(UUID id) {
        return super.getObject(id);
    }

    @Override
    public List<Y> getAllObject(String tableName) {
        return super.getAllObject(tableName);
    }

    @Override
    public List<Y> getAllObject(String tableName, String objectName) {
        return super.getAllObject(tableName, objectName);
    }

    @Override
    public void deleteObject(int id) {
        super.deleteObject(id);
    }

    @Override
    public void deleteObject(UUID id) {
        super.deleteObject(id);
    }

    @Override
    public void executeQuery(Consumer<Session> action) throws PersistenceException {
        super.executeQuery(action);
    }
}
