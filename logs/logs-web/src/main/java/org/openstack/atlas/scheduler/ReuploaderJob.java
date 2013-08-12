package org.openstack.atlas.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.config.LbLogsConfiguration;
import org.openstack.atlas.config.LbLogsConfigurationKeys;
import org.openstack.atlas.exception.AuthException;
import org.openstack.atlas.logs.common.util.ReuploaderUtils;
import org.openstack.atlas.scheduler.execution.QuartzExecutable;
import org.openstack.atlas.service.domain.entities.JobName;
import org.openstack.atlas.service.domain.entities.JobStateVal;
import org.openstack.atlas.service.domain.pojos.LoadBalancerIdAndName;
import org.openstack.atlas.service.domain.repository.LoadBalancerRepository;
import org.openstack.atlas.service.domain.services.JobStateService;
import org.openstack.atlas.util.common.VerboseLogger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReuploaderJob extends QuartzJobBean implements StatefulJob {

    private static final Log LOG = LogFactory.getLog(ReuploaderJob.class);
    private QuartzExecutable execution;
    private LoadBalancerRepository loadBalancerRepository;
    private JobStateService jobStateService;
    private static final VerboseLogger vlog = new VerboseLogger(ReuploaderJob.class);

    @Required
    public void setLoadBalancerRepository(LoadBalancerRepository loadBalancerRepository) {
        this.loadBalancerRepository = loadBalancerRepository;
    }

    @Required
    public void setJobStateService(JobStateService jobStateService) {
        this.jobStateService = jobStateService;
    }

    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        LOG.info("running " + getClass());
        vlog.printf("ReuploaderJob starting: ");


        jobStateService.updateJobState(JobName.LOG_FILE_CF_UPLOAD, JobStateVal.CREATED);

        try {
            Map<Integer, LoadBalancerIdAndName> lbId2NameMap = createLbIdAndNameMap(loadBalancerRepository.getAllLoadbalancerIdsAndNames());
            LbLogsConfiguration conf = new LbLogsConfiguration();
            String dir = conf.getString(LbLogsConfigurationKeys.rawlogs_cache_dir);
            vlog.printf("cache dir: %s", dir);
            ReuploaderUtils reup = new ReuploaderUtils(dir, lbId2NameMap);
            reup.clearDirs(3); // Clean before the run as well. Just incase reuploadFiles dies
            reup.reuploadFiles();
            reup.clearDirs(3);
        } catch (AuthException e) {
            jobStateService.updateJobState(JobName.LOG_FILE_CF_UPLOAD, JobStateVal.FAILED);
            LOG.error("Error trying to upload to CloudFiles: ", e);
        } catch (Exception e) {
            jobStateService.updateJobState(JobName.LOG_FILE_CF_UPLOAD, JobStateVal.FINISHED);
            LOG.error("Unexpected Error trying to upload to CloudFiles: ", e);
        }

        jobStateService.updateJobState(JobName.LOG_FILE_CF_UPLOAD, JobStateVal.FINISHED);
        LOG.info("JOB COMPLETED. Total Time Taken for job to complete : ");
    }

    // Build AccountId/LbId to name map
    public Map<Integer, LoadBalancerIdAndName> createLbIdAndNameMap(List<LoadBalancerIdAndName> dbResults) {
        Map<Integer, LoadBalancerIdAndName> nameMap = new HashMap<Integer, LoadBalancerIdAndName>();
        for (LoadBalancerIdAndName lbId : dbResults) {
            int key = lbId.getLoadbalancerId();
            nameMap.put(key, lbId);
        }
        return nameMap;
    }
}
