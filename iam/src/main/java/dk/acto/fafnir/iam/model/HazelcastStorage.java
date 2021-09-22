package dk.acto.fafnir.iam.model;

import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.api.TableInitializer;
import dk.acto.fafnir.api.TablePersister;
import dk.acto.fafnir.api.model.AuthorizationTable;
import dk.acto.fafnir.api.model.ClaimsPayload;
import dk.acto.fafnir.api.model.conf.HazelcastConf;

import java.util.UUID;

public class HazelcastStorage implements TablePersister, TableInitializer, ItemListener<ClaimsPayload> {
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;
    private final AuthorizationTable authorizationTable;
    private UUID itemListener;

    public HazelcastStorage(HazelcastInstance hazelcastInstance, HazelcastConf hazelcastConf) {
        this.hazelcastInstance = hazelcastInstance;
        this.hazelcastConf = hazelcastConf;
        this.authorizationTable = initialize();
    }

    @Override
    public AuthorizationTable initialize() {
        ISet<ClaimsPayload> set =  hazelcastInstance.getSet(hazelcastConf.getSetName());
        this.itemListener = set.addItemListener(this, true);
        var authTable = new AuthorizationTable();
        set.forEach(authTable::consume);
        return authTable;
    }

    @Override
    public AuthorizationTable persist(AuthorizationTable authorizationTable) {
        ISet<ClaimsPayload> set =  hazelcastInstance.getSet(hazelcastConf.getSetName());
        set.removeItemListener(itemListener);
        set.clear();
        authorizationTable.dump().forEach(set::add);
        set.addItemListener(this, true);
        return authorizationTable;
    }

    @Override
    public void itemAdded(ItemEvent<ClaimsPayload> item) {
        authorizationTable.consume(item.getItem());
    }

    @Override
    public void itemRemoved(ItemEvent<ClaimsPayload> item) {

    }
}
