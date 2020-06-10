package com.plumelog.server.collect;


import com.plumelog.core.exception.LogQueueConnectException;
import com.plumelog.server.InitConfig;
import com.plumelog.server.client.ElasticLowerClient;
import com.plumelog.core.constant.LogMessageConstant;
import com.plumelog.core.redis.RedisClient;
import com.plumelog.server.util.DateUtil;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * className：RedisLogCollect
 * description：RedisLogCollect 获取redis中日志，存储到es
 *
 * @author Frank.chen
 * @version 1.0.0
 */
public class RedisLogCollect extends BaseLogCollect{
    private  org.slf4j.Logger logger= LoggerFactory.getLogger(RedisLogCollect.class);
    private RedisClient redisClient;

    /**
     * 无密码redis
     * @param redisHost
     * @param redisPort
     * @param esHosts
     * @param userName
     * @param passWord
     */
    public RedisLogCollect(String redisHost,int redisPort,String esHosts,String userName,String passWord){

        this.redisClient=RedisClient.getInstance(redisHost,redisPort,"");
        logger.info("getting log ready!");
        super.elasticLowerClient= ElasticLowerClient.getInstance(esHosts,userName,passWord);
        logger.info("sending log ready!");
    }

    /**
     * 有密码redis
     * @param redisHost
     * @param redisPort
     * @param redisPassWord
     * @param esHosts
     * @param userName
     * @param passWord
     */
    public RedisLogCollect(String redisHost,int redisPort,String redisPassWord,String esHosts,String userName,String passWord){

        this.redisClient=RedisClient.getInstance(redisHost,redisPort,redisPassWord);
        logger.info("getting log ready!");
        super.elasticLowerClient= ElasticLowerClient.getInstance(esHosts,userName,passWord);
        logger.info("sending log ready!");
    }
    public  void redisStart(){

        threadPoolExecutor.execute(()->{
            collectRuningLog();
        });
        threadPoolExecutor.execute(()->{
            collectTraceLog();
        });

    }
    private  void collectRuningLog(){
        while (true) {
            try {
                Thread.sleep(InitConfig.MAX_INTERVAL);
                List<String> logs=redisClient.getMessage(LogMessageConstant.LOG_KEY, InitConfig.MAX_SEND_SIZE);
                collect(logs,LogMessageConstant.ES_INDEX+ LogMessageConstant.LOG_TYPE_RUN + "_" + DateUtil.parseDateToStr(new Date(),DateUtil.DATE_FORMAT_YYYYMMDD));
                } catch (InterruptedException e) {
                    logger.error("",e);
                } catch (LogQueueConnectException e) {
                    logger.error("从redis队列拉取日志失败！",e);
                }
        }
    }
    private  void collectTraceLog(){
        while (true) {
            try {
                Thread.sleep(InitConfig.MAX_INTERVAL);
                List<String> logs=redisClient.getMessage(LogMessageConstant.LOG_KEY_TRACE,InitConfig.MAX_SEND_SIZE);
                collectTrace(logs,LogMessageConstant.ES_INDEX+LogMessageConstant.LOG_TYPE_TRACE+"_"+ DateUtil.parseDateToStr(new Date(),DateUtil.DATE_FORMAT_YYYYMMDD));
                } catch (InterruptedException e) {
                    logger.error("",e);
                }catch (LogQueueConnectException e) {
                    logger.error("从redis队列拉取日志失败！",e);
                }
        }
    }
    private  void collect(List<String> logs,String index){
        if(logs.size()>0) {
            logs.forEach(log->{
                logger.debug("get log:" + log);
                super.logList.add(log);
            });
            if (super.logList.size() > 0) {
                List<String> logList=new ArrayList();
                logList.addAll(super.logList);
                super.logList.clear();
                super.sendLog(index,logList);
            }
        }
    }
    private  void collectTrace(List<String> logs,String index){
        if(logs.size()>0) {
            logs.forEach(log->{
                logger.debug("get log:" + log);
                super.traceLogList.add(log);
            });
            if (super.traceLogList.size() > 0) {
                List<String> logList=new ArrayList();
                logList.addAll(super.traceLogList);
                super.traceLogList.clear();
                super.sendTraceLogList(index,logList);
            }
        }
    }
}
