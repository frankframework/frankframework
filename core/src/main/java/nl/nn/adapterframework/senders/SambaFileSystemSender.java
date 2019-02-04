package nl.nn.adapterframework.senders;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.SambaFileSystem;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.CredentialFactory;

import org.apache.commons.lang.StringUtils;

public class SambaFileSystemSender extends FileSystemSender<SmbFile, SambaFileSystem> {

	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	private NtlmPasswordAuthentication auth = null;

	private String action = null;
	private List<String> actions = Arrays.asList("delete", "upload", "mkdir", "rmdir", "rename",
			"download", "list");
	private String share = null;
	private SmbFile smbContext = null;
	private boolean force = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getShare() == null)
			throw new ConfigurationException(getLogPrefix() + "server share endpoint is required");
		if (!getShare().startsWith("smb://"))
			throw new ConfigurationException(getLogPrefix() + "url must begin with [smb://]");

		if (getAction() == null)
			throw new ConfigurationException(getLogPrefix() + "action must be specified");
		if (!actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix() + "unknown or invalid action ["
					+ getAction() + "] supported actions are " + actions.toString() + "");

		//Check if necessarily parameters are available
		ParameterList parameterList = getParameterList();
		if (getAction().equals("upload")
				&& (parameterList == null || parameterList.findParameter("file") == null))
			throw new ConfigurationException(getLogPrefix()
					+ "the upload action requires the file parameter to be present");
		if (getAction().equals("rename")
				&& (parameterList == null || parameterList.findParameter("destination") == null))
			throw new ConfigurationException(getLogPrefix()
					+ "the rename action requires a destination parameter to be present");

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getDomain(), cf.getUsername(), cf.getPassword());
			log.debug("setting authentication to [" + auth.toString() + "]");
		}

		try {
			//Try to initially connect to the host and create the SMB session.
			//The session automatically closes and re-creates when required.
			setSmbContext(new SmbFile(getShare(), auth));

		} catch (MalformedURLException e) {
			throw new ConfigurationException(e);
		}
		super.setAction(action);
		setFileSystem(new SambaFileSystem(smbContext, force));
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public NtlmPasswordAuthentication getAuth() {
		return auth;
	}

	public void setAuth(NtlmPasswordAuthentication auth) {
		this.auth = auth;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action.toLowerCase();
	}

	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}

	public String getShare() {
		return share;
	}

	public void setShare(String share) {
		if (!share.endsWith("/"))
			share += "/";
		this.share = share;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public SmbFile getSmbContext() {
		return smbContext;
	}

	public void setSmbContext(SmbFile smbContext) {
		this.smbContext = smbContext;
	}

}
