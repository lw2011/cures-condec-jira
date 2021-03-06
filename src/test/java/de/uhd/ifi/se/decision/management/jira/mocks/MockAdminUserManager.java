package de.uhd.ifi.se.decision.management.jira.mocks;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.user.UserResolutionException;

public class MockAdminUserManager implements UserManager{

	@Override
	public boolean authenticate(String arg0, String arg1) {
		return false;
	}

	@Override
	public String getRemoteUsername() {
		return null;
	}

	@Override
	public String getRemoteUsername(HttpServletRequest arg0) {
		if((boolean)arg0.getAttribute("NoSysAdmin")){
			return "NoSysAdmin";
		}
		if((boolean)arg0.getAttribute("SysAdmin")){
			return "SysAdmin";
		}
		return null;
	}

	@Override
	public UserProfile getUserProfile(String arg0) {
		return null;
	}

	@Override
	public boolean isAdmin(String arg0) {
		return false;
	}

	@Override
	public boolean isSystemAdmin(String adminOrNot) {
        return !"NoSysAdmin".equals(adminOrNot);
    }

	@Override
	public boolean isUserInGroup(String arg0, String arg1) {
		return false;
	}

	@Override
	public Principal resolve(String arg0) throws UserResolutionException {
		return null;
	}

}
