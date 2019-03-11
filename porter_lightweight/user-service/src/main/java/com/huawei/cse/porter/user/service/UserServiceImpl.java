package com.huawei.cse.porter.user.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.huawei.cse.porter.user.api.SessionInfo;
import com.huawei.cse.porter.user.api.UserService;
import com.huawei.cse.porter.user.dao.SessionInfoModel;
import com.huawei.cse.porter.user.dao.SessionMapper;
import com.huawei.cse.porter.user.dao.UserInfo;
import com.huawei.cse.porter.user.dao.UserMapper;
import com.netflix.config.DynamicPropertyFactory;

@Service
public class UserServiceImpl implements UserService {
  private static Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private SessionMapper sessionMapper;


  public SessionInfo login(String userName,
      String password) {
    UserInfo userInfo = userMapper.getUserInfo(userName);
    if (userInfo != null) {
      if (validatePassword(password, userInfo.getPassword())) {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessiondId(UUID.randomUUID().toString());
        sessionInfo.setUserName(userInfo.getUserName());
        sessionInfo.setRoleName(userInfo.getRoleName());
        sessionMapper.createSession(sessionInfo);
        return sessionInfo;
      }
    }
    return null;
  }

  public SessionInfo getSession(String sessionId) {
    if (sessionId == null) {
      throw new InvocationException(405, "", "invalid session.");
    }
    SessionInfoModel sessionInfo = sessionMapper.getSessioinInfo(sessionId);
    if (sessionInfo != null) {
      if (System.currentTimeMillis() - sessionInfo.getActiveTime().getTime() > 10 * 60 * 1000) {
        LOGGER.info("user session expired.");
        return null;
      } else {
        sessionMapper.updateSessionInfo(sessionInfo.getSessiondId());
        return SessionInfoModel.toSessionInfo(sessionInfo);
      }
    }
    return null;
  }

  private boolean validatePassword(String plain, String encrypt) {
    // 简单加密校验，开发者可以结合业务场景自行修改
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(plain.getBytes("UTF-8"));
      byte byteBuffer[] = messageDigest.digest();
      String encryptedText = Base64.encodeBase64String(byteBuffer);
      return encryptedText.equals(encrypt);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("", e);
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("", e);
    }
    return false;
  }


  public String ping(String message) {
    long delay = DynamicPropertyFactory.getInstance().getLongProperty("user.ping.delay", 0).get();
    if (delay > 0) {
      try {
        TimeUnit.SECONDS.sleep(delay);
      } catch (InterruptedException e) {
        LOGGER.error("", e);
      }
    }
    return message;
  }
}
