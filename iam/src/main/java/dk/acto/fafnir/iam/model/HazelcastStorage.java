package dk.acto.fafnir.iam.model;

import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;

import java.util.UUID;

public class HazelcastStorage implements TablePersister, TableInitializer, ItemListener<ClaimData> {
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
        ISet<ClaimData> set =  hazelcastInstance.getSet(hazelcastConf.getSetName());
        this.itemListener = set.addItemListener(this, true);
        var authTable = new AuthorizationTable();
        set.forEach(authTable::consume);
        return authTable;
    }

    @Override
    public AuthorizationTable persist(AuthorizationTable authorizationTable) {
        ISet<ClaimData> set =  hazelcastInstance.getSet(hazelcastConf.getSetName());
        set.removeItemListener(itemListener);
        set.clear();
        authorizationTable.dump().forEach(set::add);
        set.addItemListener(this, true);
        return authorizationTable;
    }

    @Override
    public void itemAdded(ItemEvent<ClaimData> item) {
        authorizationTable.consume(item.getItem());
    }

    @Override
    public void itemRemoved(ItemEvent<ClaimData> item) {
        authorizationTable.destroy(item.getItem());
    }
}
