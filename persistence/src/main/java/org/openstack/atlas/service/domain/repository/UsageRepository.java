package org.openstack.atlas.service.domain.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.service.domain.entities.LoadBalancer;
import org.openstack.atlas.service.domain.entities.Usage;
import org.openstack.atlas.service.domain.entities.Usage_;
import org.openstack.atlas.service.domain.events.UsageEvent;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerUsage;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerUsage_;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public class UsageRepository {

    final Log LOG = LogFactory.getLog(UsageRepository.class);
    @PersistenceContext(unitName = "loadbalancing")
    private EntityManager entityManager;

    public List<Usage> getMostRecentUsageForLoadBalancers(Collection<Integer> loadBalancerIds) {
        if (loadBalancerIds == null || loadBalancerIds.isEmpty()) return new ArrayList<Usage>();

        Query query = entityManager.createNativeQuery("SELECT a.* " +
                "FROM lb_usage a, " +
                "(SELECT loadbalancer_id, max(end_time) as end_time FROM lb_usage WHERE loadbalancer_id in (:loadbalancerIds) GROUP BY loadbalancer_id) b " +
                "WHERE a.loadbalancer_id in (:loadbalancerIds) and a.loadbalancer_id = b.loadbalancer_id and a.end_time = b.end_time;", Usage.class)
                .setParameter("loadbalancerIds", loadBalancerIds);

        List<Usage> usage = (List<Usage>) query.getResultList();
        if (usage == null) return new ArrayList<Usage>();

        return usage;
    }

    public void batchCreate(List<Usage> usages) {
        LOG.info(String.format("batchCreate() called with %d records", usages.size()));

        String query = generateBatchInsertQuery(usages);
        entityManager.createNativeQuery(query).executeUpdate();
    }

    public void batchUpdate(List<Usage> usages) {
        LOG.info(String.format("batchUpdate() called with %d records", usages.size()));

        String query = generateBatchUpdateQuery(usages);
        entityManager.createNativeQuery(query).executeUpdate();
    }

    private String generateBatchInsertQuery(List<Usage> usages) {
        final StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO lb_usage(loadbalancer_id, avg_concurrent_conns, bandwidth_in, bandwidth_out, start_time, end_time, num_polls, num_vips, tags_bitmask, event_type) values");
        sb.append(generateFormattedValues(usages));
        return sb.toString();
    }

    private String generateBatchUpdateQuery(List<Usage> usages) {
        final StringBuilder sb = new StringBuilder();
        sb.append("REPLACE INTO lb_usage(id, loadbalancer_id, avg_concurrent_conns, bandwidth_in, bandwidth_out, start_time, end_time, num_polls, num_vips, tags_bitmask, event_type) values");
        sb.append(generateFormattedValues(usages));
        return sb.toString();
    }

    private String generateFormattedValues(List<Usage> usages) {
        StringBuilder sb = new StringBuilder();

        for (Usage usage : usages) {
            sb.append("(");
            if (usage.getId() != null) {
                sb.append(usage.getId()).append(",");
            }
            sb.append(usage.getLoadbalancer().getId()).append(",");
            sb.append(usage.getAverageConcurrentConnections()).append(",");
            sb.append(usage.getIncomingTransfer()).append(",");
            sb.append(usage.getOutgoingTransfer()).append(",");

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String startTime = formatter.format(usage.getStartTime().getTime());
            sb.append("'").append(startTime).append("',");

            String endTime = formatter.format(usage.getEndTime().getTime());
            sb.append("'").append(endTime).append("',");

            sb.append(usage.getNumberOfPolls()).append(",");
            sb.append(usage.getNumVips()).append(",");
            sb.append(usage.getTags()).append(",");
            if (usage.getEventType() == null) {
                sb.append(usage.getEventType());
            } else {
                sb.append("'").append(usage.getEventType()).append("'");
            }
            sb.append("),");

        }
        if (sb.toString().endsWith(",")) {
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        return sb.toString();
    }

    public List<Integer> getLoadBalancerIdsIn(Collection<Integer> lbIdsToCheckAgainst) {
        if (lbIdsToCheckAgainst == null || lbIdsToCheckAgainst.isEmpty()) return new ArrayList<Integer>();

        Query query = entityManager.createNativeQuery("SELECT id FROM loadbalancer WHERE id in (:loadbalancerIds);")
                .setParameter("loadbalancerIds", lbIdsToCheckAgainst);

        List<Integer> idsInDatabase = (List<Integer>) query.getResultList();
        if (idsInDatabase == null) return new ArrayList<Integer>();

        return idsInDatabase;

    }

    public List<Usage> getRecordForLoadBalancer(Integer loadBalancerId, UsageEvent usageEvent) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Usage> criteria = builder.createQuery(Usage.class);
        Root<Usage> loadBalancerUsageRoot = criteria.from(Usage.class);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setId(loadBalancerId);

        Predicate hasLoadBalancerId = builder.equal(loadBalancerUsageRoot.get(Usage_.loadbalancer), loadBalancer);
        Predicate hasEventType = builder.equal(loadBalancerUsageRoot.get(Usage_.eventType), usageEvent.name());

        criteria.select(loadBalancerUsageRoot);
        criteria.where(builder.and(hasLoadBalancerId, hasEventType));

        return entityManager.createQuery(criteria).getResultList();
    }
}
