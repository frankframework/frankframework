/*
   Copyright 2021 Nationale-Nederlanden, 2022, 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.credentialprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.JNDIRealm;
import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.file.ConfigFileLoader;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.credentialprovider.rolemapping.RoleGroupMapper;
import org.frankframework.credentialprovider.rolemapping.RoleGroupMappingRuleSet;
import org.frankframework.credentialprovider.util.Cache;


/**
 * Extension of {@link org.apache.catalina.realm.JNDIRealm} where we take care of the
 * role to ldap group mapping
 * <p>
 * Set the <code>pathname</code> parameter to the role-mapping file where the
 * role to ldap group mapping is defined.
 *
 * @author Fabian van Druenen
 * @author Gerrit van Brakel
 */
public class RoleToGroupMappingJndiRealm extends JNDIRealm implements RoleGroupMapper {

	private static final String ALL_AUTHENTICATED = "AllAuthenticated";

	private final Log log = LogFactory.getLog(this.getClass());

	private final Cache<String, Attributes, NamingException> roleMembershipCache = new Cache<>(3600 * 1000);

	/**
	 * The pathname (absolute or relative to Catalina's current working directory "catalina.base")
	 * of the XML file containing our mapping information.
	 */
	@Setter @Getter private String pathname = null;

	/**
	 * The Digester we will use to process role-mapping data.
	 */
	private static Digester digester = null;


	/**
	 * Find the LDAP group memberships of this user.
	 * Based on {@link JNDIRealm#authenticate(String username, String credentials)}
	 */
	public List<String> getRoles(String username) {
		JNDIConnection connection = null;
		List<String> roles;
		try {
			connection = get();
			try {
				roles = getRoles(connection, username);
			} catch (NullPointerException | NamingException e) {
				this.containerLog.info(sm.getString("jndiRealm.exception.retry"), e);
				close(connection);
				closePooledConnections();
				connection = get();
				roles = getRoles(connection, username);
			}
			release(connection);
			return roles;
		} catch (NamingException e) {
			this.containerLog.error(sm.getString("jndiRealm.exception"), e);
			close(connection);
			closePooledConnections();
			if (this.containerLog.isDebugEnabled()) this.containerLog.debug("Returning null roles.");
			return List.of();
		}
	}

	/**
	 * Find the LDAP group memberships of this user.
	 * Based on {@link JNDIRealm#authenticate(JNDIConnection connection, String username, String credentials)}
	 */
	private List<String> getRoles(JNDIConnection connection, String username) throws NamingException {
		if (StringUtils.isEmpty(username)) {
			containerLog.debug("username null or empty: returning null roles.");
			return List.of();
		}
		if (this.userPatternArray != null) {
			for (int curUserPattern = 0; curUserPattern < this.userPatternArray.length; curUserPattern++) {
				User user1 = getUser(connection, username, null, curUserPattern);
				if (user1 != null)
					try {
						List<String> list = getRoles(connection, user1);
						if (this.containerLog.isDebugEnabled()) this.containerLog.debug("Found roles: " + list.toString());
						return list;
					} catch (InvalidNameException ine) {
						this.containerLog.warn(sm.getString("jndiRealm.exception"), ine);
					}
			}
			return List.of();
		}
		User user = getUser(connection, username, null, -1);
		if (user == null) {
			return List.of();
		}
		List<String> roles = getRoles(connection, user);
		if (this.containerLog.isDebugEnabled()) this.containerLog.debug("Found roles: " + roles.toString());
		return roles;
	}

