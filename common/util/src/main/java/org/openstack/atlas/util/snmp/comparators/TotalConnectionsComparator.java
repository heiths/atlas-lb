package org.openstack.atlas.util.snmp.comparators;

import java.util.Comparator;
import org.openstack.atlas.util.snmp.RawSnmpUsage;

public class TotalConnectionsComparator implements Comparator<RawSnmpUsage> {

    @Override
    public int compare(RawSnmpUsage o1, RawSnmpUsage o2) {
        long l1 = o1.getTotalConnections();
        long l2 = o2.getTotalConnections();
        if (l1 < l2) {
            return -1;
        }
        if (l1 > l2) {
            return 1;
        }
        return 0;
    }
}