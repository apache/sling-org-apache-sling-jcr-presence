/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.presence.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.presence.UserPresence;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.oak.spi.security.user.UserConstants.REP_AUTHORIZABLE_ID;
import static org.apache.jackrabbit.oak.spi.security.user.UserConstants.REP_DISABLED;

@Component(
    service = Observer.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(
    ocd = UserPresenterConfiguration.class,
    factory = true
)
public class UserPresenter extends NodeObserver {

    private String userId;

    private BundleContext bundleContext;

    private ServiceRegistration<UserPresence> presenceRegistration;

    @Reference
    private volatile SlingRepository slingRepository;

    private static final String USERS_PATH = "/home/users";

    private static final String USER_ID = "userId";

    private static final String DISABLED = "disabled";

    private static final String SYSTEM_USER = "systemUser";

    private final Logger logger = LoggerFactory.getLogger(UserPresenter.class);

    public UserPresenter() {
        super(USERS_PATH, JCR_PRIMARYTYPE, REP_AUTHORIZABLE_ID, REP_DISABLED);
    }

    @Activate
    public void activate(final UserPresenterConfiguration configuration, final BundleContext bundleContext) {
        logger.info("activating user presenter for {}", configuration.userId());
        userId = configuration.userId();
        this.bundleContext = bundleContext;
        try {
            final UserInfo userInfo = getUserById(userId);
            if (Objects.nonNull(userInfo)) {
                registerUserPresence(userInfo);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Deactivate
    public void deactivate() {
        logger.info("deactivating user presenter for {}", userId);
        unregisterUserPresence();
        bundleContext = null;
    }

    @Override
    protected void added(@NotNull final String path, @NotNull final Set<String> added, @NotNull final Set<String> deleted, @NotNull final Set<String> changed, @NotNull final Map<String, String> properties, @NotNull final CommitInfo commitInfo) {
        if (matches(properties)) {
            try {
                final UserInfo userInfo = getUserById(userId);
                if (Objects.nonNull(userInfo)) {
                    registerUserPresence(userInfo);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    protected void deleted(@NotNull final String path, @NotNull final Set<String> added, @NotNull final Set<String> deleted, @NotNull final Set<String> changed, @NotNull final Map<String, String> properties, @NotNull final CommitInfo commitInfo) {
        if (matches(properties)) {
            try {
                if (Objects.isNull(getUserById(userId))) {
                    unregisterUserPresence();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    protected void changed(@NotNull final String path, @NotNull final Set<String> added, @NotNull final Set<String> deleted, @NotNull final Set<String> changed, @NotNull final Map<String, String> properties, @NotNull final CommitInfo commitInfo) {
        if (matches(properties)) {
            try {
                final UserInfo userInfo = getUserById(userId);
                if (Objects.nonNull(userInfo)) {
                    final Dictionary<String, Object> serviceProperties = serviceProperties(userInfo);
                    presenceRegistration.setProperties(serviceProperties);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private UserPresence userPresence() {
        return () -> userId;
    }

    private void registerUserPresence(@NotNull final UserInfo userInfo) {
        final Dictionary<String, Object> properties = serviceProperties(userInfo);
        presenceRegistration = bundleContext.registerService(UserPresence.class, userPresence(), properties);
        logger.info("user presence for {} registered", userId);
    }

    private void unregisterUserPresence() {
        if (presenceRegistration != null) {
            presenceRegistration.unregister();
            presenceRegistration = null;
            logger.info("user presence for {} unregistered", userId);
        }
    }

    private UserInfo getUserById(final String userId) throws Exception {
        Session session = null;
        try {
            session = slingRepository.loginAdministrative(null);
            final UserManager userManager = AccessControlUtil.getUserManager(session);
            final Authorizable authorizable = userManager.getAuthorizable(userId);
            if (Objects.isNull(authorizable) || authorizable.isGroup()) {
                return null;
            } else {
                final User user = (User) authorizable;
                return new UserInfo(userId, user.isDisabled(), user.isSystemUser());
            }
        } finally {
            if (Objects.nonNull(session)) {
                session.logout();
            }
        }
    }

    private Dictionary<String, Object> serviceProperties(@NotNull final UserInfo userInfo) {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(USER_ID, userInfo.userId);
        properties.put(DISABLED, userInfo.isDisabled);
        properties.put(SYSTEM_USER, userInfo.isSystemUser);
        return properties;
    }

    private boolean isNodeUserOrSystemUser(@NotNull final Map<String, String> properties) {
        final String primaryType = properties.get(JCR_PRIMARYTYPE);
        return UserConstants.NT_REP_USER.equals(primaryType) || UserConstants.NT_REP_SYSTEM_USER.equals(primaryType);
    }

    private boolean matches(@NotNull final Map<String, String> properties) {
        final String authorizableId = properties.get(REP_AUTHORIZABLE_ID);
        if (userId.equals(authorizableId)) {
            return isNodeUserOrSystemUser(properties); // extra check
        }
        return false;
    }

    private static class UserInfo {

        final String userId;

        final boolean isDisabled;

        final boolean isSystemUser;

        public UserInfo(@NotNull final String userId, final boolean isDisabled, final boolean isSystemUser) {
            this.userId = userId;
            this.isDisabled = isDisabled;
            this.isSystemUser = isSystemUser;
        }

    }

}
