package org.openstack.atlas.service.domain.services.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.docs.loadbalancers.api.v1.SslTermination;
import org.openstack.atlas.service.domain.entities.*;
import org.openstack.atlas.service.domain.exceptions.BadRequestException;
import org.openstack.atlas.service.domain.util.StringUtilities;
import org.openstack.atlas.util.ca.zeus.ZeusCertFile;

import java.util.List;
import java.util.Map;


public final class SslTerminationHelper {
    protected static final Log LOG = LogFactory.getLog(SslTerminationHelper.class);


    public static boolean modificationStatus(SslTermination sslTermination, LoadBalancer dbLoadBalancer) throws BadRequestException {
        //Validator let it through, now verify the request is for update of attributes only, skip cert validation...
        //Otherwise inform user that there is no ssl termination to update values for...
        if (sslTermination.getCertificate() == null && sslTermination.getPrivatekey() == null) {
            if (dbLoadBalancer.hasSsl()) {
                LOG.info("Updating attributes only, skipping certificate validation.");
                return true;
            } else {
                LOG.error("Cannot update values for non-existent ssl termination object...");
                throw new BadRequestException("No ssl termination to update values for.");
            }
        }
        return false;
    }

    public static boolean isProtocolSecure(LoadBalancer loadBalancer) throws BadRequestException {
        LoadBalancerProtocol protocol = loadBalancer.getProtocol();
        if (protocol == LoadBalancerProtocol.HTTPS || protocol == LoadBalancerProtocol.IMAPS
                || protocol == LoadBalancerProtocol.LDAPS || protocol == LoadBalancerProtocol.POP3S) {
            throw new BadRequestException("Can not create ssl termination on a load balancer using a secure protocol.");
        }
        return true;
    }

    public static boolean verifyPortSecurePort(LoadBalancer loadBalancer, SslTermination sslTermination, Map<Integer, List<LoadBalancer>> vipPorts, Map<Integer, List<LoadBalancer>> vip6Ports) {
        LOG.info("Verifying port and secure port are unique for loadbalancer: " + loadBalancer.getId());
        if (sslTermination != null && sslTermination.getSecurePort() != null) {
            if (loadBalancer.hasSsl()
                    && loadBalancer.getSslTermination().getSecurePort() == sslTermination.getSecurePort()) {
                return true;
            }

            if (!vipPorts.isEmpty()) {

                if (vipPorts.containsKey(sslTermination.getSecurePort())) {
                    return false;
                }
            }

            if (!vip6Ports.isEmpty()) {
                if ((vip6Ports.containsKey(sslTermination.getSecurePort()))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void verifyCertificationCredentials(ZeusCertFile zeusCertFile) throws BadRequestException {
        if (zeusCertFile.getErrorList().size() > 0) {
            String errors = StringUtilities.buildDelemtedListFromStringArray(zeusCertFile.getErrorList().toArray(new String[zeusCertFile.getErrorList().size()]), ",");
            LOG.error(String.format("There was an error(s) while updating ssl termination: '%s'", errors));
            throw new BadRequestException(errors);
        }
    }

    public static org.openstack.atlas.service.domain.entities.SslTermination verifyAttributes(SslTermination queTermination, org.openstack.atlas.service.domain.entities.SslTermination dbTermination) {
        if (dbTermination == null) {
            dbTermination = new org.openstack.atlas.service.domain.entities.SslTermination();
        }

        //Set fields to updated values
        if (queTermination.isEnabled() != null) {
            dbTermination.setEnabled(queTermination.isEnabled());
        }

        if (queTermination.isSecureTrafficOnly() != null) {
            dbTermination.setSecureTrafficOnly(queTermination.isSecureTrafficOnly());
        }

        if (queTermination.getSecurePort() != null) {
            dbTermination.setSecurePort(queTermination.getSecurePort());
        }

        //The certificates are either null or populated, no updating.
        if (queTermination.getCertificate() != null) {
            dbTermination.setCertificate(queTermination.getCertificate());
        }

        if (queTermination.getIntermediateCertificate() != null) {
            dbTermination.setIntermediateCertificate(queTermination.getIntermediateCertificate());
        } else {
            if (queTermination.getCertificate() != null && queTermination.getCertificate() != null) {
                dbTermination.setIntermediateCertificate(null);
            }
        }

        if (queTermination.getPrivatekey() != null) {
            dbTermination.setPrivatekey(queTermination.getPrivatekey());
        }
        return dbTermination;
    }
}
