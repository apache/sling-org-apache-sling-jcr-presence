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
package org.apache.sling.jcr.presence;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.ServiceReference;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UserPresenterIT extends JcrPresenceTestSupport {

    protected static final String FACTORY_PID = "org.apache.sling.jcr.presence.internal.UserPresenter";

    @Configuration
    public Option[] configuration() {
        return options(
            baseConfiguration(),
            factoryConfiguration(FACTORY_PID)
                .put("userId", "sling-readall")
                .asOption(),
            factoryConfiguration(FACTORY_PID)
                .put("userId", "test")
                .asOption()
        );
    }

    @Test
    public void testUserPresence() throws Exception {
        // sling-readall user
        assertThat(countUserPresences("sling-readall", false, true)).isEqualTo(1);

        // test user
        assertThat(isUserPresent("test")).isFalse();
        assertThat(countUserPresences("test", false, true)).isEqualTo(0);

        createUser("test", true);
        assertThat(isUserPresent("test")).isTrue();

        with().
            pollInterval(1, SECONDS).
            then().
            await().
            alias("counting user presences (created)").
            atMost(10, SECONDS).
            until(() -> countUserPresences("test", false, true) == 1);

        disableUser("test");
        assertThat(isUserPresent("test")).isTrue();

        with().
            pollInterval(1, SECONDS).
            then().
            await().
            alias("counting user presences (disabled)").
            atMost(10, SECONDS).
            until(() -> countUserPresences("test", true, true) == 1);

        removeUser("test");
        assertThat(isUserPresent("test")).isFalse();

        with().
            pollInterval(1, SECONDS).
            then().
            await().
            alias("counting user presences (removed)").
            atMost(10, SECONDS).
            until(() -> countUserPresences("test", false, true) == 0);
    }

    private void createUser(@NotNull final String userId, final boolean systemUser) throws Exception {
        final Session session = slingRepository.loginAdministrative(null);
        if (systemUser) {
            AccessControlUtil.getUserManager(session).createSystemUser(userId, null);
        } else {
            AccessControlUtil.getUserManager(session).createUser(userId, UUID.randomUUID().toString());
        }
        session.save();
        session.logout();
    }

    private void disableUser(@NotNull final String userId) throws Exception {
        final Session session = slingRepository.loginAdministrative(null);
        final Authorizable authorizable = AccessControlUtil.getUserManager(session).getAuthorizable(userId);
        final User user = (User) authorizable;
        user.disable("no reason");
        session.save();
        session.logout();
    }

    private void removeUser(@NotNull final String userId) throws Exception {
        final Session session = slingRepository.loginAdministrative(null);
        final Authorizable authorizable = AccessControlUtil.getUserManager(session).getAuthorizable(userId);
        authorizable.remove();
        session.save();
        session.logout();
    }

    private boolean isUserPresent(final String userId) throws Exception {
        Session session = null;
        try {
            session = slingRepository.loginAdministrative(null);
            return Objects.nonNull(AccessControlUtil.getUserManager(session).getAuthorizable(userId));
        } finally {
            if (Objects.nonNull(session)) {
                session.logout();
            }
        }
    }

    private int countUserPresences(@NotNull final String userId, final boolean disabled, final boolean systemUser) throws Exception {
        final String filter = String.format("(&(userId=%s)(disabled=%s)(systemUser=%s))", userId, disabled, systemUser);
        final Collection<ServiceReference<UserPresence>> serviceReferences = bundleContext.getServiceReferences(UserPresence.class, filter);
        return serviceReferences.size();
    }

}