	/**
	 * Overrides getRoles to find the nested group memberships of this user, assuming users and groups
	 * have a "memberOf" like attribute (specified by 'userRoleName' and 'roleName') that specifies the groups
	 * they are member of. The original getRoles assumed groups have a 'member' attribute, specifying their
	 * members. That approach is not available in this implementation.
	 *
	 * Shamik uses the nn-tomcat-extensions JNDIRealmEx, with additional settings:
	 * - roleBase="company specific tenant base"
	 * - roleSubtree="true"
	 * - roleSearch="(&amp;(member={0})(objectcategory=group))"
	 * - roleName="cn"
	 * - roleNested="true"
	 * This is expected to be less performant, because it searches each time over all groups.
	 */
	@Override
	protected List<String> getRoles(JNDIConnection connection, User user) throws NamingException {
		long t1 = log.isDebugEnabled() ? System.currentTimeMillis() : 0;
		int groupCheckCount=0;
		int nestedRolesFound=0;
		try {
			List<String> roles = user.getRoles();
			Set<String> allRoles = new LinkedHashSet<>(roles);
			Queue<String> rolesToCheck = new ArrayDeque<>(allRoles);

			if (this.containerLog.isTraceEnabled()) this.containerLog.trace("allRoles in: "+allRoles);

			String[] attrIds = { getRoleName() };

			String role;
			while((role=rolesToCheck.poll())!=null) {
				groupCheckCount++;

				Attributes attrs = roleMembershipCache.computeIfAbsentOrExpired(role, r -> connection.context.getAttributes(r, attrIds));

				for (NamingEnumeration<? extends Attribute> attsEnum= attrs.getAll(); attsEnum.hasMoreElements();) {
					Attribute attr = attsEnum.next();
					for (NamingEnumeration<?> attEnum= attr.getAll(); attEnum.hasMoreElements();) {

						String nestedRole = attEnum.next().toString();
						if (this.containerLog.isTraceEnabled()) this.containerLog.trace("nestedRole: "+nestedRole);

						if (!allRoles.contains(nestedRole)) {
							rolesToCheck.add(nestedRole);
							allRoles.add(nestedRole);
							nestedRolesFound++;
						}
					}
				}
			}
			allRoles.add(ALL_AUTHENTICATED);
			if (this.containerLog.isTraceEnabled()) this.containerLog.trace("allRoles out: "+allRoles);
			return new ArrayList<>(allRoles);
		} finally {
			if (log.isDebugEnabled()) {
				long t2 = System.currentTimeMillis();
				log.debug("Role retrieval for user ["+user.getDN()+"] in LDAP took ["+(t2-t1)+"]ms, groupCheckCount ["+groupCheckCount+"] nestedRolesFound ["+nestedRolesFound+"]");
			}
		}
	}


	@Override
	protected void startInternal() throws LifecycleException {
		if (log.isTraceEnabled()) log.trace(">>> startInternal");
		super.startInternal();

		try {
			initMappingConfig();
		} catch (IOException e) {
			throw new LifecycleException(e);
		}

		if (log.isTraceEnabled()) log.trace("<<< startInternal");
	}

	/**
	 * @return a configured <code>Digester</code> to use for processing the XML input file, creating a new one if necessary.
	 */
	protected synchronized Digester getDigester() {
		if (digester == null) {
			digester = new Digester();
			digester.setValidating(false);
			try {
				digester.setFeature("http://apache.org/xml/features/allow-java-encodings", true);
			} catch (Exception e) {
				log.warn(sm.getString("memoryRealm.xmlFeatureEncoding"), e);
			}
			digester.addRuleSet(new RoleGroupMappingRuleSet());
		}
		return digester;
	}

	/**
	 * Read the mapping configuration and apply the role group mapping to the container
	 */
	protected void initMappingConfig() throws IOException {
		if (log.isTraceEnabled()) log.trace(">>> initMappingConfig");
		String pathName = getPathname();
		if (pathName == null) {
			if (log.isDebugEnabled()) log.debug("<<< initMappingConfig no path configured");
			return;
		}

		try (InputStream is = ConfigFileLoader.getSource().getResource(pathName).getInputStream()) {
			// Load the contents of the database file
			if (log.isDebugEnabled()) log.debug("Loading mapping: " + pathName);

			Digester digester = getDigester();
			try {
				synchronized (digester) {
					digester.push(this);
					digester.parse(is);
				}
			} catch (IOException | SAXException e) {
				throw new IOException("Exception while reading role-group-mapping file", e);
			} finally {
				digester.reset();
				reportMappingConfig();
			}
		}

		if (log.isTraceEnabled()) log.trace("<<< initMappingConfig");
	}

	/**
	 * Report the roles mapping configured on the container
	 */
	protected void reportMappingConfig() {
		if (log.isTraceEnabled()) log.trace(">>> reportMappingConfig");
		Container container = getContainer();
		if (container instanceof Context cxt) {
			String[] securityRoles = cxt.findSecurityRoles();
			if (securityRoles != null) {
				log.info("Security role mappings:");
				for (String role : securityRoles) {
					log.info("Security [role]: %s [link]: %s".formatted(role, cxt.findRoleMapping(role)));
				}
			} else {
				log.info("No security roles found.");
			}
		}

		if (log.isTraceEnabled()) log.trace("<<< reportMappingConfig");
	}

	/**
	 * Add the role, and it's link(mapping) to the context where the webapp is
	 * running in. The tomcat implementation will use this to do the mapping, just
	 * like it's done with the web.xml "security-role-ref" specification
	 */
	@Override
	public void addRoleGroupMapping(String role, String group) {
		if (role != null && !role.isEmpty() && group != null && !group.isEmpty()) {

			Container container = getContainer();
			log.info(">>> addRoleGroupMapping container: " + container);

			if (container instanceof Context cxt) {

				cxt.addRoleMapping(role, group);
				log.info(">>> addRoleGroupMapping role: " + role + ", group: " + group);
			} else {
				log.warn(">>> skipped addRoleGroupMapping no Context found in container: " + container + " for role: " + role + ", group: " + group);
			}
		} else {
			log.warn(">>> skipped addRoleGroupMapping role: " + role + ", group: " + group);
		}
	}

}
