package org.openstack.atlas.service.domain.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.service.domain.entities.*;
import org.openstack.atlas.service.domain.exceptions.EntityNotFoundException;
import org.openstack.atlas.service.domain.util.Constants;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository
@Transactional
public class SslTerminationRepository {

    final Log LOG = LogFactory.getLog(SslTerminationRepository.class);
    @PersistenceContext(unitName = "loadbalancing")
    private EntityManager entityManager;/**/


    public boolean removeSslTermination(Integer lid, Integer aid) throws EntityNotFoundException {
        SslTermination up = getSslTerminationByLbId(lid, aid);
        if (up == null) {
            return false;
        } else {
            entityManager.remove(up);
            return true;
        }
    }

    public SslTermination getSslTerminationByLbId(Integer lid, Integer accountId) throws EntityNotFoundException {
        List<SslTermination> sslTerminations = entityManager.createQuery("SELECT s FROM SslTermination s where s.loadbalancer.id = :lid").setParameter("lid", lid).getResultList();
        if (sslTerminations != null && sslTerminations.size() > 0 && sslTerminations.get(0).getLoadbalancer().getAccountId().equals(accountId)) {
            return sslTerminations.get(0);
        } else {
            String message = Constants.SslTerminationNotFound;
            LOG.warn(message);
            throw new EntityNotFoundException(message);
        }
    }

    public SslTermination setSslTermination(Integer lid, SslTermination sslTermination) throws EntityNotFoundException {
        LoadBalancer lb = getLbById(lid);
        sslTermination.setLoadbalancer(lb);
        entityManager.merge(sslTermination);
        return sslTermination;
    }

    private LoadBalancer getLbById(Integer id) throws EntityNotFoundException {
        LoadBalancer lb = entityManager.find(LoadBalancer.class, id);
        if (lb == null) {
            String message = Constants.LoadBalancerNotFound;
            LOG.warn(message);
            throw new EntityNotFoundException(message);
        }
        return lb;
    }
}
